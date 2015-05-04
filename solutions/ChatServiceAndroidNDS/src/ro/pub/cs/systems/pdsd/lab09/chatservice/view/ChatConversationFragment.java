package ro.pub.cs.systems.pdsd.lab09.chatservice.view;

import java.util.List;

import ro.pub.cs.systems.pdsd.lab09.chatservice.R;
import ro.pub.cs.systems.pdsd.lab09.chatservice.general.Constants;
import ro.pub.cs.systems.pdsd.lab09.chatservice.model.Message;
import ro.pub.cs.systems.pdsd.lab09.chatservice.networkservicediscoveryoperations.ChatClient;
import ro.pub.cs.systems.pdsd.lab09.chatservice.networkservicediscoveryoperations.NetworkServiceDiscoveryOperations;
import android.app.Fragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

public class ChatConversationFragment extends Fragment {
	
	private LinearLayout chatCommunicationHistoryLinearLayout = null;
	private EditText     messageEditText                      = null;
	private Button       sendMessageButton                    = null;
	
	private ChatClient chatClient = null;
	
	private int clientPosition = -1;
	private int clientType     = -1;
	
	private SendMessageButtonClickListener sendMessageButtonClickListener = new SendMessageButtonClickListener();
	private class SendMessageButtonClickListener implements Button.OnClickListener {
		
		@Override
		public void onClick(View view) {
			String message = messageEditText.getText().toString();
			if (message.isEmpty()) {
				Toast.makeText(getActivity(), "You should fill a message!", Toast.LENGTH_SHORT).show();
			} else {
				messageEditText.setText("");
				chatClient.sendMessage(message);
			}
		}
	}
	
	public ChatConversationFragment(int clientPosition, int clientType) {
		this.clientPosition = clientPosition;
		this.clientType     = clientType;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
		return inflater.inflate(R.layout.fragment_chat_conversation, parent, false);
	}
	
	public synchronized void appendMessage(final Message message) {
		chatCommunicationHistoryLinearLayout.post(new Runnable() {
			@Override
			public void run() {
				TextView messageTextView = new TextView(getActivity());
				messageTextView.setText(message.getContent());
				LinearLayout.LayoutParams messageTextViewLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				switch (message.getType()) {
					case Constants.MESSAGE_TYPE_SENT:
						messageTextView.setBackgroundResource(R.drawable.frame_border_sent_message);
						messageTextView.setGravity(Gravity.LEFT);
						messageTextViewLayoutParams.gravity = Gravity.LEFT;
						break;
					case Constants.MESSAGE_TYPE_RECEIVED:
						messageTextView.setBackgroundResource(R.drawable.frame_border_received_message);
						messageTextView.setGravity(Gravity.RIGHT);
						messageTextViewLayoutParams.gravity = Gravity.RIGHT;
						break;
				}
				
				chatCommunicationHistoryLinearLayout.addView(messageTextView, messageTextViewLayoutParams);
				
				Space space = new Space(getActivity());
				LinearLayout.LayoutParams spaceLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 10);
				chatCommunicationHistoryLinearLayout.addView(space, spaceLayoutParams);
			}
		});
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ChatActivity chatServiceActivity = (ChatActivity)getActivity();
		NetworkServiceDiscoveryOperations networkServiceDiscoveryOperations = chatServiceActivity.getNetworkServiceDiscoveryOperations();
		
		switch(clientType) {
			case Constants.CONVERSATION_TO_SERVER:
				chatClient = networkServiceDiscoveryOperations.getCommunicationToServers().get(clientPosition);
				break;
			case Constants.CONVERSATION_FROM_CLIENT:
				chatClient = networkServiceDiscoveryOperations.getCommunicationFromClients().get(clientPosition);
				break;
		}
		
		chatCommunicationHistoryLinearLayout = (LinearLayout)getActivity().findViewById(R.id.chat_communication_history_linear_layout);
		messageEditText = (EditText)getActivity().findViewById(R.id.message_edit_text);
		
		sendMessageButton = (Button)getActivity().findViewById(R.id.send_message_button);
		sendMessageButton.setOnClickListener(sendMessageButtonClickListener);
		
		if (chatClient != null) {
			chatClient.setContext(chatServiceActivity);
			List<Message> conversationHistory = chatClient.getConversationHistory();
			for(Message conversation: conversationHistory) {
				appendMessage(conversation);
			}
		}
	}
	
	public int getClientPosition() {
		return clientPosition;
	}
	
	public int getClientType() {
		return clientType;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}	
	
}
