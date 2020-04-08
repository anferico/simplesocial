package simplesocial;

public class Constants
{
	// Symbolic name of the server
	public static final String SERVER_NAME = "localhost";
	
	// Port where the server listens for 'registration' requests
	public static final int SERVER_REG_PORT = 55555;
	
	// Port where the server listens for 'login' requests
	public static final int SERVER_LOGIN_PORT = 55556;
	
	// Port where the server listens for 'send friendship' requests
	public static final int SERVER_FSREQUEST_PORT = 55557;
	
	// Port where the server listens for 'accept/deny friendship' requests 
	public static final int SERVER_FSGRANT_PORT = 55558;
	
	// Port where the server listens for 'friends list' requests
	public static final int SERVER_FRIENDS_PORT = 55559;
	
	// Port where the server listens for 'search for a user' requests 
	public static final int SERVER_USERS_PORT = 55560;
	
	// Port where the server listens for 'post something' requests 
	public static final int SERVER_CONTENT_PORT = 55561;
	
	// Port where the server listens for 'logout' requests
	public static final int SERVER_LOGOUT_PORT = 55562;
	
	// Port where the server exposes the RMI registry 
	public static final int SERVER_RMI_REGISTRY_PORT = 21774;
	
	// Multicast address of the keep-alive group
	public static final String SERVER_MULTICAST_GROUP_ADDRESS = "233.255.255.255";
	
	// Port to which the server's MulticastSocket is bound
	public static final int SERVER_MULTICAST_GROUP_PORT = 21770;
	
	// Port where the server receives responses to keep-alive messages
	public static final int SERVER_KEEPALIVE_RESPONSES_PORT = 21771;
	
	// Port where the client listens for new friendship requests
	public static final int CLIENT_FSREQUESTS_PORT = 40020;
		
    // Port where the client accepts connections from the server when the latter 
    // wants to probe its state (online or offline)
	public static final int CLIENT_PROBING_PORT = 40021;
}
