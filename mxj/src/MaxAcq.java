import com.cycling74.max.*;
import com.cycling74.jitter.*;
import com.biopac.ndt.*;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Vector;
import java.util.Enumeration;
import java.net.InetAddress;
import java.io.File;
import java.io.FileInputStream;

public class MaxAcq extends MaxObject {
	private class DataConnection extends Object {
		public JitterMatrix matrix = new JitterMatrix(1, "float64", 20, 1);
		public ChannelDataSocket data_socket = null;
		public String channel_type = "";
		public int channel = 0;
	}
	
	// current server
	private ACQServer current_server = null;
	private int nsamples = 20;
	private boolean first_stream = true;
	private Map<String, DataConnection> connections = new HashMap<String, DataConnection>();
	static private int PORT = 16214;
	
//	DataConnection connection = new DataConnection();
	
	public MaxAcq() {
		declareOutlets(new int[]{DataTypes.ALL});
		connect();
	}

	// simple connection method (automagic)
	public void connect() {
		 Vector servers = locate_servers();
		 post(""+servers.size()+" servers found");
	     guess_server(servers);
	}
	
	public void getconnected() {
		outlet(0, new Atom[] {
				Atom.newAtom("connected"), 
				Atom.newAtom(current_server == null ? 0 : 1)
			});
	}

	public void loadTemplate(String s) {
		// load the specified test template onto the server so we
		// can toggle acquisitions and have a valid channel configuration
		if(current_server != null) {
			String filename = s;
			if(s.indexOf(":/") >= 0) {
				filename = s.substring(s.indexOf(":/")+1);
			}
			
			File template_file = null;
			try {
				template_file = new File(filename);
			}
			catch (java.lang.NullPointerException ex) {
				error("Can't find file "+s);
				return;
			}
			
			if(! template_file.exists() || ! template_file.canRead()) {
				error("Can't find file " + s +" or it can't be read");
				return;
			}
			
			FileInputStream fs = null;
			try {
				fs = new FileInputStream(template_file);
			}
			catch (java.io.FileNotFoundException ex) {
				error("Can't find file "+s);
				return;
			}
			
			byte[] file_data = new byte[(int)template_file.length()];
			try {
				fs.read(file_data);
			}
			catch (java.io.IOException ex) {
				error(ex.toString());
				return;
			}
			
			try {
				current_server.loadTemplate(file_data);
			}
			catch(ProtocolException e) {
				error(e.toString()); 
			}
		}
	}
	
	public void getMPUnitType() {
		if(current_server != null) {
			try {
				short v = current_server.getMPUnitType();
				outlet(0, new Atom[] {
							Atom.newAtom("MPUnitType"), 
							Atom.newAtom(v)
						});
			}
			catch(ProtocolException e) {
				// nothing;
			}
		}
	}
	
	private boolean isValidChannelType(String s) {
		return s.equals(ACQServer.kAnalogChannelType) || 
			s.equals(ACQServer.kDigitalChannelType) || 
			s.equals(ACQServer.kCalcChannelType);
	}

	public void getEnabledChannels(String s) {
		if(!isValidChannelType(s)) {
			error(s+" is not a valid channel type");
			return;
		}
		
		if(current_server != null) {
			try {
				Vector v = current_server.getEnabledChannels(s);
				Atom[] a = new Atom[v.size()+2];
				a[0] = Atom.newAtom("EnabledChannels");
				a[1] = Atom.newAtom(s);
				for(int i=0; i < v.size(); i++) {
					a[i+2] = Atom.newAtom(((Integer)v.get(i)).intValue());
				}
				
				outlet(0, a);
			}
			catch(ProtocolException e) {
				// nothing;
			}
		}
	}
	
	public void getSamplingRate() {
		if(current_server != null) {
			try {
				double v = current_server.getSamplingRate();
				outlet(0, new Atom[] {
						Atom.newAtom("SamplingRate"), 
						Atom.newAtom(v)
					});
			}
			catch(ProtocolException e) {
				error(e.toString());
			}
		}
	}
	
	public void getSamplingRateDivider(String s, int idx) {
		if(!isValidChannelType(s)) {
			error(s+" is not a valid channel type");
			return;
		}
		
		if(current_server != null) {
			try {
				int v = current_server.getSamplingRateDivider(s, (short)idx);
				outlet(0, new Atom[] {
						Atom.newAtom("SamplingRateDivider"), 
						Atom.newAtom(v)
					});
			}
			catch(ProtocolException e) {
				error(e.toString());
			}
		}
	}
	
	
	
	public void getAcquiring() {
		if(current_server != null) {
			try {
				boolean v = current_server.isAcquisitionInProgress();
				outlet(0, new Atom[] {
						Atom.newAtom("Acquiring"), 
						Atom.newAtom(v ? 1 : 0)
					});
			}
			catch(ProtocolException e) {
				error(e.toString());
			}
		}
	}
	
	public void Acquiring(int enable) {
		if(current_server != null) {
			try {
				boolean e = enable != 0;
				boolean v = current_server.isAcquisitionInProgress();
				if(e != v) {
					current_server.toggleAcquisition();
				}
			}
			catch(ProtocolException e) {
				error(e.toString());
			}
		}
	}
	
	public void nsamples(int n) {
		nsamples = n < 1 ? 1 : n;
	//	connection.matrix.setDim(new int[] {nsamples, 1});
		Iterator<DataConnection> dcs = connections.values().iterator();
		while(dcs.hasNext()) {
			DataConnection connection = dcs.next();
			connection.matrix.setDim(new int[] {nsamples, 1});
		}
	}
	
	public void bang() {
		/*
		if(current_server != null && connection.data_socket != null) {
			double vec[] = new double[nsamples];
			//post("nsmap "+connection.data_socket.numSamples() + " " + nsamples);
			while(connection.data_socket.numSamples() >= nsamples) {
				for(int i=0; i < nsamples; i++) {
					vec[i] = connection.data_socket.getSample(i);
				}
				connection.matrix.copyArrayToMatrix(vec);
				connection.data_socket.removeSamplesFromBuffer(nsamples);
				outlet(0, new Atom[] {
						Atom.newAtom("jit_matrix"), 
						Atom.newAtom(connection.matrix.getAttrString("name"))
					});
			}
		}
		*/
		
		Iterator<DataConnection> dcs = connections.values().iterator();
		if(current_server != null) {
			double vec[] = new double[nsamples];
			
			while(dcs.hasNext()) {
				DataConnection connection = dcs.next();
				if(connection.data_socket != null) {
				//	post("nsmap "+connection.data_socket.numSamples() + " " + nsamples);
					while(connection.data_socket.numSamples() >= nsamples) {
						for(int i=0; i < nsamples; i++) {
							vec[i] = connection.data_socket.getSample(i);
						}
						connection.matrix.copyArrayToMatrix(vec);
						connection.data_socket.removeSamplesFromBuffer(nsamples);
						outlet(0, new Atom[] {
								Atom.newAtom(connection.channel_type),
								Atom.newAtom(connection.channel),
								Atom.newAtom("jit_matrix"),
								Atom.newAtom(connection.matrix.getAttrString("name"))
							});
					}
				}
			}
		}
	}
	
	public void stream(String s, int idx) {
		if(!isValidChannelType(s)) {
			error(s+" is not a valid channel type");
			return;
		}

		/*
		if(current_server != null) {
			if(connection.data_socket == null) {
				connection.data_socket = new ChannelDataSocket(PORT);
			}
			
			if(connection.data_socket.isProcessing()) {
				post("stop processing");
				connection.data_socket.stopProcessing();
			}
			
			try {
				if(current_server.isChannelEnabled(s, (short)idx)) {
					post("Enabled " + s + " " + idx);
					current_server.changeMostRecentSampleEnabled(s, (short)idx, true);
					
					current_server.disableAllDataDelivery();
					current_server.changeDataConnectionMethod(ACQServer.kMultipleConnectionDelivery);
					current_server.changeTransportType(ACQServer.kTCPTransportType);
					current_server.changeDataDeliveryEnabled(s, (short)idx, true);
					current_server.changeConnectionPort(s, (short)idx, (short)PORT);
					current_server.changeBinaryEndian(s, (short)idx, ACQServer.kBigEndian);
					current_server.changeBinaryType(s, (short)idx, ACQServer.kDoubleDataType);
				
					// start the stream
					connection.data_socket.startProcessing();
				}
				else {
					error("Channel "+s+" "+idx+" is not available");
					return;
				}
			}
			catch(ProtocolException e) {
				error(e.toString());
			}
	        
	        System.out.println("Acquiring data into template...");
	        try {
	        	post("current_server.isAcquisitionInProgress(): " + current_server.isAcquisitionInProgress());
        		current_server.toggleAcquisition();
        		if(first_stream) {
        			current_server.toggleAcquisition();
        			current_server.toggleAcquisition();
        			first_stream = false;
        		}
        		else {
        			if(! current_server.isAcquisitionInProgress()) {
        				current_server.toggleAcquisition();
        			}
        		}
	        	post("2 current_server.isAcquisitionInProgress(): " + current_server.isAcquisitionInProgress());
	        }
	        catch(ProtocolException e) {
	        	error(e.toString());
	        }
		}
		
		connection.channel_type = s;
		connection.channel = idx;
		*/
		
		String name = s+idx;
		DataConnection connection = connections.get(name);
		if(connection == null) {
			connection = new DataConnection();
		}
		else {
			connections.remove(name);
		}
		
		if( stream_to_connection(connection, s, idx) ) {
			connections.put(name, connection);
		}
	}

	private boolean stream_to_connection(DataConnection connection, String s, int idx) {
		if(current_server != null) {
			if(connection.data_socket == null) {
				connection.data_socket = new ChannelDataSocket(PORT);
				
				// avoid conflicting port numbers
				PORT++;
				if(PORT > 18000) {
					PORT = 16214;
				}
			}
			
			if(connection.data_socket.isProcessing()) {
			//	post("stop processing");
				connection.data_socket.stopProcessing();
			}
			
			connection.channel_type = s;
			connection.channel = idx;
			
			try {
				if(current_server.isChannelEnabled(s, (short)idx)) {
				//	post("Enabled " + s + " " + idx);
					current_server.disableAllDataDelivery();
					current_server.changeDataConnectionMethod(ACQServer.kMultipleConnectionDelivery);
					current_server.changeTransportType(ACQServer.kTCPTransportType);
					
					Iterator<DataConnection> dcs = connections.values().iterator();
					while(dcs.hasNext()) {
						DataConnection dc = dcs.next();
						if(! start_connection_stream(dc)){
							return false;
						}
					}
					start_connection_stream(connection);
				
					// start the stream
					connection.data_socket.startProcessing();
				}
				else {
					error("Channel "+s+" "+idx+" is not available");
					return false;
				}
			}
			catch(ProtocolException e) {
				error(e.toString());
				return false;
			}
	        
	        System.out.println("Acquiring data into template...");
	        try {
	        //	post("current_server.isAcquisitionInProgress(): " + current_server.isAcquisitionInProgress());
        		current_server.toggleAcquisition();
        		if(first_stream) {
        			current_server.toggleAcquisition();
        			current_server.toggleAcquisition();
        			first_stream = false;
        		}
        		else {
        			if(! current_server.isAcquisitionInProgress()) {
        				current_server.toggleAcquisition();
        			}
        		}
	        //	post("2 current_server.isAcquisitionInProgress(): " + current_server.isAcquisitionInProgress());
	        }
	        catch(ProtocolException e) {
	        	error(e.toString());
	        	return false;
	        }
		}
		
		post("SUCCESFUL CONNECTION");
		return true;
	}
	
	private boolean start_connection_stream(DataConnection connection) {
		try {
			current_server.changeMostRecentSampleEnabled(connection.channel_type, (short)connection.channel, true);
			current_server.changeDataDeliveryEnabled(connection.channel_type, (short)connection.channel, true);
			current_server.changeConnectionPort(connection.channel_type, (short)connection.channel, (short)connection.data_socket.getPort());
			current_server.changeBinaryEndian(connection.channel_type, (short)connection.channel, ACQServer.kBigEndian);
			current_server.changeBinaryType(connection.channel_type, (short)connection.channel, ACQServer.kDoubleDataType);
		}
		catch(ProtocolException e) {
			error(e.toString());
			return false;
		}
		return true;
	}
	
	private Vector locate_servers() {
		ServerDiscoveryHelper server_finder = new ServerDiscoveryHelper();
        try {
        	server_finder.locateServers();
        }
        catch (Exception e) {
           error("Exception thrown when locating servers: "+e);
        }
        
        return server_finder.getServerList();
	}
	
	private void guess_server(Vector servers) {
		ACQServer server = null;
		
		Enumeration e = servers.elements();
        while(e.hasMoreElements()) {
            ACQServerAddress addr = (ACQServerAddress)e.nextElement();
            
			// default to using our local machine first
            try {
				if(addr.getAddress().equals(InetAddress.getLocalHost())) {
					server = new ACQServer(addr);
					break;
				}
			}
			catch (java.net.UnknownHostException ex) {
				// nothing
			}
        }
        
        if(server == null) {
        	e = servers.elements();
            while(e.hasMoreElements()) {
                ACQServerAddress addr = (ACQServerAddress)e.nextElement();
    			server = new ACQServer(addr);
    			break;
            }
        }
        
        current_server = server;
	}
}