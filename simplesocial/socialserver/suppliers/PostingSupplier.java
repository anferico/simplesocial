package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;

import simplesocial.socialserver.*;

public class PostingSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;	
	
	public PostingSupplier(Socket communicationSocket)
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
			
			// Leggo il contenuto che si desidera pubblicare e il token
			// dalla connessione stabilita col client
			String content = reader.readLine();
			String token = reader.readLine();
			
			// La risposta da spedire sulla connessione
			String reply = null;
			
			// l'utente che desidera pubblicare il contenuto
			SocialUser publisher = manager.userFromToken(token);
			if (publisher == null)
			{
				// L'utente risulta offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// L'utente risulta correttamente online
				 				         							
    			for (String follower : publisher.getFollowers())
    			{	
    				SocialUser followerUser = manager.userFromUsername(follower);
    				if (followerUser.isOnline())
    				{
    					// Il follower è attualmente online
    					
    					// Avverto il SocialClient del follower
    					followerUser.getStub().onNewContentPublished(content);
    				}
    				else
    				{
    					// Il follower è attualmente offline
    					
    					// Memorizzo il contenuto per spedirlo quando il follower
    					// farà il login
    					manager.storeContent(content, follower);
    				}
    			}
    			
    			reply = "CONTENT_PUBLISHED";
			}
						
			// Scrivo la risposta (contenente l'esito dell'operazione) al client
			BufferedWriter writer = new BufferedWriter(
										new OutputStreamWriter(
											this.communicationSocket.getOutputStream()));
			
			writer.write(reply);
			writer.newLine();
			writer.flush();
			
		}
		catch (IOException e)
		{
			System.out.println("Si è verificato un errore durante la pubblicazione del contenuto.");
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
