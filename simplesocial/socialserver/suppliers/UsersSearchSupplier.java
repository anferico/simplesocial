package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;
import java.util.*;

import simplesocial.socialserver.*;

public class UsersSearchSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;	
	
	public UsersSearchSupplier(Socket communicationSocket)
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
			
			// Leggo la stringa per filtrare gli utenti e il token dalla
			// connessione stabilita col client
			String filter = reader.readLine();
			if (filter.equals("<nofilter>"))
			{
				// Valore speciale per indicare l'assenza di filtri
				filter = null;
			}
			String token = reader.readLine();
			
			// La risposta da spedire sulla connessione
			String reply = null;
			
			// L'utente che ha richiesto la lista degli iscritti a Simple Social
			SocialUser user = manager.userFromToken(token);
			if (user == null)
			{
				// L'utente risulta offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// L'utente risulta correttamente online 				    			   
				
				// Costruisco via via la lista degli utenti secondo un
				// formato concordato col client
				StringBuilder friendsList = new StringBuilder();
				
				List<SocialUser> ssUsers = manager.getUsers(filter);
				for (int i = 0; i < ssUsers.size(); i++)
				{
					SocialUser su = ssUsers.get(i);					
					friendsList.append(su.getUsername());
					
					if (i < ssUsers.size() - 1)
					{
						// '-' separa gli utenti
						friendsList.append("-");
					}
				}				
				
    			// Il contenuto della risposta diventa la lista degli utenti filtrati
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
			System.out.println("Si è verificato un errore durante il recupero degli utenti.");
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
