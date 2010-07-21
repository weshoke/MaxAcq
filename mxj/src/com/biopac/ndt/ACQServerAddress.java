/*
 * ACQServerAddress.java
 *
 * Copyright 2006-2010, BIOPAC Systems, Inc.
 * All rights reserved
 */

package com.biopac.ndt;

import java.net.InetAddress;

/**
 * Holds IP address and control port number of a located AcqKnowledge
 * server
 *
 * @author  edwardp
 * @version 
 */
public class ACQServerAddress {
        private InetAddress ipAddress;
        private short port;
        
        /**
         * Get the IP address of the remote machine
         */
        public InetAddress getAddress() { return(ipAddress); }
        
        /**
         * Get the control port of the remote machine that is used to deliver
         * XML-RPC requests
         */
        public short getControlPort() { return(port); }
        
        /**
         * Construct a new server adddress
         *
         * @param addr  remote IP address of the AcqKnowledge server
         * @param p     remote port of the AcqKnowledge server
         */
        public ACQServerAddress(InetAddress addr, short p) {
            ipAddress=addr;
            port=p;
        }
}
