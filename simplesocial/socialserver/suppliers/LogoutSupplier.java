package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;

import simplesocial.socialserver.*;

public class LogoutSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;	
	
	public LogoutSupplier(Socket communicationSocket)
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
			
			// Leggo il comando simbolico 'LOGOUT' e il token
			// dalla connessione stabilita col client
			String command = reader.readLine();
			String token = reader.readLine();
			
			// La risposta da spedire sulla connessione
			String reply = null;
			
			// L'utente che desidera effettuare il logout
			SocialUser user = manager.userFromToken(token);
			if (user == null)
			{
				// L'utente risulta offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// L'utente risulta correttamente online
				 				      
				// Imposto lo stato dell'utente a 'offline', cancellando il token 
				// assegnatogli precedentemente
				user.setSessionToken(null);
				// Imposto a null gli indirizzi che prima utilizzavo per contattarlo
				user.setProbingSockAddress(null);
				user.setFriendshipRequestsSockAddress(null);
    			
    			reply = "LOGGED_OUT";
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
			System.out.println("Si è verificato un errore durante il logout.");
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
