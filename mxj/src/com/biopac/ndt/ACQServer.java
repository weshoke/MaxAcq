/*
 * ACQServer.java
 *
 * Copyright 2006-2010, BIOPAC Systems, Inc.
 * All rights reserved
 */

package com.biopac.ndt;

import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Java abstraction for AcqKnowledge server communications.  This class allows
 * for network communications for configuring and receiving data as it is
 * acquired.
 *
 * @author  edwardp
 * @version 
 */
public class ACQServer {
    // private members
    
    private ACQServerAddress myAddress=null;
    private XmlRpcClient myClient=null;
    
    // XML-RPC procedure names
    
    private static final String kGetMPUnitTypeName="acq.getMPUnitType";
    private static final String kGetEnabledChannelsName="acq.getEnabledChannels";
    private static final String kGetChannelScalingName="acq.getChannelScaling";
    private static final String kGetSamplingRateName="acq.getSamplingRate";
    private static final String kGetDownsamplingDividerName="acq.getDownsamplingDivider";
    private static final String kGetDataConnectionMethodName="acq.getDataConnectionMethod";
    private static final String kChangeDataConnectionMethodName="acq.changeDataConnectionMethod";
    private static final String kGetTransportTypeName="acq.getTransportType";
    private static final String kChangeTransportTypeName="acq.changeTransportType";
    private static final String kGetUDPBroadcastEnabledName="acq.getUDPBroadcastEnabled";
    private static final String kChangeUDPBroadcastEnabledName="acq.changeUDPBroadcastEnabled";
    private static final String kGetSingleConnectionModePortName="acq.getSingleConnectionModePort";
    private static final String kChangeSingleConnectionModePortName="acq.changeSingleConnectionModePort";
    private static final String kGetDataDeliveryEnabledName="acq.getDataDeliveryEnabled";
    private static final String kChangeDataDeliveryEnabledName="acq.changeDataDeliveryEnabled";
    private static final String kGetMostRecentSampleValueDeliveryEnabledName="acq.getMostRecentSampleValueDeliveryEnabled";
    private static final String kChangeMostRecentSampleValueDeliveryEnabledName="acq.changeMostRecentSampleValueDeliveryEnabled";
    private static final String kGetDataConnectionPortName="acq.getDataConnectionPort";
    private static final String kChangeDataConnectionPortName="acq.changeDataConnectionPort";
    private static final String kGetDataTypeName="acq.getDataType";
    private static final String kChangeDataTypeName="acq.changeDataType";
    private static final String kGetMostRecentSampleValueName="acq.getMostRecentSampleValue";
    private static final String kGetMostRecentSampleValueArrayName="acq.getMostRecentSampleValueArray";
    private static final String kGetAcquisitionInProgressName="acq.getAcquisitionInProgress";
    private static final String kToggleAcquisitionName="acq.toggleAcquisition";
    private static final String kLoadTemplateName="acq.loadTemplate";
    private static final String kSetUDPPacketSizeName="acq.setUDPPacketSize";
    private static final String kGetUDPPacketSizeName="acq.getUDPPacketSize";
    private static final String kGetDataConnectionHostnameName="acq.getDataConnectionHostname";
    private static final String kChangeDataConnectionHostnameName="acq.changeDataConnectionHostname";
    private static final String kSetOutputChannelName="acq.setOutputChannel";
    private static final String kSetDataConnectionTimeoutSecName="acq.setDataConnectionTimeoutSec";
    
    // XML-RPC structure keys
    
    private static final String kChannelTypeKey="type";
    private static final String kChannelIndexKey="index";
    private static final String kScalingScaleKey="scale";
    private static final String kScalingOffsetKey="offset";
    private static final String kDataTypeTypeKey="type";
    private static final String kDataTypeEndianKey="endian";
    private static final String kChannelValueStructIndexKey="channel";
    private static final String kChannelValueStructValueKey="value";
    
    // channel type identifiers used as function parameters
    
    /**
     * Used to indicate an analog channel.  This is a physical channel on the
     * MP device
     */
    public static final String kAnalogChannelType="analog";
    
    /**
     * Used to indicate a digital channel.  This is a physical channel on the
     * MP device.  Not all MP devices have digital channels.
     */
    public static final String kDigitalChannelType="digital";
    
    /**
     * Used to indicate a calculation channel.  These are channels with
     * derived results that are calculated by the AcqKnowledge software
     */
    public static final String kCalcChannelType="calc";
    
    // connection method types for indicating how data is sent to the client,
    // one connection per channel or interleaved on a single connection
    
    /**
     * Used to indicate that data for all channels will be sent over a single
     * network connection from the server.  This interleaves all of the data
     * in a single stream.
     */
    public static final String kSingleConnectionDelivery="single";
    
    /**
     * Used to indicate that mutiple connections will be established from the
     * server to the client.  This uses a unique connection for each channel
     * of data.
     */
    public static final String kMultipleConnectionDelivery="multiple";
    
    // transport type constants
    
    /**
     * Indicates data connections will be made to the client using TCP
     */
    public static final String kTCPTransportType="tcp";
    
    /**
     * Indicates data connections will be made to the client using UDP
     */
    public static final String kUDPTransportType="udp";
    
    // data type constnats
    
    /**
     * Indicates data is delivered as 64 bit floating point
     */
    public static final String kDoubleDataType="double";
    
    /**
     * Indicates data is delivered as 32 bit floating point
     */
    public static final String kFloatDataType="float";
    
    /**
     * Indicates data is delivered as 16 bit integers
     */
    public static final String kShortDataType="short";
    
    /**
     * Indicates data is delivered in big endian byte ordering
     */
    public static final String kBigEndian="big";
    
    /**
     * Indicates data is delivered in little endian byte ordering
     */
    public static final String kLittleEndian="little";
    
    // functions
    
    /**
     * Creates a new ACQServer for communicating with the AcqKnowledge network
     * data transfer facility.
     *
     * @param addr  address of AcqKnowledge server.  Provides both the IP
     *              address as well as port over which the server is listening
     *              for control connections
     */
    public ACQServer(ACQServerAddress addr) {
        myAddress=addr;
        createXmlRpcClient();
    }
	
	/**
	 * Gets the address used to make connections to the AcqKnowledge server.
	 *
	 * @return current address of the server, or null if there is no set
	 *	address
	 */
	public ACQServerAddress serverAddress() {
		return(myAddress);
	}
    
    /**
     * Change the server address used to connect to the AcqKnowledge server.
     *
     * @param addr  new server address
     */
    public void changeAddress(ACQServerAddress addr) {
        myAddress=addr;
        createXmlRpcClient();
    }
    
    /**
     * Create a client instance we can use to send control requests to the
     * AcqKnowledge server.
     */
    private void createXmlRpcClient() {
        XmlRpcClientConfigImpl config=new XmlRpcClientConfigImpl();
        try
        {
//			System.out.println("RPX-URL: "+"http://"+myAddress.getAddress().getHostAddress()+":"+myAddress.getControlPort()+"/RPC2");
            config.setServerURL(new URL("http://"+myAddress.getAddress().getHostAddress()+":"+myAddress.getControlPort()+"/RPC2"));
        }
        catch (java.net.MalformedURLException e)
        {
            System.err.println("Invalid address for ACQ server!");
        }
        myClient=new XmlRpcClient();
        myClient.setConfig(config);
    }
    
    /**
     * Fetch the model number of MP unit to which AcqKnowledge is conncted.
     *
     * @return 100, 150, 35, 45
     * @throws ACQServer.ServerException
     */
    public short getMPUnitType() throws ProtocolException {
        short toReturn=0;
        
        try
        {
            Object result=myClient.execute(kGetMPUnitTypeName, new Vector());
            if(!(result instanceof Integer))
                throw new ProtocolException();
            toReturn=((Integer)result).shortValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(toReturn);
    }
    
    /**
     * Fetch a list of indexes of enabled analog channels
     *
     * @return Vector of 0 based Integer indexes of the channels that are
     *  enabled for acquisition
     * @throws ProtocolException on error
     */
    public Vector getEnabledAnalogChannels() throws ProtocolException {
        Vector retVal=null;
        
        try
        {
            Vector params=new Vector();
            params.add(kAnalogChannelType);
            Object result=myClient.execute(kGetEnabledChannelsName, params);
            if(!(result instanceof Object[]))
                throw new ProtocolException("Unknown response type");
            retVal=new Vector(Arrays.asList((Object[])result));
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(retVal);
    }
    
    /**
     * Fetch a list of indexes of enabled digital channels.
     *
     * @return Vector of 0 based Integer indexes of the channels that are
     *  enabled for acquisition
     * @throws ProtocolException on error
     */
    public Vector getEnabledDigitalChannels() throws ProtocolException {
        Vector retVal=null;
        
        try
        {
            Vector params=new Vector();
            params.add(kDigitalChannelType);
            Object result=myClient.execute(kGetEnabledChannelsName, params);
            if(!(result instanceof Object[]))
                throw new ProtocolException("Unknown response type");
            retVal=new Vector(Arrays.asList((Object[])result));
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(retVal);
    }
    
    /**
     * Fetch a list of indexes of enabled calcluation channels.
     *
     * @return Vector of 0 based Integer indexes of the channels that are
     *  enabled for acquisition
     * @throws ProtocolException on error
     */
    public Vector getEnabledCalculationChannels() throws ProtocolException {
        Vector retVal=null;
        
        try
        {
            Vector params=new Vector();
            params.add(kCalcChannelType);
            Object result=myClient.execute(kGetEnabledChannelsName, params);
            if(!(result instanceof Object[]))
                throw new ProtocolException("Unknown response type");
            retVal=new Vector(Arrays.asList((Object[])result));
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(retVal);
    }
    
    /**
     * Helper function for returning the array of enabled channels by specifying
     * channel type as a parameter instead of by method name.
     *
     * @param chanType  channel type, one of the k*ChannelType constants
     * @return Vector of Integer objects with indexes of the channels of that
     *  type that are enabled for acquisition and can also be enabled for
     *  data delivery or getMostRecentSampleValue tracking.
     * @throws ProtocolException
     */
    public Vector getEnabledChannels(String chanType) throws ProtocolException {
        Vector enabledIndexes;
        
        if(chanType.equals(kAnalogChannelType))
            enabledIndexes=getEnabledAnalogChannels();
        else if(chanType.equals(kDigitalChannelType))
            enabledIndexes=getEnabledDigitalChannels();
        else if(chanType.equals(kCalcChannelType))
            enabledIndexes=getEnabledCalculationChannels();
        else
            throw new ProtocolException("Unrecognized channel type");
        
        return(enabledIndexes);
    }
    
    /**
     * Helper function for determining if a specific channel of a specific
     * type is enabled for acquisition.  Only channels that are enabled for
     * acquisition can be configured for data delivery and getMostRecentSampleValue
     * calls.
     *
     * @param chanType  channel type to check, one of the k*ChannelType constants
     * @param chanIndex 0 based channel index to check
     * @return true if the channel is enabled, false if not
     * @throws ProtocolException
     */
    public boolean isChannelEnabled(String chanType, short chanIndex) throws ProtocolException {
        Vector enabledIndexes;
        
        if(chanType.equals(kAnalogChannelType))
            enabledIndexes=getEnabledAnalogChannels();
        else if(chanType.equals(kDigitalChannelType))
            enabledIndexes=getEnabledDigitalChannels();
        else if(chanType.equals(kCalcChannelType))
            enabledIndexes=getEnabledCalculationChannels();
        else
            throw new ProtocolException("Unrecognized channel type");
        
        
        return(enabledIndexes.contains(new Integer(chanIndex)));
    }
    
    /**
     * Get the additive offset factor that can be applied to data
     * delivered as signed integers to convert into physical units.
     *
     * @param chanType  string identifier providing the channel type.  Must
     *                  be one of the k*ChannelType constants defined in this
     *                  class
     * @param index     0 based index of the channel
     * @return additive offset factor
     * @throws ProtocolException on error
     */
    public double getAmplOffsetFactor(String chanType, short index) throws ProtocolException {
        double retVal;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetChannelScalingName, params);
            // returns a scaling structure with two keys
            if(!(result instanceof Map))
                throw new ProtocolException("Unexpected return type");
            Map scalingStruct=(Map)result;
            if(!(scalingStruct.containsKey(kScalingOffsetKey)))
                throw new ProtocolException("Scaling structure does not contain offset key");
            retVal=((Double)scalingStruct.get(kScalingOffsetKey)).doubleValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(retVal);
    }
    
    /**
     * Get the multiplicative scaling factor that can be applied to data
     * delivered as signed integers to convert into physical units.
     *
     * @param chanType  string identifier providing the channel type.  Must
     *                  be one of the k*ChannelType constants defined in this
     *                  class
     * @param index     0 based index of the channel
     * @return multiplicative scaling factor
     * @throws ProtocolException on error
     */
    public double getAmplScaleFactor(String chanType, short index) throws ProtocolException {
        double retVal;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetChannelScalingName, params);
            // returns a scaling structure with two keys
            if(!(result instanceof Map))
                throw new ProtocolException("Unexpected return type");
            Map scalingStruct=(Map)result;
            if(!(scalingStruct.containsKey(kScalingScaleKey)))
                throw new ProtocolException("Scaling structure does not contain scaling key");
            retVal=((Double)scalingStruct.get(kScalingScaleKey)).doubleValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(retVal);
    }
    
    /**
     * Make a new Hashtable that can is used to convey a channel type and
     * index to the AcqKnowledge server in XML-RPC requests.
     *
     * @param chanType  string identifier providing the channel type.  Must
     *                  be one of the k*ChannelType constants defined in this
     *                  class
     * @param index     0 baesd index of the channel
     * @return Hashtable filled with appropriate values for use as an XML-RPC
     *  channel parameter structure
     */
    private Hashtable xmlrpcChanParamStruct(String chanType, short index) {
        Hashtable chanStruct=new Hashtable();
        chanStruct.put(kChannelTypeKey, chanType);
        chanStruct.put(kChannelIndexKey, new Integer(index));
        return(chanStruct);
    }
    
    /**
     * Make a new Hashtable that can be used to convey a data type and endian
     * to the AcqKnowledge server in XML-RPC requests.
     *
     * @param dataType  string identifier for the binary type, one of the
     *                  k*DataType constants
     * @param endian    string identifier for the byte ordering, one of the
     *                  k*Endian constants
     * @return Hashtable filled with appropriate data type structure info
     */
    private Hashtable dataTypeParamStruct(String dataType, String endian) {
        Hashtable dataTypeStruct=new Hashtable();
        dataTypeStruct.put(kDataTypeTypeKey, dataType);
        dataTypeStruct.put(kDataTypeEndianKey, endian);
        return(dataTypeStruct);
    }
    
    /**
     * Return the sampling rate at which data is being acquired.
     *
     * @return sampling rate in Hertz units
     * @throws ProtocolException on error
     */
    public double getSamplingRate() throws ProtocolException {
        double samplingRate;
        
        try
        {
            Object result=myClient.execute(kGetSamplingRateName, new Vector());
            if(!(result instanceof Double))
                throw new ProtocolException("Unexpected return type");
            samplingRate=((Double)result).doubleValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(samplingRate);
    }
    
    /**
     * Return the divider used to reduce the sampling rate for a specific
     * channel during acquisitions.  Channels acquired at the full rate
     * have a divider of 1.
     *
     * To compute the actual sampling rate of incoming data for an individual
     * channel, divide the result of getSamplingRate() by the divider returned
     * by this function.
     *
     * This integer divisor is also used to determine skipped frames for
     * interleaved data delivery.
     *
     * @param chanType  channel type identifier.  Must be one of the class
     *                  k*ChannelType constants
     * @param index     0 based channel index of the channel to query
     * @return divider used to reduce the sampling rate of an individual channel
     * @throws ProtocolException
     * @see getSamplingRate
     */
    public int getSamplingRateDivider(String chanType, short index) throws ProtocolException {
        int divider;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetDownsamplingDividerName, params);
            if(!(result instanceof Integer))
                throw new ProtocolException("Unexpected return type");
            divider=((Integer)result).intValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(divider);
    }
    
    /**
     * Return the current data connection method that is being used to send
     * information to the client.  This may be either all of the data on a
     * single connection interleaved, or multiple connections (one per channel)
     *
     * @return connection method identifier
     * @throws ProtocolException
     * @see changeDataConnectionMethod
     * @see kSingleConnectionDelivery
     * @see kMultipleConnectionDelivery
     */
    public String getDataConnectionMethod() throws ProtocolException {
        String dataConnectionMethod;
        
        try
        {
            Object result=myClient.execute(kGetDataConnectionMethodName, new Vector());
            if(!(result instanceof String))
                throw new ProtocolException("Unexpected return type");
            dataConnectionMethod=(String)result;
            if(!(dataConnectionMethod.equals(kSingleConnectionDelivery) || dataConnectionMethod.equals(kMultipleConnectionDelivery)))
                throw new ProtocolException("Unrecognized data connection method type");
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(dataConnectionMethod);
    }
    
    /**
     * Change the data connection method type that is used to transfer data
     * from the server to the client.  Data can be delivered either interleaved
     * over a single connection or sent using one connection per channel.
     *
     * @param newMethod new data trahsfer method, either kSingleConnection
     *                  delivery or kMultipleConnectionDelivery
     * @see getDataConnectionMethod
     * @see kSingleConnectionDelivery
     * @see kMultipleConnectionDelivery
     * @throws ProtocolException
     */
    public void changeDataConnectionMethod(String newMethod) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(newMethod);
            myClient.execute(kChangeDataConnectionMethodName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Fetch the transport type that is currently being used to establish data
     * connections to the client.
     *
     * @return transport type constant, kTCPTransportType or kUDPTransportType
     * @throws ProtocolException
     */
    public String getTransportType() throws ProtocolException {
        String transportType;
        
        try
        {
            Object result=myClient.execute(kGetTransportTypeName, new Vector());
            if(!(result instanceof String))
                throw new ProtocolException("Unknwon return type!");
            transportType=(String)result;
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(transportType);
    }
    
    /**
     * Change the transport type that is used to establish data connections to
     * the client.
     *
     * @param newTransportType  kTCPTransportType or kUDPTransportType
     * @throws ProtocolException
     */
    public void changeTransportType(String newTransportType) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(newTransportType);
            myClient.execute(kChangeTransportTypeName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Determine whether UDP broadcast is used for data delivery.  If enabled
     * and the transport type is set to UDP, data packets will be sent to the
     * broadcast address of the network.
     *
     * @return true if broadcasting is enabled, false if disabled
     * @throws ProtocolException
     */
    public boolean getUDPBroadcastEnabled() throws ProtocolException {
        boolean isEnabled;
        try
        {
            Object result=myClient.execute(kGetUDPBroadcastEnabledName, new Vector());
            if(!(result instanceof Boolean))
                throw new ProtocolException("Unknown return type!");
            isEnabled=((Boolean)result).booleanValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(isEnabled);
    }
    
    /**
     * Change whether UDP broadcast is used for data delivery.  Broadcast is
     * only possible to use when the transport type is set to udp.
     *
     * @param useBroadcast  true to broadcast data to network, false to
     *                      send to client only
     * @throws ProtocolException
     */
    public void changeUDPBroadcastEnabled(boolean useBroadcast) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(new Boolean(useBroadcast));
            myClient.execute(kChangeUDPBroadcastEnabledName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Get the port number on which data will be delivered if the connection
     * transfer type is set to "single" for putting all interleaved data over
     * a single connection.  If the transfer type is set to multiple, this
     * port is not used.
     *
     * @return current port number for single connections
     * @see getPortForChannel
     * @throws ProtocolException
     */
    public short getSingleConnectionPort() throws ProtocolException {
        short thePort;
        
        try
        {
            Object result=myClient.execute(kGetSingleConnectionModePortName, new Vector());
            if(!(result instanceof Integer))
                throw new ProtocolException("Unknown return type!");
            thePort=((Integer)result).shortValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(thePort);
    }
    
    /**
     * Change the port number on which data will be delivered if the connection
     * transfer type is set to "single" for putting all interleaved data over
     * a single connection.  If the transfer type is set to multiple, this
     * port is not used.
     *
     * @param newPort   new port on which single mode connections should be
     *                  made
     * @see changePortForChannel
     * @throws ProtocolException
     */
    public void changeSingleConnectionPort(short newPort) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(new Integer(newPort));
            myClient.execute(kChangeSingleConnectionModePortName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Check whether a specific channel is enabled for data delivery.  Only
     * channels that are enabled will be sent over the network to the client.
     *
     * @param chanType  type of the channel to be checked, k*ChannelType constant
     * @param index     zero based index of the channel to check
     * @return true if the channel will be sent to the client, false if not
     * @throws ProtocolException
     */
    public boolean getDataDeliveryEnabled(String chanType, short index) throws ProtocolException {
        boolean enabled;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetDataDeliveryEnabledName, params);
            if(!(result instanceof Boolean))
                throw new ProtocolException("Unexpected return type!");
            enabled=((Boolean)result).booleanValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(enabled);
    }
    
    /**
     * Change whether a specific channel is enabled for data delivery.  Only
     * channels that are enabled will be sent over the network to the client.
     *
     * @param chanType  type of the channel to modify, k*ChannelType constant
     * @param index     zero based index of the channel to check
     * @param isEnabled true to enable the channel for delivery, false to
     *                  suppress network transfer for the channel
     * @throws ProtocolException
     */
    public void changeDataDeliveryEnabled(String chanType, short index, boolean isEnabled) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            params.add(new Boolean(isEnabled));
            myClient.execute(kChangeDataDeliveryEnabledName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Helper function to disable data delivery for all channels.
     *
     * @throws ProtocolException
     */
    public void disableAllDataDelivery() throws ProtocolException {
        Vector enabledIndexes;
        
        enabledIndexes=getEnabledAnalogChannels();
        Enumeration e=enabledIndexes.elements();
        while(e.hasMoreElements())
            changeDataDeliveryEnabled(kAnalogChannelType, ((Integer)e.nextElement()).shortValue(), false);
        enabledIndexes=getEnabledDigitalChannels();
        e=enabledIndexes.elements();
        while(e.hasMoreElements())
            changeDataDeliveryEnabled(kDigitalChannelType, ((Integer)e.nextElement()).shortValue(), false);
        enabledIndexes=getEnabledCalculationChannels();
        e=enabledIndexes.elements();
        while(e.hasMoreElements())
            changeDataDeliveryEnabled(kCalcChannelType, ((Integer)e.nextElement()).shortValue(), false);
    }
    
    /**
     * Fetch whether a channel is enabled for getMostRecentSample requests.
     * Channels that are enabled for this tracking can have their values
     * queried during acquisitions using XML-RPC polling.
     *
     * @param chanType  type of the channel to query, k*ChannelType constant
     * @param index     zero based index of the channel to check
     * @return true if getMostRecentSample value tracking is enabled, false
     *  if disabled
     * @throws ProtocolException
     */
    public boolean getMostRecentSampleEnabled(String chanType, short index) throws ProtocolException {
        boolean enabled;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetMostRecentSampleValueDeliveryEnabledName, params);
            if(!(result instanceof Boolean))
                throw new ProtocolException("Unexpected return type!");
            enabled=((Boolean)result).booleanValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(enabled);
    }
    
    /**
     * Change whether a channel is enabled for getMostRecentSample value tracking.
     * Channels that are enabled are allowed to be polled during acquisitions
     * with XML-RPC requests to get the most recent value acquired for the
     * channel.
     *
     * @param chanType  type of channel to modify, one of the k*ChannelType constants
     * @param index      0 based index of the channel
     * @param newEnable true to enable getMostRecentSample tracking, false
     *                  to disable
     * @throws ProtocolException
     */
    public void changeMostRecentSampleEnabled(String chanType, short index, boolean newEnable) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            params.add(new Boolean(newEnable));
            myClient.execute(kChangeMostRecentSampleValueDeliveryEnabledName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Fetch the port on which connections will be made to deliver the channel's
     * data if the transfer type is configured to "multiple".
     *
     * @param chanType  type of channel to query, one of the k*ChannelType constants
     * @param index     0 based index of the channel
     * @return port number for multiple connection transfer type
     * @throws ProtocolException
     */
    public short getConnectionPort(String chanType, short index) throws ProtocolException {
        short thePort;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetDataConnectionPortName, params);
            if(!(result instanceof Integer))
                throw new ProtocolException("Unexpected return type!");
            thePort=((Integer)result).shortValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(thePort);
    }
    
    /**
     * Change the port on which connections will be made to deliver the channel's
     * data if the transfer type is configure to "multiple".
     *
     * @param chanType  type of channel whose port is to be changed, one of the
     *                  k*ChannelType constants
     * @param index     0 based index of the channel
     * @param newPort   new port number to use for the channel
     * @throws ProtocolException
     */
    public void changeConnectionPort(String chanType, short index, short newPort) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            params.add(new Integer(newPort));
            myClient.execute(kChangeDataConnectionPortName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Get the binary type being used to transfer the data for a channel over
     * the data connection.
     *
     * @param chanType  type of channel to query, one of the k*ChannelType constants
     * @param index     0 based index of the channel
     * @return k*Type constant
     * @throws ProtocolException
     */
    public String getBinaryType(String chanType, short index) throws ProtocolException {
        String theType;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetDataTypeName, params);
            if(!(result instanceof Map))
                throw new ProtocolException("Unexpected return type!");
            Map typeStruct=(Map)result;
            if(!typeStruct.containsKey(kDataTypeTypeKey))
                throw new ProtocolException("Data type structure missing type key");
            theType=(String)(typeStruct.get(kDataTypeTypeKey));
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(theType);
    }
    
    /**
     * Change the data type used to transmit the data of the channel over the
     * data connection.  This maintains the endian currently in use.
     *
     * @param chanType  type of channel whose data type should be modified
     * @param index     0 based index of channel
     * @param newType   new data format for channel, one of the k*DataType strings
     * @throws ProtocolException
     */
    public void changeBinaryType(String chanType, short index, String newType) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            params.add(dataTypeParamStruct(newType, getBinaryEndian(chanType, index)));
            myClient.execute(kChangeDataTypeName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Fetch the current byte order used to deliver data over the data connection
     * for the channel.
     *
     * @param chanType  type of channel whose endian should be returned
     * @param index     0 based index of channel
     * @return k*Endian constant indicating current endian of data delivery
     * @throws ProtocolException
     */
    public String getBinaryEndian(String chanType, short index) throws ProtocolException {
        String theEndian;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetDataTypeName, params);
            if(!(result instanceof Map))
                throw new ProtocolException("Unexpected return type!");
            Map typeStruct=(Map)result;
            if(!typeStruct.containsKey(kDataTypeEndianKey))
                throw new ProtocolException("Data type structure missing endian key");
            theEndian=(String)(typeStruct.get(kDataTypeEndianKey));
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(theEndian);
    }
    
    /**
     * Change the byte endian used for delivery of data for the channel over
     * the data connection.  This maintains the current data type in use.
     *
     * @param chanType  type of channel, one of the k*ChannelType constants
     * @param index     0 based index of channel
     * @param newEndian new endian ordering to use, one of the k*Endian constants
     * @throws ProtocolException
     */
    public void changeBinaryEndian(String chanType, short index, String newEndian) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            params.add(dataTypeParamStruct(getBinaryType(chanType, index), newEndian));
            myClient.execute(kChangeDataTypeName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Get the most recent sample value of a channel with all scaling applied.
     * This will fail if no acquisition is in progress or if the channel
     * was not enabled for most recent sample value tracking.
     *
     * @param chanType  type of channel, one of the k*ChannelType constants
     * @param index     0 based index of channel
     * @return most recently acquired sample value
     * @throws ProtocolException
     */
    public double getMostRecentSampleValue(String chanType, short index) throws ProtocolException {
        double value;
        
        try
        {
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            Object result=myClient.execute(kGetMostRecentSampleValueName, params);
            if(!(result instanceof Double))
                throw new ProtocolException("Unexpected return type");
            value=((Double)result).doubleValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(value);
    }
    
    /**
     * Return the most recent sample values for all channels of a given type.
     * There must be an acquisition in progress for this call to succeed.
     * Only channels that have had recent value tracking enabled will have
     * values returned.
     *
     * @param chanType  channel type to fetch, one of the k*ChannelType constants
     * @return Hashtable with Integer keys of 0-based channel index and Double
     *  values with scaled results
     * @throws ProtocolException
     */
    public Hashtable getAllMostRecentSampleValues(String chanType) throws ProtocolException {
        Hashtable toReturn=new Hashtable();
        
        try
        {
            Object result=myClient.execute(kGetMostRecentSampleValueArrayName, new Vector());
            if(!(result instanceof Object[]))
                throw new ProtocolException("Unexpected return type!");
            Object[] valueStructArray=(Object[])result;
            for(int i=0; i<valueStructArray.length; i++)
            {
                if(!(valueStructArray[i] instanceof Map))
                    throw new ProtocolException("Unexpected return type!");
                Map valueStruct=(Map)valueStructArray[i];
                if(!valueStruct.containsKey(kChannelValueStructIndexKey))
                    throw new ProtocolException("Missing index key");
                if(!valueStruct.containsKey(kChannelValueStructValueKey))
                    throw new ProtocolException("Missing value key");
                
                // extract value
                
                Object theValueAsObject=valueStruct.get(kChannelValueStructValueKey);
                if(!(theValueAsObject instanceof Double))
                    throw new ProtocolException("Unexpected type for channel value!");
                Double theValue=(Double)theValueAsObject;
                
                // check if channel matches our requested type and, if so,
                // add its value to our result hash
                
                Object channelIndexStruct=valueStruct.get(kChannelValueStructIndexKey);
                if(!(channelIndexStruct instanceof Map))
                    throw new ProtocolException("Unexpected cahnnel index type!");
                Map channelIndexMap=(Map)channelIndexStruct;
                if(channelIndexMap.containsKey(kChannelTypeKey) && (((String)channelIndexMap.get(kChannelTypeKey)).equals(chanType)))
                {
                    toReturn.put(channelIndexMap.get(kChannelIndexKey), theValue);
                }
            }
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(toReturn);
    }
    
    /**
     * Determine if there is any acquisition in progress by the server.  As
     * there may be multiple graphs open on the server, this will return
     * true if any graph is acquiring.
     *
     * @return true if data is being acquired, false if not
     * @throws ProtocolException
     */
    public boolean isAcquisitionInProgress() throws ProtocolException {
        boolean acqInProgress;
        
        try
        {
            Object result=myClient.execute(kGetAcquisitionInProgressName, new Vector());
            if(!(result instanceof Boolean))
                throw new ProtocolException("Unexpected return type!");
            acqInProgress=((Boolean)result).booleanValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(acqInProgress);
    }
    
    /**
     * Toggle the acquisition of data from the hardware.  If acquisition
     * is in progress, it is halted.  If there is no acquisition in
     * progress, it is started.
     *
     * @see isAcquisitionInProgress
     * @throws ProtocolException
     */
    public void toggleAcquisition() throws ProtocolException {
        try
        {
            myClient.execute(kToggleAcquisitionName, new Vector());
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Given a binary graph template in base64, send the template data over
     * to the server and open a new graph window configured to use the
     * acquisition settings from the template.
     *
     * @param templateData  base64 encoded template data
     * @throws ProtocolException
     */
    public void loadTemplate(byte[] templateData) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(templateData);
            myClient.execute(kLoadTemplateName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Change the number of bytes in each UDP packet for UDP transfer types.
     *
     * @param newSize   new number of bytes in teh packet
     * @throws ProtocolException
     */
    public void setUDPPacketSize(int newSize) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(new Integer(newSize));
            myClient.execute(kSetUDPPacketSizeName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Fetch the number of bytes in each UDP packet for UDP transfer types.
     *
     * @return number of bytes in a single datagram
     * @throws ProtocolException
     */
    public int getUDPPacketSize() throws ProtocolException {
        int toReturn;
        
        try
        {
            Object result=myClient.execute(kGetUDPPacketSizeName, new Vector());
            if(!(result instanceof Integer))
                throw new ProtocolException("Unexpected return type!");
            toReturn=((Integer)result).intValue();
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(toReturn);
    }
    
    /**
     * Fetch the hostname of the machine to which data connections will
     * be established by the server.
     *
     * @return hostname, or empty string indicating data connections will be
     *  made to the most recent machine that made a client call
     * @throws ProtocolException
     */
    public String getDataConnectionHostname() throws ProtocolException {
        String toReturn;
        
        try
        {
            Object result=myClient.execute(kGetDataConnectionHostnameName, new Vector());
            if(!(result instanceof String))
                throw new ProtocolException("Unexpected return type!");
            toReturn=(String)result;
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
        
        return(toReturn);
    }
    
    /**
     * Change the hostname of the machine to which data connections will be
     * established by the server.
     *
     * @param newhostname   hostname to set, or empty string to make data
     *                      connections to the most recent machine making
     *                      control connectins
     * @throws ProtocolException
     */
    public void changeDataConnectionHostname(String newHostname) throws ProtocolException {
        try
        {
            Vector params=new Vector();
            params.add(newHostname);
            myClient.execute(kChangeDataConnectionHostnameName, params);
        }
        catch (XmlRpcException e)
        {
            throw new ProtocolException(e.toString());
        }
    }
    
    /**
     * Change the voltage of an ouptut channel on the MP device to which
     * AcqKnowledge is connected.  For MP36-R devices, only digital 1-8
     * output may be changed.  For MP150 devices, Analog Out 0/1 and
     * digital outputs 1-16 may be toggled.
     *
     * @param chanType	string identifier providing the channel type.  Must
     *					be one of the k*ChannelType constants defined in this
     *					class.  Only analog and digital channel types are
     *					valid.
     * @param index		0 based index of the channel.
     * @param val		new voltage value for the output channel.  For
     *					digital outputs, this should be either 0 or 5.
     * @throws ProtocolException
     */
    public void setOutputChannel(String chanType, short index, double val) throws ProtocolException {
    	try
    	{
            Vector params=new Vector();
            params.add(xmlrpcChanParamStruct(chanType, index));
            params.add(val);
            myClient.execute(kSetOutputChannelName, params);
    	}
    	catch (XmlRpcException e)
    	{
    		throw new ProtocolException(e.toString());
    	}
    }
    
    /**
     * Change the timeout AcqKnowledge uses for closing data connections.
     * When an acquisition completes, AcqKnowledge may continue to keep
     * the data connections open for a specific period of time to 
     * continue delivering any data that has not yet been transferred
     * to the client application.
     *
     * The default timeout of 0 closes the data connections immediately
     * when the acquisition halts.
     *
     * @param sec		number of seconds to keep data connections
     *					open at the end of acquisitions.
     */
    public void setDataConnectionTimeoutSec(int sec) throws ProtocolException {
    	try
    	{
    		Vector params=new Vector();
    		params.add(sec);
    		myClient.execute(kSetDataConnectionTimeoutSecName, params);
    	}
    	catch (XmlRpcException e)
    	{
    		throw new ProtocolException(e.toString());
    	}
    }
}
