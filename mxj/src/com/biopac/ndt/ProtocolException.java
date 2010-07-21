/*
 * ProtocolException.java
 *
 * Copyright 2006-2010, BIOPAC Systems, Inc.
 * All rights reserved
 */

package com.biopac.ndt;

/**
 * Thrown on communications errors when remote servers send along bad
 * responses from our protocol requests.
 *
 * @author  edwardp
 * @version 
 */
public class ProtocolException extends java.lang.Exception {

    /** Creates new ProtocolException */
    public ProtocolException() {
    }
    
    public ProtocolException(String e) {
        super(e);
    }
}
