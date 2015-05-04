package ro.pub.cs.systems.pdsd.lab09.chatservice.view;

import java.util.ArrayList;

import ro.pub.cs.systems.pdsd.lab09.chatservice.R;
import ro.pub.cs.systems.pdsd.lab09.chatservice.general.Constants;
import ro.pub.cs.systems.pdsd.lab09.chatservice.model.NetworkService;
import ro.pub.cs.systems.pdsd.lab09.chatservice.networkservicediscoveryoperations.NetworkServiceDiscoveryOperations;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class ChatActivity extends Activity {
	
	private NetworkServiceDiscoveryOperations networkServiceDiscoveryOperations = null;
	
	private boolean serviceRegistrationStatus = false;
	private boolean serviceDiscoveryStatus    = false;
	
	private ArrayList<NetworkService> discoveredServices    = null;
	private ArrayList<NetworkService> conversations         = null;
	
	private Handler handler = null;
	
	private MulticastLock multicastLock = null;
	private WifiManager wifiManager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(Constants.TAG, "onCreate() callback method was invoked!");
		setContentView(R.layout.activity_chat);
		
		wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		multicastLock = wifiManager.createMulticastLock(Constants.TAG);
		multicastLock.setReferenceCounted(true);
		multicastLock.acquire();
		
		discoveredServices = new ArrayList<NetworkService>();
		conversations      = new ArrayList<NetworkService>();
		
		setHandler(new Handler());
		
		setNetworkServiceDiscoveryOperations(new NetworkServiceDiscoveryOperations(this));
		
		setChatNetworkServiceFragment(new ChatNetworkServiceFragment());
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        Log.v(Constants.TAG, "onResume() callback method was invoked!");
        if (networkServiceDiscoveryOperations != null) {
        	if (!serviceDiscoveryStatus) {
	        	serviceDiscoveryStatus = true;
	        	networkServiceDiscoveryOperations.startNetworkServiceDiscovery();
				if (getChatNetworkServiceFragment() != null) {
					getChatNetworkServiceFragment().startServiceDiscovery();
				}
        	}
        }
    }
	
	@Override
	protected void onPause() {
		Log.v(Constants.TAG, "onPause() callback method was invoked!");
		if (networkServiceDiscoveryOperations != null) {	
			if (serviceDiscoveryStatus) {
				serviceDiscoveryStatus = false;
				networkServiceDiscoveryOperations.stopNetworkServiceDiscovery();
				if (getChatNetworkServiceFragment() != null) {
					getChatNetworkServiceFragment().stopServiceDiscovery();
				}
			}
		}
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		Log.v(Constants.TAG, "onDestroy() callback method was invoked!");
		if (networkServiceDiscoveryOperations != null) {
			if (serviceDiscoveryStatus) {
				serviceDiscoveryStatus = false;
				networkServiceDiscoveryOperations.stopNetworkServiceDiscovery();
				if (getChatNetworkServiceFragment() != null) {
					getChatNetworkServiceFragment().stopServiceDiscovery();
				}
			}
			if (serviceRegistrationStatus) {
				serviceRegistrationStatus = false;
				networkServiceDiscoveryOperations.unregisterNetworkService();
				if (getChatNetworkServiceFragment() != null) {
					getChatNetworkServiceFragment().stopServiceRegistration();
				}
			}
			networkServiceDiscoveryOperations.closeJmDns();
		}
		
		if (multicastLock != null) {
			multicastLock.release();
		}
		super.onDestroy();
	} 	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void setHandler(Handler handler) {
		this.handler = handler;
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	public void setNetworkServiceDiscoveryOperations(NetworkServiceDiscoveryOperations networkServiceDiscoveryOperations) {
		this.networkServiceDiscoveryOperations = networkServiceDiscoveryOperations;
	}
	
	public NetworkServiceDiscoveryOperations getNetworkServiceDiscoveryOperations() {
		return networkServiceDiscoveryOperations;
	}
	
	public void setChatNetworkServiceFragment(ChatNetworkServiceFragment chatNetworkServiceFragment) {
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(R.id.container_frame_layout, chatNetworkServiceFragment, Constants.FRAGMENT_TAG);
		fragmentTransaction.commit();
	}
	
	public ChatNetworkServiceFragment getChatNetworkServiceFragment() {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_TAG);
		if (fragment instanceof ChatNetworkServiceFragment) {
			return (ChatNetworkServiceFragment)fragment;
		}
		return null;
	}
	
	public void setServiceRegistrationStatus(boolean serviceRegistrationStatus) {
		this.serviceRegistrationStatus = serviceRegistrationStatus;
	}
	
	public boolean getServiceRegistrationStatus() {
		return serviceRegistrationStatus;
	}
	
	public void setServiceDiscoveryStatus(boolean serviceDiscoveryStatus) {
		this.serviceDiscoveryStatus = serviceDiscoveryStatus;
	}
	
	public boolean getServiceDiscoveryStatus() {
		return serviceDiscoveryStatus;
	}
	
	public void setWifiManager(WifiManager wifiManager) {
		this.wifiManager = wifiManager;
	}
	
	public WifiManager getWifiManager() {
		return wifiManager;
	}

	public void setDiscoveredServices(ArrayList<NetworkService> discoveredServices) {
		this.discoveredServices = discoveredServices;
		handler.post(new Runnable() {
			@Override
			public void run() {
				ChatNetworkServiceFragment chatNetworkServiceFragment = getChatNetworkServiceFragment();
				if (chatNetworkServiceFragment != null && chatNetworkServiceFragment.isVisible()) {
					chatNetworkServiceFragment.getDiscoveredServicesAdapter().notifyDataSetChanged();
				}
			}
		});
	}
	
	public ArrayList<NetworkService> getDiscoveredServices() {
		return discoveredServices;
	}
	
	public void setConversations(final ArrayList<NetworkService> conversations) {
		this.conversations = conversations;
		handler.post(new Runnable() {
			@Override
			public void run() {
				ChatNetworkServiceFragment chatNetworkServiceFragment = getChatNetworkServiceFragment();
				if (chatNetworkServiceFragment != null && chatNetworkServiceFragment.isVisible()) {
					chatNetworkServiceFragment.getConversationsAdapter().setData(conversations);
					chatNetworkServiceFragment.getConversationsAdapter().notifyDataSetChanged();
				}
			}
		});
	}
	
	public ArrayList<NetworkService> getConversations() {
		return conversations;
	}	
}
