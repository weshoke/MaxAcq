/*
 * ServerDiscoveryHelper.java
 *
 * Copyright 2006-2010, BIOPAC Systems, Inc.
 * All rights reserved
 */

package com.biopac.ndt;
import java.net.InetAddress;
import java.util.Vector;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.StringTokenizer;
import java.lang.String;
import java.lang.Integer;

/**
 * Contains methods that can be used to locate any AcqKnowledge servers that
 * are on the local network and configured to respond to automatic discovery
 * requests.  Once located, instances of the ACQServer class can be used to
 * interact with servers.
 *
 * @author  edwardp
 * @version 
 */
public class ServerDiscoveryHelper {   
    private Vector locatedServers=new Vector();
    
    /**
     * Creates new NetworkDataTransfer
     */
    public ServerDiscoveryHelper() {
    }
    
    /**
     * Return a vector of the servers that were discovered through the last
     * locateServers() invocation
     *
     * @return vector of ACQServerAddress instances
     * @see locateServers
     */
    public Vector getServerList() { return locatedServers; }
    
    private static final short serverDiscoveryPort=15012;
    private static final String serverDiscoveryRequest="AcqP Client";
    private static final String serverResponse="AcqP Server Port";
    private static final int maxServers=128;
    
    /**
     * Send a broadcast packet to locate AcqKnowledge servers on the network
     * and assemble a list of located servers and the port on which they
     * are listening for control connections
     *
     * @throws java.io.IOException
     */
    public void locateServers() throws java.io.IOException, java.net.SocketException {
        // clear out our existing set of known servers
        
        locatedServers.clear();
        
        // broadcast location packet
        
        DatagramSocket discoverySocket=new DatagramSocket();
        DatagramPacket discoveryRequest=new DatagramPacket(serverDiscoveryRequest.getBytes(), serverDiscoveryRequest.length(), InetAddress.getByName("255.255.255.255"), serverDiscoveryPort);
        discoverySocket.send(discoveryRequest);
        
        // wait for servers to reply.  Each reply should begin with our
        // server response and be followed by the port number of the
        // control connection.
        
        discoverySocket.setSoTimeout(1000);
        try
        {
            byte[] responseBuf=new byte[512];
            DatagramPacket response=new DatagramPacket(responseBuf, responseBuf.length);
            
            for(int i=0; i<maxServers; i++)
            {
                discoverySocket.receive(response);
                
				if(response.getPort()==serverDiscoveryPort)
				{
					StringTokenizer tokens=new StringTokenizer(new String(responseBuf, 0, response.getLength()), ":");
					if(tokens.countTokens()==2) // we expect only the server reply and port number
					{
						String firstToken=tokens.nextToken();
						if(firstToken.equals(serverResponse))
						{
							locatedServers.add(new ACQServerAddress(response.getAddress(), (short)Integer.parseInt(tokens.nextToken())));
						}
					}
				}
            }
        }
        catch (java.io.InterruptedIOException timeoutException)
        {
            // we just timed out without getting any other responses
        }
    }
}
