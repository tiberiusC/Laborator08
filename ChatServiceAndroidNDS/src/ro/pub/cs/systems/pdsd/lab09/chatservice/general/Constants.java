package ro.pub.cs.systems.pdsd.lab09.chatservice.general;

public interface Constants {
	
	final public static boolean DEBUG                    = true;
	
	final public static String  TAG                      = "Chat Service1";
	
	final public static String  SERVICE_NAME             = "Chat1";
	final public static String  SERVICE_NAME_SEARCH_KEY  = "Chat";
	final public static String  SERVICE_TYPE             = "_http._tcp.";
	
	final public static String  FRAGMENT_TAG             = "ContainerFrameLayout";
	
	final public static int     MESSAGE_QUEUE_CAPACITY   = 50;
	
	final public static int     CONVERSATION_TO_SERVER   = 1;
	final public static int     CONVERSATION_FROM_CLIENT = 2;
	
	final public static int     MESSAGE_TYPE_SENT        = 1;
	final public static int     MESSAGE_TYPE_RECEIVED    = 2;

}
