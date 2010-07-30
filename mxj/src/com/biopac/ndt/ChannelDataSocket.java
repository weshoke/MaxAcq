/*
 * ChannelDataSocket.java
 *
 * Copyright 2006-2010, BIOPAC Systems, Inc.
 * All rights reserved
 */

package com.biopac.ndt;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * This helper class allows for reception of channel data via a TCP socket
 * over which an AcqKnowledge server is delivering binary data.  This
 * is intended to be used in the multiple connection transfer type mode
 * with big endian double valued data type transfer streams.
 *
 * Channel data that is read in from the server is retained in a temporary
 * buffer in memory and cached for delivery to the client.  The data is retained
 * in memory until either the next call to startProcessing() or until the
 * client explicitly removes it from the buffer using removeSamplesFromBuffer().
 *
 * Clients that are performing continuous or long-term data acquisitions
 * should be sure to continually invoke removeSamplesFromBuffer() to avoid
 * running out of memory.
 *
 * @author  edwardp
 * @version 
 */
public class ChannelDataSocket {
    private int dataConnectionPort;
    private boolean bRunThreads=false;
    private List data;
    private Thread listeningThread=null;
    
    /**
     * The listening thread will listen for incoming data connections and
     * spawn off additional threads to handle incoming data.
     */
    private class ListeningThread extends Thread {
        /**
         * Listen for incoming data connections and dispatch additional
         * handler threads as desired.
         */
        public void run() {
            try
            {
                ServerSocket listeningSocket=new ServerSocket(dataConnectionPort);
                while(bRunThreads)
                {
                    Socket s=listeningSocket.accept();
                    if(bRunThreads) // our final accept may be to unblock the thread for death
                    {
                        (new DataProcessingThread(s)).start();
                    }
                }
                listeningSocket.close();
            }
            catch (IOException e)
            {
                System.err.println("Error in listening thread: "+e);
            }
        }
    }
    
    /**
     * Handle all incoming data on the connection while it is still alive
     * and copy it into our internal buffers
     */
    private class DataProcessingThread extends Thread {
        private Socket s;
        
        /**
         * Construct a new data processing thread to buffer data as it is
         * received over a TCP data connection
         *
         * @param sock  the socket to which the AcqKnowledge server is
         *              connected
         */
        public DataProcessingThread(Socket sock) {
            s=sock;
        }
        
        /**
         * Wait for incoming data on the socket and copy it into the internal
         * ChannelDataSocket object's data buffers
         */
        public void run() {
            try
            {
                InputStream inputStream=s.getInputStream();
                DataInputStream dataStream=new DataInputStream(inputStream);
                while(bRunThreads)
                {
                    data.add(new Double(dataStream.readDouble()));
                }
            }
            catch (EOFException eofException)
            {
                // server closed the stream.  This is normal at end of
                // acquisition, so no need to perform special handling.
            }
            catch (IOException e)
            {
                System.err.println("Exception in data processing thread: "+e);
            }
            
            // do another close on the socket to ensure it's closed if we're
            // exiting threads
            
            try
            {
                s.close();
            }
            catch (Exception e)
            {
            }
        }
    }
    
    /**
     * Creates new ChannelDataSocket 
     *
     * @param port  port on which the socket should listen for data connections
     */
    public ChannelDataSocket(int port) {
        dataConnectionPort=port;   
    }
    
    public int getPort() {
    	return dataConnectionPort;
    }
    
    /**
     * When we're destroyed, make sure we stop our threads and unbind our
     * listening port
     */
    protected void finalize() throws Throwable {
        stopProcessing();
    }
    
    public synchronized boolean isProcessing() {
    	return bRunThreads;
    }
    
    /**
     * Start listening for data connections from the AcqKnowledge server and
     * spawn the threads used for data processing.
     */
    public synchronized void startProcessing() {
        // if we were already running, perform a stopProcessing to halt any
        // previous threads and unblock the ports
        
        if(bRunThreads)
            stopProcessing();
        
        // start the new threads
        
        bRunThreads=true;
        data=Collections.synchronizedList(new ArrayList(8196));
        listeningThread=new ListeningThread();
        listeningThread.start();
    }
    
    /**
     * Stop processing any incoming data or connections.  Data that was
     * buffered prior to this call may still be accessed.
     */
    public synchronized void stopProcessing() {
        if(!bRunThreads)
            return;
        
        bRunThreads=false;
        
        // when stopping processing, we need to make a connection to our
        // server data socket to trigger it to close.
        
        try
        {
            Socket s=new Socket(InetAddress.getLocalHost(), dataConnectionPort);
            s.close();
        }
        catch (Exception e)
        {
        }
        
        // block until our listening thread has exited, freeing up the bound
        // ServerSocket
        
        while(listeningThread.isAlive())
        {
            try
            {
                listeningThread.join();
            }
            catch (InterruptedException e)
            {
                // our thread was interrupted while waiting for the join,
                // just try again to join on the other thread
            }
        }
    }
    
    /**
     * Get the total number of samples that have been retained in the memory
     * buffers.
     *
     * @return number of buffered samples
     */
    public synchronized int numSamples() {
    	if(data == null) {
    		return 0;
    	}
    	else {
    		return(data.size());
    	}
    }
    
    /**
     * Get the value of an individual sample in the internal buffers
     *
     * @param index sample index, should be in the range [0, numSamples())
     * @return sample value
     * @throws IndexOutOfBoundsException
     */
    public synchronized double getSample(int index) throws IndexOutOfBoundsException {
        return(((Double)data.get(index)).doubleValue());
    }
    
    /**
     * Strip a fixed number of samples at the beginning of the data buffer.
     * This can be used to reclaim memory after samples have been read
     * and processed.
     *
     * For situations where data acquisition is continuous, this should
     * be used periodically to release memory and avoid OutOfMemoryExceptions
     * in the thread receiving and appending the data.
     *
     * @param numSamples    number of samples to remove.  The samples in the
     *                      range [0, numSamples) will be erased from the
     *                      data array
     * @throws IndexOutOfBoundsException
     */
    public synchronized void removeSamplesFromBuffer(int numSamples) {
    	if(data == null) {
    		return;
    	}
    	
        if(numSamples > data.size())
            throw new IndexOutOfBoundsException();
        
        for(int i=numSamples-1; i>=0; i--)
            data.remove(i);
    }
    
    public synchronized double[] getSamples(int index, int n) throws IndexOutOfBoundsException {
    	double[] samples = new double[n];
    	for(int i=0; i < n; i++) {
    		samples[i] = (((Double)data.get(index)).doubleValue());
    	}
        return samples;
    }
}
