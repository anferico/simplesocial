package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;
import java.util.*;

import simplesocial.socialserver.*;

public class FriendsListSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;	
	
	public FriendsListSupplier(Socket communicationSocket)
	{
		this.communicationSocket = communicationSocket;
		this.manager = SimpleSocialManager.getManager();
	}

	@Override
	public void run()
	{
		try
		{
			BufferedReader reader = new BufferedReader(
										new InputStreamReader(
											this.communicationSocket.getInputStream()));
			
			// Leggo il comando simbolico 'GET_FRIENDS' e il token dalla
			// connessione stabilita col client
			String command = reader.readLine();
			String token = reader.readLine();
			
			// La risposta da spedire sulla connessione
			String reply = null;
			
			// L'utente che ha richiesto la lista dei propri amici
			SocialUser user = manager.userFromToken(token);
			if (user == null)
			{
				// L'utente risulta offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// L'utente risulta correttamente online 				    			   
				
				String username = user.getUsername();
				
				// Costruisco via via la lista degli amici di 'username', secondo un
				// formato concordato col client
				StringBuilder friendsList = new StringBuilder("");
				
				List<SocialUser> theirFriends = manager.getFriends(username);
				for (int i = 0; i < theirFriends.size(); i++)
				{
					SocialUser su = theirFriends.get(i);
					if (su.isOnline())
					{
						// '!' indica che l'utente è online
						friendsList.append("!");
					}
					
					friendsList.append(su.getUsername());
					
					if (i < theirFriends.size() - 1)
					{
						// '-' separa gli amici
						friendsList.append("-");
					}
				}				
				
    			// Il contenuto della risposta diventa la lista degli amici
    			reply = friendsList.toString();        			        						
			}
			
			// Scrivo la risposta al client
			BufferedWriter writer = new BufferedWriter(
										new OutputStreamWriter(
											this.communicationSocket.getOutputStream()));
			
			writer.write(reply);
			writer.newLine();
			writer.flush();
			
		}
		catch (IOException e)
		{
			System.out.println("Si è verificato un errore durante il recupero degli amici.");
		}
		finally
		{
			try
			{
				// Chiudo la connessione con il client
				if (this.communicationSocket != null) { this.communicationSocket.close(); }				
			}
			catch (IOException e) { ; }
		}		
	}

}
