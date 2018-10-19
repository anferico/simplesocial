package simplesocial.socialserver;

import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleSocialManager implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private static SimpleSocialManager instance = null;
	private List<SocialUser> users;
	private List<Friendship> estabilishedFriendships;
	private List<Friendship> pendingFriendships;
	private List<PendingContent> pendingContents; 
	
	private SimpleSocialManager()
	{
		restore();
	}
	
	// Restituisce l'unica istanza di questa classe (realizza il pattern Singleton)
	public static synchronized SimpleSocialManager getManager()
	{
		if (instance == null)
		{
			SimpleSocialManager.instance = new SimpleSocialManager();
		}
		
		return instance;
	}
	
	// Esegue un backup della rete sociale
	public void backup()
	{				
		try
		{
			ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream("networkState", false));
			outStream.writeObject(this);
			
			outStream.close();
		}
		catch (Exception e)
		{
			System.out.println("Errore durante il backup. Dettagli: " + e.getMessage());
		}
	}
	
	// Ripristina l'ultimo stato della rete sociale
	private void restore()
	{
		try
		{
			ObjectInputStream inStream = new ObjectInputStream(new FileInputStream("networkState"));
			SimpleSocialManager lastSnapshot = (SimpleSocialManager) inStream.readObject();
			
			inStream.close();
			
			// Ripristino lo stato della rete
			users = lastSnapshot.users;
			estabilishedFriendships = lastSnapshot.estabilishedFriendships;
			pendingFriendships = lastSnapshot.pendingFriendships;
			pendingContents = lastSnapshot.pendingContents;
		}
		catch (Exception e)
		{
			// Lo stato della rete non era mai stato salvato
			
			users = Collections.synchronizedList(new ArrayList<SocialUser>());
			estabilishedFriendships = Collections.synchronizedList(new ArrayList<Friendship>());
			pendingFriendships = Collections.synchronizedList(new ArrayList<Friendship>());
			pendingContents = Collections.synchronizedList(new ArrayList<PendingContent>());
		}
	}
	
	// Aggiunge un nuovo utente alla lista degli utenti registrati
	public void registerUser(String username, String password)
	{
		if (!userExists(username))
		{
			users.add(new SocialUser(username, password));
		}
	}
	
	// Ottiene un riferimento all'oggetto di tipo SocialUser il cui username
	// coincide con quello passato come parametro
	public SocialUser userFromUsername(String username)
	{
		for (SocialUser u : users)
		{
			if (u.getUsername().equals(username))
			{
				return u;
			}
		}
		
		return null;
	}
	
	// Ottiene un riferimento all'oggetto di tipo SocialUser che rappresenta
	// l'utente il cui token coincide con quello passato come parametro
	public SocialUser userFromToken(String token)
	{
		for (SocialUser u : users)
		{
			String aToken = u.getSessionToken();
			if (aToken != null)
			{    
    			if (aToken.equals(token))
    			{
    				return u;
    			}
			}
		}
		
		return null;
	}
	
	// Restituisce true se esiste un utente con username uguale a quello passato come
	// parametro, false altrimenti
	public boolean userExists(String username)
	{
		return userFromUsername(username) != null;
	}
	
	// Restituisce true se l'utente con username uguale a quello passato come parametro
	// è online, false altrimenti
	public boolean isUserOnline(String username)
	{
		if (userExists(username))
		{
			return userFromUsername(username).isOnline();
		}
		else
		{
			return false;			
		}
	}
	
	// Restituisce true se l'utente con username uguale a quello passato come parametro
	// ha impostato una password che coincide con quella passata come parametro, false
	// altrimenti
	public boolean isValidPassword(String username, String password)
	{
		SocialUser u = userFromUsername(username);
		if (u != null)
		{
			return u.getPassword().equals(password);
		}
		else
		{
			return false;
		}
	}
	
	// Restituisce true se user1 e user2 sono amici, false altrimenti
	public boolean areFriends(String user1, String user2)
	{
		for (Friendship ef : estabilishedFriendships)
		{
			if (ef.involves(user1, user2))
			{
				return true;
			}
		}
		return false;
	}
	
	// Restituisce true se esiste una richiesta di amicizia in sospeso mandata da 'who'
	// a 'toWhom', false altrimenti
	public boolean friendshipRequested(String who, String toWhom)
	{
		for (Friendship pf : pendingFriendships)
		{
			if (pf.getFirstUser().equals(who) && pf.getSecondUser().equals(toWhom))
			{
				return true;
			}
		}
		return false;
	}
	
	// Finalizza una richiesta di amicizia che era in sospeso, stabilendo un nuovo legame
	// di amicizia tra 'addresser' e 'addressee', oppure lascia invariata la rete di 
	// amicizie (ciò dipende dalla scelta del destinatario della richiesta).
	public void finalizeFriendshipRequest(String addresser, String addressee, boolean grant)
	{			
		if (grant == true)
		{
    		// Stabilisco l'amicizia tra i due
    		estabilishedFriendships.add(new Friendship(addresser, addressee));			
		}
		
		// In ogni caso, rimuovo la richiesta originariamente spedita da 'addresser' ad 'addressee'
		pendingFriendships.removeIf(pf ->
			pf.getFirstUser().equals(addresser) && pf.getSecondUser().equals(addressee)
		);
	}
	
	// Registra una richiesta di amicizia spedita da 'addresser' ad 'addressee'.
	public void registerFriendshipRequest(String addresser, String addressee)
	{	
		// Registro la richiesta di amicizia mandata da 'addresser' a 'addressee'
		pendingFriendships.add(new Friendship(addresser, addressee));
	}
	
	// Restituisce la lista degli amici di 'username'
	public List<SocialUser> getFriends(String username)
	{
		List<SocialUser> friends = new ArrayList<SocialUser>();
		
		for (Friendship ef : estabilishedFriendships)
		{
			if (ef.getFirstUser().equals(username))
			{
				friends.add(userFromUsername(ef.getSecondUser()));
			}
			else if (ef.getSecondUser().equals(username))
			{
				friends.add(userFromUsername(ef.getFirstUser()));
			}
		}
		
		return friends;
	}
	
	// Restituisce la lista di tutti gli utenti registrati, eventualmente
	// filtrati attraverso il parametro passato
	public List<SocialUser> getUsers(String filter)
	{
		List<SocialUser> filteredUsers = new ArrayList<SocialUser>();
		
		for (SocialUser su : users)
		{
			if (filter != null)
			{
				if (su.getUsername().contains(filter))
				{
					filteredUsers.add(su);					
				}
			}
			else
			{
				filteredUsers.add(su);
			}
		}
		
		return filteredUsers;
	}
	
	// Restituisce gli utenti attualmente online
	public List<SocialUser> getOnlineUsers()
	{
		List<SocialUser> onlineUsers = new ArrayList<SocialUser>();
		for (SocialUser su : users)
		{
			if (su.isOnline())
			{
				onlineUsers.add(su);
			}
		}
		
		return onlineUsers;
	}
	
	// Memorizza un contenuto pubblicato che è d'interesse per 'recipient'
	public void storeContent(String content, String recipient)
	{
		pendingContents.add(new PendingContent(content, recipient));
	}
	
	// Restituisce (ed elimina) tutti i contenuti in sospeso per l'utente 'username'
	public List<String> getPendingContents(String username)
	{
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < pendingContents.size(); i++)
		{
			PendingContent content = pendingContents.get(i);
			if (content.getRecipient().equals(username))
			{
				// Questo contenuto è destinato ad 'username'
				
				result.add(content.getContent());
				pendingContents.remove(i);
				i--;
			}
		}
		
		return result;
	}
	
	// Aggiorna lo stato di tutti gli utenti
	public void updateUsersState()
	{
		for (SocialUser user : users)
		{
			if (user.isOnline())
			{
				// L'utente risulta online, verifico se lo è davvero
				
				try
				{
					Socket dummySock = new Socket();
					dummySock.connect(user.getProbingSockAddress());
					
					dummySock.close();    			
				}
				catch (IOException e)
				{
					// L'utente non è online, quindi lo metto offline
					
					user.setSessionToken(null);
					user.setProbingSockAddress(null);
					user.setFriendshipRequestsSockAddress(null);
				}
			}
		}
	}
	
	// Elimina le richieste di amicizia non riscontrate che sono scadute, dove per
	// scadute s'intende più vecchie di 'expireTime'
	public void deleteExpiredFriendshipRequests(int expireTime)
	{
		for (int i = 0; i < pendingFriendships.size(); i++)
		{
			Friendship pf = pendingFriendships.get(i);
			
			// L'età (in ore) della richiesta di amicizia
			long age = (new Date().getTime() - pf.getRequestDate().getTime()) / (1000 * 60 * 60);
			if (age >= expireTime)
			{
				pendingFriendships.remove(i);
				i--;
			}
		}
	}
	
}
