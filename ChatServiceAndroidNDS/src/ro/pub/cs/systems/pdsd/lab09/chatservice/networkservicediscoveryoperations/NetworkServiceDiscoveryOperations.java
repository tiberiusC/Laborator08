package ro.pub.cs.systems.pdsd.lab09.chatservice.networkservicediscoveryoperations;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import ro.pub.cs.systems.pdsd.lab09.chatservice.general.Constants;
import ro.pub.cs.systems.pdsd.lab09.chatservice.model.NetworkService;
import ro.pub.cs.systems.pdsd.lab09.chatservice.view.ChatActivity;
import ro.pub.cs.systems.pdsd.lab09.chatservice.view.ChatNetworkServiceFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

public class NetworkServiceDiscoveryOperations {
	
	private Context context = null;
	
	private List<ChatClient> communicationToServers   = null;
	private List<ChatClient> communicationFromClients = null;
	
	private NsdManager nsdManager = null;
	
	private ResolveListener resolveListener = null;
	
	private DiscoveryListener discoveryListener = null;
	
	private RegistrationListener registrationListener = null;
	
	private String serviceName = null;
	
	private ChatServer chatServer = null;
	
	public NetworkServiceDiscoveryOperations(final Context context) {
		
		this.context = context;
		this.communicationToServers   = new ArrayList<ChatClient>();
		this.communicationFromClients = new ArrayList<ChatClient>();
		
		nsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);
		
		resolveListener = new ResolveListener() {

	        @Override
	        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
	            Log.e(Constants.TAG, "Resolve failed: " + errorCode);
	        }

	        @Override
	        public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
	            Log.i(Constants.TAG, "Resolve succeeded: " + nsdServiceInfo);

	            if (nsdServiceInfo.getServiceName().equals(serviceName)) {
	                Log.i(Constants.TAG, "The service running on the same machine has been discovered.");
	                return;
	            }
	            
	            String host = nsdServiceInfo.getHost().toString();
	            if(host.startsWith("/")) {
	    			host = host.substring(1);
	    		}
	            int port = nsdServiceInfo.getPort();
	            
	            ArrayList<NetworkService> discoveredServices = ((ChatActivity)context).getChatNetworkServiceFragment().getDiscoveredServices();
	            NetworkService networkService = new NetworkService(nsdServiceInfo.getServiceName(), host, port, Constants.CONVERSATION_TO_SERVER);
	            if (!discoveredServices.contains(networkService)) {
	            	discoveredServices.add(networkService);
	            	communicationToServers.add(new ChatClient(null, host, port));
	            }
	            ((ChatActivity)context).getChatNetworkServiceFragment().setDiscoveredServices(discoveredServices);
	            
	            Log.i(Constants.TAG, "A service has been discovered on " + host + ":" + port);
	        }
	    };	    
		
		discoveryListener = new NsdManager.DiscoveryListener() {

	        @Override
	        public void onDiscoveryStarted(String serviceType) {
	            Log.i(Constants.TAG, "Service discovery started: " + serviceType);
	        }

	        @Override
	        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
	            Log.i(Constants.TAG, "Service found: " + nsdServiceInfo);
	            if (!nsdServiceInfo.getServiceType().equals(Constants.SERVICE_TYPE)) {
	                Log.i(Constants.TAG, "Unknown Service Type: " + nsdServiceInfo.getServiceType());
	            } else if (nsdServiceInfo.getServiceName().equals(serviceName)) {
	                Log.i(Constants.TAG, "The service running on the same machine has been discovered: " + serviceName);
	            } else if (nsdServiceInfo.getServiceName().contains(Constants.SERVICE_NAME_SEARCH_KEY)) {
	            	nsdManager.resolveService(nsdServiceInfo, resolveListener);
	            }
	        }

	        @Override
	        public void onServiceLost(final NsdServiceInfo nsdServiceInfo) {
	            Log.i(Constants.TAG, "Service lost: " + nsdServiceInfo);
	            
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
	    	            NetworkService networkService = new NetworkService(nsdServiceInfo.getServiceName(), (nsdServiceInfo.getHost() != null) ? nsdServiceInfo.getHost().toString() : null, nsdServiceInfo.getPort(), -1);
	    	            if (discoveredServices.contains(networkService)) {
	    	            	int index = discoveredServices.indexOf(networkService);
	    	            	discoveredServices.remove(index);
	    	            	communicationToServers.remove(index);
	    	            }
	    	            chatNetworkServiceFragment.setDiscoveredServices(discoveredServices);
	    	            
	    	            Log.d(Constants.TAG, "serviceName = " + serviceName + "nsdServiceInfo.getServiceName() = " + nsdServiceInfo.getServiceName());
	    	            
	    	            if (nsdServiceInfo.getServiceName() != null && nsdServiceInfo.getServiceName().equals(serviceName)) {
	    		            ArrayList<NetworkService> conversations = chatNetworkServiceFragment.getConversations();
	    		            conversations.clear();
	    		            chatNetworkServiceFragment.setConversations(conversations);
	    		            
	    		            for(ChatClient communicationFromClient: communicationFromClients) {
	    		            	communicationFromClient.stopThreads();
	    		            }
	    		            communicationFromClients.clear();
	    	            }
	            	}
	            });

	        }

	        @Override
	        public void onDiscoveryStopped(String serviceType) {
	            Log.i(Constants.TAG, "Service discovery stopped: " + serviceType);
	        }

	        @Override
	        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
	            Log.e(Constants.TAG, "Service discovery start failed: Error code:" + errorCode);
	            nsdManager.stopServiceDiscovery(this);
	        }

	        @Override
	        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
	            Log.e(Constants.TAG, "Service discovery stop failed: Error code:" + errorCode);
	            nsdManager.stopServiceDiscovery(this);
	        }
	    };
	    
		registrationListener = new RegistrationListener() {

	        @Override
	        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
	            serviceName = nsdServiceInfo.getServiceName();
	        }

	        @Override
	        public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
	            Log.e(Constants.TAG, "An exception occured while registering the service: "+errorCode);
	        }

	        @Override
	        public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
	        }

	        @Override
	        public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
	        	Log.e(Constants.TAG, "An exception occured while unregistering the service: "+errorCode);
	        }
	    };

	}
	
	public void registerNetworkService(int port) throws Exception {
		Log.v(Constants.TAG, "Register Network Service on Port " + port);
		chatServer = new ChatServer(this, port);
		ServerSocket serverSocket = chatServer.getServerSocket();
		if (serverSocket == null) {
			throw new Exception("Could not get server socket");
		}
		chatServer.start();
		
		NsdServiceInfo nsdServiceInfo  = new NsdServiceInfo();

		nsdServiceInfo.setServiceName(Constants.SERVICE_NAME);
		nsdServiceInfo.setServiceType(Constants.SERVICE_TYPE);
		nsdServiceInfo.setHost(serverSocket.getInetAddress());
		nsdServiceInfo.setPort(serverSocket.getLocalPort());
		
		nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
	}
	
	public void unregisterNetworkService() {
		Log.v(Constants.TAG, "Unregistrer Network Service");
		nsdManager.unregisterService(registrationListener);
		for (ChatClient communicationToServer: communicationToServers) {
			communicationToServer.stopThreads();
		}
		for (ChatClient communicationFromClient: communicationFromClients) {
			communicationFromClient.stopThreads();
		}
		chatServer.stopThread();
	}
	
	public void startNetworkServiceDiscovery() {
		Log.v(Constants.TAG, "Start Network Service Discovery");
		nsdManager.discoverServices(Constants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
	}
	
	public void stopNetworkServiceDiscovery() {
		Log.v(Constants.TAG, "Stop Network Service Discovery");
		nsdManager.stopServiceDiscovery(discoveryListener);
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

}
