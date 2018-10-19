package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;

import simplesocial.socialserver.*;

public class FriendshipGrantSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;
	
	public FriendshipGrantSupplier(Socket communicationSocket)
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
			
			// Leggo l'username dell'utente che aveva spedito la richiesta,
			// la scelta dell'utente destinatario della richiesta e token
			// dalla connessione stabilita col client
			String addresserUsername = reader.readLine();
			boolean addresseeChoice = Boolean.parseBoolean(reader.readLine());
			String token = reader.readLine();
			
			// La risposta da spedire sulla connessione
			String reply = null;
			
			// Il destinatario della richiesta di amicizia
			SocialUser addressee = manager.userFromToken(token);
			if (addressee == null)
			{
				// L'utente risulta offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// L'utente risulta correttamente online
				
    			String addresseeUsername = addressee.getUsername();
    			if (!manager.friendshipRequested(addresserUsername, addresseeUsername))
    			{
    				// Non c'è alcuna richiesta di amicizia in sospeso spedita da
    				// 'addresser' ad 'addressee'
    				reply = "MISSING_ORIGINAL_REQUEST";
    			}
    			else 
    			{
    				// Esiste una richesta di amicizia in sospeso spedita da
    				// 'addresser' ad 'addressee'
        			
    				if (manager.areFriends(addresserUsername, addresseeUsername))
    				{
        				// I due sono già amici
    					reply = "ALREADY_FRIENDS";
    				}
        			else
        			{
        				// I due NON erano già amici, quindi aggiorno (eventualmente) 
        				// la rete di amicizie
        				manager.finalizeFriendshipRequest(addresserUsername, addresseeUsername, addresseeChoice);
        				reply = "FRIENDS_UPDATED";
        			}
			
    			}
			}
			
			
			// Scrivo la risposta (contenente l'esito dell'operazione)
			BufferedWriter writer = new BufferedWriter(
										new OutputStreamWriter(
											this.communicationSocket.getOutputStream()));
			
			writer.write(reply);
			writer.newLine();
			writer.flush();
			
		}
		catch (IOException e)
		{
			System.out.println("Si è verificato un errore durante la conferma/il rifiuto della richiesta di amicizia.");
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
