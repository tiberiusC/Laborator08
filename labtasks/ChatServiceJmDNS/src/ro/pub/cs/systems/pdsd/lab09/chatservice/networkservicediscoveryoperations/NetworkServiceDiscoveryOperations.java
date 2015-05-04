package ro.pub.cs.systems.pdsd.lab09.chatservice.networkservicediscoveryoperations;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import ro.pub.cs.systems.pdsd.lab09.chatservice.general.Constants;
import ro.pub.cs.systems.pdsd.lab09.chatservice.model.NetworkService;
import ro.pub.cs.systems.pdsd.lab09.chatservice.view.ChatActivity;
import ro.pub.cs.systems.pdsd.lab09.chatservice.view.ChatNetworkServiceFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

public class NetworkServiceDiscoveryOperations {
	
	private Context context = null;
	
	private List<ChatClient> communicationToServers   = null;
	private List<ChatClient> communicationFromClients = null;
	
	private JmDNS jmDns = null;
	
	private ServiceListener serviceListener = null;
	
	private String serviceName = null;
	
	private ChatServer chatServer = null;
	
	public NetworkServiceDiscoveryOperations(final Context context) {
		
		this.context = context;
		
		this.communicationToServers   = new ArrayList<ChatClient>();
		this.communicationFromClients = new ArrayList<ChatClient>();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					WifiManager wifiManager = ((ChatActivity)context).getWifiManager();
					InetAddress address = InetAddress.getByAddress(
		                    ByteBuffer.allocate(4).putInt(Integer.reverseBytes(wifiManager.getConnectionInfo().getIpAddress())).array()
		                    );
					String name = address.getHostName();
					Log.i(Constants.TAG, "address = " + address + " name = " + name);
					jmDns = JmDNS.create(address, name);
				} catch (IOException ioException) {
					Log.e(Constants.TAG, "An exception has occurred: " + ioException.getMessage());
					if (Constants.DEBUG) {
						ioException.printStackTrace();
					}
				}				
			}
		}).start();
		
		serviceListener = new ServiceListener() {
			@Override
			public void serviceResolved(ServiceEvent serviceEvent) {
				Log.i(Constants.TAG, "Resolve succeeded: " + serviceEvent);

	            if (serviceEvent.getName().equals(serviceName)) {
	                Log.i(Constants.TAG, "The service running on the same machine has been discovered.");
	                return;
	            }
	            
	            ServiceInfo serviceInfo = serviceEvent.getInfo();
	            if (serviceInfo == null) {
	            	Log.e(Constants.TAG, "Service Info for Service is null!");
	            	return;
	            }
	            
	            String[] hosts = serviceInfo.getHostAddresses();
	            String host = null;
	            if (hosts.length == 0) {
	            	Log.e(Constants.TAG, "No host addresses returned for the service!");
	            	return;
	            }
	            host = hosts[0];
	            if(host.startsWith("/")) {
	    			host = host.substring(1);
	    		}
	            
	            int port = serviceInfo.getPort();
	            
	            ArrayList<NetworkService> discoveredServices = ((ChatActivity)context).getChatNetworkServiceFragment().getDiscoveredServices();
	            NetworkService networkService = new NetworkService(serviceEvent.getName(), host, port, Constants.CONVERSATION_TO_SERVER);
	            if (!discoveredServices.contains(networkService)) {
	            	discoveredServices.add(networkService);
	            	communicationToServers.add(new ChatClient(null, host, port));
	            }
	            ((ChatActivity)context).getChatNetworkServiceFragment().setDiscoveredServices(discoveredServices);
	            
	            Log.i(Constants.TAG, "A service has been discovered on " + host + ":" + port);				
			}
			
			@Override
			public void serviceAdded(ServiceEvent serviceEvent) {
	            Log.i(Constants.TAG, "Service found: " + serviceEvent);	            
	            
	            if (!serviceEvent.getType().equals(Constants.SERVICE_TYPE)) {
	                Log.i(Constants.TAG, "Unknown Service Type: " + serviceEvent.getType());
	            } else if (serviceEvent.getName().equals(serviceName)) {
	                Log.i(Constants.TAG, "The service running on the same machine has been discovered: " + serviceName);
	            } else if (serviceEvent.getName().contains(Constants.SERVICE_NAME_SEARCH_KEY)) {
	            	Log.i(Constants.TAG, "The service should be resolved now: " + serviceName);
	            	jmDns.requestServiceInfo(serviceEvent.getType(), serviceEvent.getName(), 1);
	            }				
			}
			
			@Override
			public void serviceRemoved(final ServiceEvent serviceEvent) {
	            Log.i(Constants.TAG, "Service lost: " + serviceEvent);
	            
	            ServiceInfo serviceInfo = serviceEvent.getInfo();
	            if (serviceInfo == null) {
	            	Log.e(Constants.TAG, "Service Info for Service is null!");
	            	return;
	            }
	            
	            String[] hosts = serviceInfo.getHostAddresses();
	            String host = null;
	            if (hosts.length != 0) {
	            	host = hosts[0];
		            if(host.startsWith("/")) {
		    			host = host.substring(1);
		    		}
	            }
	            
	            final String finalizedHost = host;
	            
	            final int finalizedPort = serviceInfo.getPort();
	            
	            final ChatActivity chatActivity = (ChatActivity)context;
	            Handler handler = chatActivity.getHandler();
	            handler.post(new Runnable() {
	            	@Override
	            	public void run() {
	            		ChatNetworkServiceFragment chatNetworkServiceFragment = chatActivity.getChatNetworkServiceFragment();
	    				if (chatNetworkServiceFragment == null) {
	    					FragmentManager fragmentManager = chatActivity.getFragmentManager();
	    					fragmentManager.popBackStackImmediate();
	    					chatNetworkServiceFragment = chatActivity.getChatNetworkServiceFragment();
	    				}
	    	            
	    	            ArrayList<NetworkService> discoveredServices = chatNetworkServiceFragment.getDiscoveredServices();
	    	            NetworkService networkService = new NetworkService(serviceEvent.getName(), finalizedHost, finalizedPort, -1);
	    	            if (discoveredServices.contains(networkService)) {
	    	            	int index = discoveredServices.indexOf(networkService);
	    	            	discoveredServices.remove(index);
	    	            	communicationToServers.remove(index);
	    	            }
	    	            chatNetworkServiceFragment.setDiscoveredServices(discoveredServices);
	            	}
	            });				
			}
		};
	}
	
	public void registerNetworkService(int port) throws Exception {
		Log.v(Constants.TAG, "Register Network Service on Port " + port);
		chatServer = new ChatServer(this, port);
		if (chatServer.getServerSocket() == null) {
			throw new Exception("Could not get server socket");
		}
		chatServer.start();
		ServiceInfo serviceInfo = ServiceInfo.create(
				Constants.SERVICE_TYPE,
				Constants.SERVICE_NAME,
				port,
				Constants.SERVICE_DESCRIPTION
				);
		
		if (jmDns != null && serviceInfo != null) {
			serviceName = serviceInfo.getName();
			jmDns.registerService(serviceInfo);
		}
	}
	
	public void unregisterNetworkService() {
		Log.v(Constants.TAG, "Unregistrer Network Service");
		if (jmDns != null) {
			jmDns.unregisterAllServices();
		}	
		
		for (ChatClient communicationToServer: communicationToServers) {
			communicationToServer.stopThreads();
		}
		for (ChatClient communicationFromClient: communicationFromClients) {
			communicationFromClient.stopThreads();
		}
		chatServer.stopThread();
		
		final ChatActivity chatActivity = (ChatActivity)context;
        Handler handler = chatActivity.getHandler();
        handler.post(new Runnable() {
        	@Override
        	public void run() {
        		try {
	        		ChatNetworkServiceFragment chatNetworkServiceFragment = chatActivity.getChatNetworkServiceFragment();
					if (chatNetworkServiceFragment == null) {
						FragmentManager fragmentManager = chatActivity.getFragmentManager();
						fragmentManager.popBackStackImmediate();
						chatNetworkServiceFragment = chatActivity.getChatNetworkServiceFragment();
					}
	
			        ArrayList<NetworkService> conversations = chatNetworkServiceFragment.getConversations();
			        conversations.clear();
			        chatNetworkServiceFragment.setConversations(conversations);
			        communicationFromClients.clear();
        		} catch (Exception exception) {
        			Log.e(Constants.TAG, "An exception has occurred: " + exception.getMessage());
        			if (Constants.DEBUG) {
        				exception.printStackTrace();
        			}
        		}
        	}
        });
		
	}
	
	public void startNetworkServiceDiscovery() {
		Log.v(Constants.TAG, "Start Network Service Discovery");
		if (jmDns != null && serviceListener != null) {
			jmDns.addServiceListener(Constants.SERVICE_TYPE, serviceListener);
		}
	}
	
	public void stopNetworkServiceDiscovery() {
		Log.v(Constants.TAG, "Stop Network Service Discovery");
		if (jmDns != null && serviceListener != null) {
			jmDns.removeServiceListener(Constants.SERVICE_TYPE, serviceListener);
		}
		ArrayList<NetworkService> discoveredServices = ((ChatActivity)context).getChatNetworkServiceFragment().getDiscoveredServices();
		discoveredServices.clear();
		((ChatActivity)context).getChatNetworkServiceFragment().setDiscoveredServices(discoveredServices);
		communicationToServers.clear();
	}
	
	public void setContext(Context context) {
		this.context = context;
	}
	
	public Context getContext() {
		return context;
	}
	
	public void setCommunicationToServers(List<ChatClient> communicationToServers) {
		this.communicationToServers = communicationToServers;
	}
	
	public List<ChatClient> getCommunicationToServers() {
		return communicationToServers;
	}
	
	public void setCommunicationFromClients(List<ChatClient> communicationFromClients) {
		this.communicationFromClients = communicationFromClients;
		ArrayList<NetworkService> conversations = new ArrayList<NetworkService>();
		for (ChatClient communicationFromClient: communicationFromClients) {
			NetworkService conversation = new NetworkService(
					null,
					communicationFromClient.getSocket().getInetAddress().toString(),
					communicationFromClient.getSocket().getLocalPort(),
					Constants.CONVERSATION_FROM_CLIENT
					);
			
			conversations.add(conversation);
		}
        ((ChatActivity)context).getChatNetworkServiceFragment().setConversations(conversations);
	}
	
	public List<ChatClient> getCommunicationFromClients() {
		return communicationFromClients;
	}
	
	public void closeJmDns() {
		try {
			if (jmDns != null) {
				jmDns.close();
				jmDns = null;
			}
		} catch (IOException ioException) {
			Log.e(Constants.TAG, "An exception has occurred: " + ioException.getMessage());
			if (Constants.DEBUG) {
				ioException.printStackTrace();
			}
		}
	}

}
