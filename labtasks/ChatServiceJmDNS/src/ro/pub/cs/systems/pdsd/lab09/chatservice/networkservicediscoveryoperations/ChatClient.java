package ro.pub.cs.systems.pdsd.lab09.chatservice.networkservicediscoveryoperations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ro.pub.cs.systems.pdsd.lab09.chatservice.general.Constants;
import ro.pub.cs.systems.pdsd.lab09.chatservice.general.Utilities;
import ro.pub.cs.systems.pdsd.lab09.chatservice.model.Message;
import ro.pub.cs.systems.pdsd.lab09.chatservice.view.ChatActivity;
import ro.pub.cs.systems.pdsd.lab09.chatservice.view.ChatConversationFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.util.Log;

public class ChatClient {
	
	private Socket socket = null;
	
	private Context context = null;
	
	private SendThread    sendThread    = null;
	private ReceiveThread receiveThread = null;
	
	private BlockingQueue<String> messageQueue = new ArrayBlockingQueue<String>(Constants.MESSAGE_QUEUE_CAPACITY);
	
	private List<Message> conversationHistory = new ArrayList<Message>();
	
	public ChatClient(Context context, final String host, final int port) {		
		this.context = context;
		
		try {
			socket = new Socket(host, port);
			Log.d(Constants.TAG, "A socket has been created on " + socket.getInetAddress() + ":" + socket.getLocalPort());
		} catch (IOException ioException) {
			Log.e(Constants.TAG, "An exception has occurred while creating the socket: " + ioException.getMessage());
			if (Constants.DEBUG) {
				ioException.printStackTrace();
			}
		}
		if (socket != null) {
			startThreads();
		}
	}
	
	public ChatClient(Context context, Socket socket) {
		this.context = context;
		
		this.socket = socket;
		if (socket != null) {
			startThreads();
		}
	}
	
	public void sendMessage(String message) {
		try {
			messageQueue.put(message);
		} catch (InterruptedException interruptedException) {
			Log.e(Constants.TAG, "An exception has occurred: " + interruptedException.getMessage());
			if (Constants.DEBUG) {
				interruptedException.printStackTrace();
			}
		}
	}
	
	private class SendThread extends Thread {
		
		@Override
		public void run() {
			
			PrintWriter printWriter = Utilities.getWriter(socket);
			if (printWriter != null) {
				try {
					Log.d(Constants.TAG, "Sending messages to "+socket.getInetAddress()+":"+socket.getLocalPort());
					
					// TODO: exercise 6
					// iterate while the thread is not yet interrupted
					// - get the content (a line) from the messageQueue, if available, using the take() method
					// - if the content is not null
					//   - send the content to the PrintWriter, as a line
					//   - create a Message instance, with the content received and Constants.MESSAGE_TYPE_SENT as message type
					//   - add the message to the conversationHistory
					//   - if the ChatConversationFragment is visible (query the FragmentManager for the Constants.FRAGMENT_TAG tag)
					//   append the message to the graphic user interface
					
					while(!Thread.currentThread().isInterrupted())
					{
						String line = messageQueue.take();
						if(line!=null)
						{
							printWriter.println(line);
							printWriter.flush();
							Log.d("Trimis","Mesajul trimis este :" + line);
							Message mesaj = new Message(line, Constants.MESSAGE_TYPE_SENT);
							conversationHistory.add(mesaj);
							if (context != null) {
								  ChatActivity chatActivity = (ChatActivity)context;
								  FragmentManager fragmentManager = chatActivity.getFragmentManager();
								  Fragment fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_TAG);
								  if (fragment instanceof ChatConversationFragment && fragment.isVisible()) {
								    ChatConversationFragment chatConversationFragment = (ChatConversationFragment)fragment;
								    chatConversationFragment.appendMessage(mesaj);
								  }
								}
						}
					}
					
				} catch (Exception exception) {
					Log.e(Constants.TAG, "An exception has occurred: " + exception.getMessage());
					if (Constants.DEBUG) {
						exception.printStackTrace();
					}
				}
			}
			
			Log.i(Constants.TAG, "Send Thread ended");
			
		}
		
		public void stopThread() {
			interrupt();
		}
		
	}
	
	private class ReceiveThread extends Thread {
		
		@Override
		public void run() {
			
			BufferedReader bufferedReader = Utilities.getReader(socket);
			if (bufferedReader != null) {
				try {
					Log.d(Constants.TAG, "Reading messages from "+socket.getInetAddress()+":"+socket.getLocalPort());
					
					// TODO: exercise 7
					// iterate while the thread is not yet interrupted
					// - receive the content (a line) from the bufferedReader, if available
					// - if the content is not null
					//   - create a Message instance, with the content received and Constants.MESSAGE_TYPE_RECEIVED as message type
					//   - add the message to the conversationHistory
					//   - if the ChatConversationFragment is visible (query the FragmentManager for the Constants.FRAGMENT_TAG tag)
					//   append the message to the graphic user interface
					while(!Thread.currentThread().isInterrupted())
					{
						String line = bufferedReader.readLine();
						Log.d("Primit", line);
						if(line!=null)
						{
							Message mesaj = new Message(line, Constants.MESSAGE_TYPE_RECEIVED);
							conversationHistory.add(mesaj);
							if (context != null) {
								  ChatActivity chatActivity = (ChatActivity)context;
								  FragmentManager fragmentManager = chatActivity.getFragmentManager();
								  Fragment fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_TAG);
								  if (fragment instanceof ChatConversationFragment && fragment.isVisible()) {
								    ChatConversationFragment chatConversationFragment = (ChatConversationFragment)fragment;
								    chatConversationFragment.appendMessage(mesaj);
								  }
								}
						}
					}
					
					
				} catch (Exception exception) {
					Log.e(Constants.TAG, "An exception has occurred: " + exception.getMessage());
					if (Constants.DEBUG) {
						exception.printStackTrace();
					}
				}
			}
			
			Log.i(Constants.TAG, "Receive Thread ended");
			
		}
		
		public void stopThread() {
			interrupt();
		}
		
	}
	
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public void setContext(Context context) {
		this.context = context;
	}
	
	public Context getContext() {
		return context;
	}
	
	public void setConversationHistory(List<Message> conversationHistory) {
		this.conversationHistory = conversationHistory;
	}
	
	public List<Message> getConversationHistory() {
		return conversationHistory;
	}
	
	public void startThreads() {
		sendThread = new SendThread();
		sendThread.start();
				
		receiveThread = new ReceiveThread();
		receiveThread.start();
	}
	
	public void stopThreads() {
		
		sendThread.stopThread();
		receiveThread.stopThread();
		
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ioException) {
			Log.e(Constants.TAG, "An exception has occurred while closing the socket: " + ioException.getMessage());
			if (Constants.DEBUG) {
				ioException.printStackTrace();
			}
		}
	}

}
