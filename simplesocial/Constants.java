package simplesocial;

public class Constants
{
	// Il nome simbolico del server
	public static final String SERVER_NAME = "localhost";
	
	// La porta in cui il server ascolta le richieste di 'registrazione'
	public static final int SERVER_REG_PORT = 55555;
	
	// La porta in cui il server ascolta le richieste di 'login'
	public static final int SERVER_LOGIN_PORT = 55556;
	
	// La porta in cui il server ascolta le richieste di 'invio di una richiesta di amicizia' 
	public static final int SERVER_FSREQUEST_PORT = 55557;
	
	// La porta in cui il server ascolta le richieste di 'risposta a una richiesta di amicizia' 
	public static final int SERVER_FSGRANT_PORT = 55558;
	
	// La porta in cui il server ascolta le richieste di 'ottieni lista amici' 
	public static final int SERVER_FRIENDS_PORT = 55559;
	
	// La porta in cui il server ascolta le richieste di 'ottieni lista utenti' 
	public static final int SERVER_USERS_PORT = 55560;
	
	// La porta in cui il server ascolta le richieste di 'pubblicazione nuovo contenuto' 
	public static final int SERVER_CONTENT_PORT = 55561;
	
	// La porta in cui il server ascolta le richieste di 'logout' 
	public static final int SERVER_LOGOUT_PORT = 55562;
	
	// La porta in cui il server rende disponibile il registry RMI 
	public static final int SERVER_RMI_REGISTRY_PORT = 21774;
	
	// L'indirizzo multicast del gruppo dei destinatari dei keep-alive
	public static final String SERVER_MULTICAST_GROUP_ADDRESS = "233.255.255.255";
	
	// La porta alla quale è legata la MulticastSocket del server
	public static final int SERVER_MULTICAST_GROUP_PORT = 21770;
	
	// La porta in cui il server riceve le risposte ai messaggi di keep-alive
	public static final int SERVER_KEEPALIVE_RESPONSES_PORT = 21771;
	
	// La porta in cui il client accoglie le richieste di amicizia speditegli
	public static final int CLIENT_FSREQUESTS_PORT = 40020;
		
	// La porta in cui il client attende l'apertura di connessioni da parte del server
	// quando quest'ultimo vuole verificare se il client è online o meno
	public static final int CLIENT_PROBING_PORT = 40021;
}
