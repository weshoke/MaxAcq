/*
 * UDPChannelDataSocket.java
 *
 * Copyright 2006-2010, BIOPAC Systems, Inc.
 * All rights reserved
 */

package com.biopac.ndt;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.ByteArrayInputStream;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * This helper class allows for reception of channel data via a UDP socket
 * over which an AcqKnowledge server is delivering binary data.  This
 * is intended to be used in the multiple connection transfer type mode
 * with big endian double valued data type transfer streams.
 *
 * All of the channel data will be retained in memory for the lifetime of
 * this object.
 *
 * @author  edwardp
 * @version 
 */
public class UDPChannelDataSocket {
    private int dataConnectionPort;
    private boolean bRunThreads=true;
    private List data;
    
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
                DatagramSocket theSock=new DatagramSocket(dataConnectionPort);
		(new DataProcessingThread(theSock)).start();
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
        private DatagramSocket s;
        
        /**
         * Construct a new data processing thread to buffer data as it is
         * received over a TCP data connection
         *
         * @param sock  the socket to which the AcqKnowledge server is
         *              connected
         */
        public DataProcessingThread(DatagramSocket sock) {
            s=sock;
        }
        
        /**
         * Wait for incoming data on the socket and copy it into the internal
         * ChannelDataSocket object's data buffers
         */
        public void run() {
            try
            {
		s.setSoTimeout(5000);	// use 2 seconds...if we receive no data after 5 seconds assume server stopped sending data
		byte[] readBuf=new byte[512];
                DatagramPacket dataBuf=new DatagramPacket(readBuf, 512);
                while(bRunThreads)
                {
                    s.receive(dataBuf);
                    ByteArrayInputStream byteStream=new ByteArrayInputStream(dataBuf.getData());
                    DataInputStream dataStream=new DataInputStream(byteStream);
                    int startSample=dataStream.readInt(); // skip over sample number at start of packet
                    int numBytes=dataStream.readInt(); // get number of bytes in data section
                    for(int i=0; i<numBytes/8; i++)
                        data.add(new Double(dataStream.readDouble()));
                }
                s.close();
            }
            catch (InterruptedIOException ioe)
            {
                // server closed the stream.  This is normal at end of
                // acquisition, so no need to perform special handling.
            }
            catch (IOException e)
            {
                System.err.println("Exception in data processing thread (UDP): "+e);
            }
        }
    }
    
    /**
     * Creates new UDPChannelDataSocket 
     *
     * @param port  port on which the socket should listen for datagrams
     */
    public UDPChannelDataSocket(int port) {
        dataConnectionPort=port;   
    }
    
    /**
     * Start listening for data connections from the AcqKnowledge server and
     * spawn the threads used for data processing.
     */
    public void startProcessing() {
        bRunThreads=true;
        data=Collections.synchronizedList(new ArrayList(8196));
        (new ListeningThread()).start();
    }
    
    /**
     * Stop processing any incoming data or connections.  Data that was
     * buffered prior to this call may still be accessed.
     */
    public void stopProcessing() {
        bRunThreads=false;
    }
    
    /**
     * Get the total number of samples that have been retained in the memory
     * buffers.
     *
     * @return number of buffered samples
     */
    public long numSamples() {
        return(data.size());
    }
    
    /**
     * Get the value of an individual sample in the internal buffers
     *
     * @param index sample index, should be in the range [0, numSamples())
     * @return sample value
     * @throws IndexOutOfBoundsException
     */
    public double getSample(int index) throws IndexOutOfBoundsException {
        return(((Double)data.get(index)).doubleValue());
    }
}
