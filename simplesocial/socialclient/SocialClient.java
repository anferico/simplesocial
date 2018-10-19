package simplesocial.socialclient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.*;

import simplesocial.*;
import simplesocial.socialserver.*;

public class SocialClient implements ISocialClient
{	
	private String myUsername;       // L'username che ho scelto
	private String token; 			 // Token spedito dal server in fase di login
	private Date tokenReceptionDate; // Data e tempo di ricezione del token 

	// La lista dei contenuti pubblicati dagli utenti che seguo
	private List<String> availableContents;
	// La lista delle richieste di amicizia ricevute ma non ancora confermate
	private List<String> pendingFriendshipRequests;
	// Il thread che accoglie i messaggi spediti al gruppo di multicast
	private Thread mcGroupMemberThread = null;
	// Il thread che attende l'apertura di connessioni da parte del server quando
	// quest'ultimo vuole verificare se è online o meno
	private Thread probeListenerThread = null;
	// Il thread che accoglie le richieste di amicizia destinate a questo utente
	private Thread requestsListenerThread = null;
	
	public SocialClient()
	{
		
	}
	
	@Override
	public void register(String username, String password)
			throws SocialException
	{
		// Creo la socket per la comunicazione con il server
		Socket sock = null;
		try
		{
			sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_REG_PORT);
		}
		catch (IOException e)
		{						
			throw new SocialException("Non è stato possibile connettersi al server.");
		}
		
		// Comincio la comunicazione con il server
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String reply = null;
		
		try
		{
			
			// Invio al server username e password
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			
			writer.write(username);
			writer.newLine();
			writer.write(password);
			writer.newLine();
						
			writer.flush();
			
			// Aspetto la risposta dal server
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));			
			reply = reader.readLine();			
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante la registrazione.");
		}
		finally
		{
			// Termino la comunicazione chiudendo la connessione col server
			try
			{
				if (sock != null) { sock.close(); }
			}
			catch (IOException e){ ; }
		}
		
		// Analizzo la risposta del server
		if (reply.equals("USERNAME_ALREADY_TAKEN"))
    	{
    		throw new SocialException("Spiacente, l'username scelto è già stato assegnato.");
    	}
		
	}

	@Override
	public void login(String username, String password)
			throws SocialException
	{
		// Creo la socket per la comunicazione con il server
		Socket sock = null;
		try
		{
			sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_LOGIN_PORT);
		}
		catch (IOException e)
		{
			throw new SocialException("Non è stato possibile connettersi al server.");
		}

		// Comincio la comunicazione con il server
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String reply = null;
		
		
		// La socket che verrà utilizzata per accogliere connessioni TCP dal
		// server che hanno lo scopo di determinare se l'utente è online
		ServerSocket probeSock = null;
		// La socket che verrà utilizzata per accogliere connessioni TCP dal
		// server che hanno lo scopo di trasmettere le richieste di amicizia
		// destinate a questo utente
		ServerSocket requestsSock = null;
		try
		{
			probeSock = new ServerSocket(0);
			requestsSock = new ServerSocket(0);
			
			// Invio al server: 
			// - username 
			// - password
			// - indirizzo IP al quale è legata 'probeSock'
			// - porta alla quale è legata 'probeSock'
			// - indirizzo IP al quale è legata 'requestsSock'
			// - porta alla quale è legata 'requestsSock'
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write(username);
			writer.newLine();
			writer.write(password);
			writer.newLine();
			writer.write(probeSock.getInetAddress().getHostAddress());
			writer.newLine();
			writer.write(String.valueOf(probeSock.getLocalPort()));
			writer.newLine();
			writer.write(requestsSock.getInetAddress().getHostAddress());
			writer.newLine();
			writer.write(String.valueOf(requestsSock.getLocalPort()));
			writer.newLine();
			
			writer.flush();
			
			// Aspetto la risposta dal server
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			reply = reader.readLine();
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante il login.");
		}
		finally
		{
			// Chiudo la connessione con il server
			try
			{
				if (sock != null) { sock.close(); }
			}
			catch (IOException e){ ; }
		}
		
		if (reply.equals("INVALID_CREDENTIALS"))
		{
			try
			{
				if (probeSock != null) { probeSock.close(); }
				if (requestsSock != null) { requestsSock.close(); }
			}
			catch (IOException e){ ; }
			
			throw new SocialException("Username e/o password non validi.");
		}
		else if (reply.equals("ALREADY_LOGGED_IN"))
		{
			try
			{
				if (probeSock != null) { probeSock.close(); }
				if (requestsSock != null) { requestsSock.close(); }
			}
			catch (IOException e){ ; }
			
			throw new SocialException("Il login per questo account è già stato effettuato.");
		}
		else
		{
			// Registro il mio username e memorizzo il token che accompagnerà ogni messaggio trasmesso al server
			myUsername = username;
			token = reply;
			tokenReceptionDate = new Date();
		}
				
				
		// Login effettuato correttamente
						
		
		try
		{
			// Cerco di recuperare eventuali richieste di amicizia e contenuti in sospeso
			ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(myUsername + ".pendingRequestsAndContents"));
			pendingFriendshipRequests = (List<String>) inStream.readObject();
			availableContents = (List<String>) inStream.readObject();

			inStream.close();
		}
		catch (Exception e)
		{
			// Non è stato possibile leggere richieste di amicizia e contenuti in sospeso, perché non
			// erano mai state salvate
			
			availableContents = Collections.synchronizedList(new ArrayList<String>());
			pendingFriendshipRequests = Collections.synchronizedList(new ArrayList<String>());
		}
		
		
		try
		{
    		// Registro la callback per le notifiche
    		Registry registry = LocateRegistry.getRegistry(Constants.SERVER_RMI_REGISTRY_PORT);
    		
    		IFollowingService followingService = (IFollowingService) registry.lookup("FOLLOWING_SERVICE");
    		
    		SocialFollower stub = new SocialFollower(availableContents);    		
    		followingService.registerCallback(username, stub);	
		}
		catch (RemoteException | NotBoundException e)
		{
			try
			{
				if (probeSock != null) { probeSock.close(); }
				if (requestsSock != null) { requestsSock.close(); }
			}
			catch (IOException f){ ; }
			
			throw new SocialException("Si è verificato un errore durante il completamento del login.");			
		}
		
		
		// Faccio partire il thread che risponderà ai messaggi di keep-alive
		MulticastGroupListener mcGroupMember = new MulticastGroupListener(username);
		if (mcGroupMemberThread != null)
		{
			mcGroupMemberThread.interrupt();
		}
		mcGroupMemberThread = new Thread(mcGroupMember);
		mcGroupMemberThread.start();		
		
		// Faccio partire il thread che risponde alle richieste del server: sei online?
		ProbeMessagesListener probeListener = new ProbeMessagesListener(probeSock);
		if (probeListenerThread != null)
		{
			probeListenerThread.interrupt();
		}
		probeListenerThread = new Thread(probeListener);
		probeListenerThread.start();
		
		// Faccio partire il thread che accoglie le richieste di amicizia spedite
		FriendshipRequestsListener requestsListener = new FriendshipRequestsListener(requestsSock, pendingFriendshipRequests);
		if (requestsListenerThread != null)
		{
			requestsListenerThread.interrupt();
		}
		requestsListenerThread = new Thread(requestsListener);
		requestsListenerThread.start();
		
	}

	@Override
	public void sendFriendshipRequest(String username) 
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		// Creo la socket per la comunicazione con il server
		Socket sock = null;
		try
		{
			sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_FSREQUEST_PORT);
		}
		catch (IOException e)
		{
			throw new SocialException("Non è stato possibile connettersi al server.");
		}

		// Comincio la comunicazione con il server
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String reply = null;
		
		try
		{
			// Invio al server l'username dell'utente che voglio aggiungere come amico
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write(username);
			writer.newLine();
			// Allego il token
			writer.write(token.toString());
			writer.newLine();	
			
			writer.flush();
			
			// Aspetto la risposta dal server
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			reply = reader.readLine();
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante l'invio della richiesta.");
		}
		finally
		{
			// Chiudo la connessione con il server
			try
			{
				if (sock != null) { sock.close(); }
			}
			catch (IOException e){ ; }
		}
		
		// Analizzo la risposta del server
		if (!reply.equals("FRIENDSHIP_REQUEST_FORWARDED"))
		{
			SocialException se = null;
			if (reply.equals("TOKEN_EXPIRED"))
			{
				se = new ExpiredTokenException();
			}
			else if (reply.equals("FRIENDSHIP_REQUEST_ALREADY_SENT"))
			{
				se = new SocialException("Hai già spedito una richiesta di amicizia a questo utente in passato.");
			}
			else if (reply.equals("USER_OFFLINE"))
			{
				se = new SocialException("L'utente risulta offline. Prova più tardi.");
			}
			else if (reply.equals("UNKNOWN_USER"))
			{
				se = new SocialException("L'utente specificato è inesistente.");
			}
			
			throw se;
		}
	}
	
	@Override
	public List<String> getPendingFriendshipRequests() 
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		return pendingFriendshipRequests;
	}

	@Override
	public void respondToFriendshipRequest(String username, boolean choice) 
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		// Creo la socket per la comunicazione con il server
		Socket sock = null;
		try
		{
			sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_FSGRANT_PORT);
		}
		catch (IOException e)
		{
			throw new SocialException("Non è stato possibile connettersi al server.");
		}

		// Comincio la comunicazione con il server
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String reply = null;
		
		try
		{
			// Invio al server l'username dell'utente che ha spedito la richiesta
			// e la scelta del destinatario
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write(username);
			writer.newLine();
			writer.write(String.valueOf(choice));
			writer.newLine();
			// Allego il token
			writer.write(token.toString());
			writer.newLine();				
			
			writer.flush();
			
			// Aspetto la risposta dal server
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			reply = reader.readLine();
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante la conferma dell'amicizia.");
		}
		finally
		{
			// Chiudo la connessione con il server
			try
			{
				if (sock != null) { sock.close(); }
			}
			catch (IOException e){ ; }
		}
		
		// Analizzo la risposta del server
		if (!reply.equals("FRIENDS_UPDATED"))
		{	
			SocialException se = null;
	    	if (reply.equals("TOKEN_EXPIRED"))
	    	{
	    		se = new ExpiredTokenException();
	    	}
	    	else if (reply.equals("MISSING_ORIGINAL_REQUEST"))
	    	{
	    		se = new SocialException("Si sta tentando di rispondere ad una richiesta di amicizia scaduta o inesistente.");
	    	}
	    	else if (reply.equals("ALREADY_FRIENDS"))
	    	{
	    		se = new SocialException("Hai già risposto a questa richiesta in passato.");
	    	}
			
			throw se;			
		}
		
		// Ho riposto correttamente alla richiesta d'amicizia, quindi la cancello dalla
		// lista di quelle in sospeso		
		pendingFriendshipRequests.removeIf(pf -> pf.equals(username));
		
	}

	@Override
	public List<Friend> getFriends()
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		// Creo la socket per la comunicazione con il server
		Socket sock = null;
		try
		{
			// java.net.ConnectException: Connection refused (alla seconda volta)
			sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_FRIENDS_PORT);
		}
		catch (IOException e)
		{
			throw new SocialException("Non è stato possibile connettersi al server.");
		}
		
		// Comincio la comunicazione con il server
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String reply = null;
		
		try
		{
			// Invio al server la richiesta
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write("GET_FRIENDS");
			writer.newLine();
			// Allego il token
			writer.write(token.toString());
			writer.newLine();		
			
			writer.flush();
			
			// Aspetto la risposta dal server
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			reply = reader.readLine();
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante il recupero degli amici.");
		}
		finally
		{
			// Chiudo la connessione con il server
			try
			{
				if (sock != null) { sock.close(); }
			}
			catch (IOException e){ ; }
		}
		
		// Analizzo la risposta del server
    	if (reply.equals("TOKEN_EXPIRED"))
    	{
    		throw new ExpiredTokenException();
    	}

    	
		// La risposta è positiva, quindi itero sulla lista di amici spedita dal server
    	List<Friend> friends = new ArrayList<Friend>();
    	if (reply.length() > 0)
    	{
    		for (String friend : reply.split("-"))
    		{
    			boolean isOnline = friend.startsWith("!");
    			if (isOnline)
    			{
    				friends.add(new Friend(friend.substring(1), true));
    			}
    			else
    			{
    				friends.add(new Friend(friend, false));
    			}
    		}
    	}
		
		return friends;
	}

	@Override
	public String[] findUsers(String searchKey)
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		// Creo la socket per la comunicazione con il server
		Socket sock = null;
		try
		{
			sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_USERS_PORT);
		}
		catch (IOException e)
		{
			throw new SocialException("Non è stato possibile connettersi al server.");
		}
		
		
		// Comincio la comunicazione con il server
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String reply = null;
		
		try
		{
			// Invio al server la chiave di ricerca
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write(searchKey);
			writer.newLine();
			// Allego il token
			writer.write(token.toString());
			writer.newLine();				
			
			writer.flush();
			
			// Aspetto la risposta dal server
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			reply = reader.readLine();
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante la ricerca.");
		}
		finally
		{
			// Chiudo la connessione con il server
			try
			{
				if (sock != null) { sock.close(); }
			}
			catch (IOException e){ ; }
		}
		
		// Analizzo la risposta del server
    	if (reply.equals("TOKEN_EXPIRED"))
    	{
    		throw new ExpiredTokenException();
    	}	

		// La risposta è positiva
    	
		String[] users = reply.split("-");
		
		return users;
	}

	@Override
	public void publishContent(String content)
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		// Creo la socket per la comunicazione con il server
		Socket sock = null;
		try
		{
			sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_CONTENT_PORT);
		}
		catch (IOException e)
		{
			throw new SocialException("Non è stato possibile connettersi al server.");
		}

		// Comincio la comunicazione con il server
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String reply = null;
		
		try
		{
			// Invio al server il contenuto che voglio pubblicare
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write(content);
			writer.newLine();
			// Allego il token
			writer.write(token.toString());
			writer.newLine();			
			
			writer.flush();
			
			// Aspetto la risposta dal server
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			reply = reader.readLine();
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante la pubblicazione del contenuto.");
		}
		finally
		{
			// Chiudo la connessione con il server
			try
			{
				if (sock != null) { sock.close(); }
			}
			catch (IOException e){ ; }
		}
		
		// Analizzo la risposta del server
		if (reply.equals("TOKEN_EXPIRED"))
		{
			throw new ExpiredTokenException();
		}
	}

	@Override
	public void followUser(String username)
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		try
		{
			Registry registry = LocateRegistry.getRegistry(Constants.SERVER_RMI_REGISTRY_PORT);
			
			IFollowingService followingService = (IFollowingService) registry.lookup("FOLLOWING_SERVICE");
			followingService.startFollowing(token, username);			
		}
		catch (RemoteException | NotBoundException e)
		{
			throw new SocialException("Si è verificato un errore durante la registrazione del proprio interesse per l'utente \"" + username + "\".");
		}
	}

	@Override
	public List<String> getContents()
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		// I contenuti ricevuti dal social server vengono memorizzati in locale.
		// Se un utente fa logout prima di aver letto i contenuti, questi vengono salvati su disco
		// Una volta visualizzato, un messaggio viene cancellato.
		
		List<String> result = new ArrayList<String>(availableContents.size());
		result.addAll(availableContents);
		
		availableContents.clear();
		
		return result;
	}

	@Override
	public void logout()
			throws SocialException
	{
		// Verifico la validità del token
		if (!isTokenValid())
		{
			throw new ExpiredTokenException();
		}
		
		// Creo la socket per la comunicazione con il server
		Socket sock = null;
		try
		{
			sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_LOGOUT_PORT);
		}
		catch (IOException e)
		{
			throw new SocialException("Non è stato possibile connettersi al server.");
		}

		// Comincio la comunicazione con il server
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String reply = null;
		
		try
		{
			// Invio al server il contenuto che voglio pubblicare
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write("LOGOUT");
			writer.newLine();
			// Allego il token
			writer.write(token);
			writer.newLine();			
			
			writer.flush();
			
			// Aspetto la risposta dal server
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			reply = reader.readLine();
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante il logout.");
		}
		finally
		{
			// Chiudo la connessione con il server
			try
			{
				if (sock != null) { sock.close(); }
			}
			catch (IOException e){ ; }
		}
		
		// Analizzo la risposta del server
		if (reply.equals("TOKEN_EXPIRED"))
		{
			throw new ExpiredTokenException();
		}
		
		
		// Interrompo i thread che avevo lanciato subito dopo il login
		mcGroupMemberThread.interrupt();
		probeListenerThread.interrupt();
		requestsListenerThread.interrupt();
		
		// Salvo su disco eventuali contenuti non letti e richieste di amicizia non riscontrate
		try
		{
			ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(myUsername + ".pendingRequestsAndContents", false));
			outStream.writeObject(pendingFriendshipRequests);
			outStream.writeObject(availableContents);
						
			outStream.close();
		}
		catch (IOException e)
		{
			throw new SocialException("Si è verificato un errore durante il completamento del logout");
		}
		
		// Resetto il mio username e invalido il mio token
		myUsername = null;
		token = null;
		tokenReceptionDate = null;
		
	}
	
	private boolean isTokenValid()
	{
		if (token == null)
		{
			return false;
		}
		
		long offset = new Date().getTime() - tokenReceptionDate.getTime();
		return (offset / (1000 * 60 * 60)) < 24;
	}

}
