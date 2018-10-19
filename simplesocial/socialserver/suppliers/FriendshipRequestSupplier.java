package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;

import simplesocial.socialserver.SimpleSocialManager;
import simplesocial.socialserver.SocialUser;

public class FriendshipRequestSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;
	
	public FriendshipRequestSupplier(Socket communicationSocket)
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
			
			// Leggo username dell'utente a cui si vuole inoltrare la richiesta 
			// e token dalla connessione stabilita col client
			String addresseeUsername = reader.readLine();
			String token = reader.readLine();
			
			// La risposta da spedire sulla connessione
			String reply = null;
			
			// Il mittente della richiesta di amicizia
			SocialUser addresser = manager.userFromToken(token);
			if (addresser == null)
			{
				// L'utente risulta offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// L'utente risulta correttamente online
				
    			SocialUser addressee = manager.userFromUsername(addresseeUsername);
    			if (addressee == null)
    			{
    				// Si sta tentando di aggiungere come amico/a un utente che non è registrato
    				reply = "UNKNOWN_USER";
    			}
    			else 
    			{
    				// Il destinatario della richiesta è un utente registrato  				         			
    				
        			String addresserUsername = addresser.getUsername();
        			
        			if (manager.areFriends(addresserUsername, addresseeUsername) 
    					|| manager.friendshipRequested(addresserUsername, addresseeUsername))
        			{
        				// I due sono già amici, oppure esiste già una richiesta di
        				// amicizia spedita da 'addresser' ad 'addressee'
        				reply = "FRIENDSHIP_REQUEST_ALREADY_SENT";
        			}
        			else
        			{        	
        				// I due NON erano già amici, inoltre non esisteva alcuna richiesta
        				// di amicizia spedita da 'addresser' ad 'addressee'
        				
        				InetSocketAddress addresseeProbSockAddr;
        				Socket addresseeProbSock = null;
        				boolean addresseeOnline = true;
        				try
        				{
                			// Apro una connessione TCP con il destinatario della richiesta per verificare se è online
            				addresseeProbSockAddr = addressee.getProbingSockAddress();
            				addresseeProbSock = new Socket(addresseeProbSockAddr.getAddress(), addresseeProbSockAddr.getPort());					
            				addresseeProbSock.close();
        				}
            			catch (IOException e)
        				{
            				// Non sono riuscito a stabilire una connessione col destinatario 
            				// della richiesta di amicizia, quindi questi è offline
            				reply = "USER_OFFLINE";
            				
            				addresseeOnline = false;
        				}
        				
        				if (addresseeOnline)
        				{
            				// Il destinatario/a della richiesta è online, quindi posso registrare
        					// la richiesta e avvertirlo/a
        					
        					// Registro la richiesta di amicizia
            				manager.registerFriendshipRequest(addresserUsername, addresseeUsername);
            				reply = "FRIENDSHIP_REQUEST_FORWARDED";
        					
            				// Apro la socket
            				InetSocketAddress addresseeFriendsSockAddr = addressee.getFriendshipRequestsSockAddress();
            				Socket addresseeFriendsSock = new Socket(addresseeFriendsSockAddr.getAddress(), addresseeFriendsSockAddr.getPort());
            				
                			// Comunico al destinatario/a della richiesta chi sta cercando 
                			// di aggiungerlo/a come amico/a
                			BufferedWriter writer = new BufferedWriter(
                										new OutputStreamWriter(
                											addresseeFriendsSock.getOutputStream()));
                			
                			writer.write(addresserUsername);
                			writer.newLine();
                			writer.flush();
                			            			
                			// Chiudo la connessione TCP col destinatario della richiesta
                			addresseeFriendsSock.close();
        				}
        			}
        			
				}
			}
						
			// Scrivo la risposta (contenente l'esito dell'operazione) al mittente
			BufferedWriter writer = new BufferedWriter(
										new OutputStreamWriter(
											this.communicationSocket.getOutputStream()));
			
			writer.write(reply);
			writer.newLine();
			writer.flush();
			
		}
		catch (IOException e)
		{
			System.out.println("Si è verificato un errore durante l'inoltro della richiesta di amicizia.");
		}
		finally
		{
			try
			{
				// Chiudo la connessione TCP col mittente
				if (this.communicationSocket != null) { this.communicationSocket.close(); }				
			}
			catch (IOException e) { ; }
		}		
	}

}
