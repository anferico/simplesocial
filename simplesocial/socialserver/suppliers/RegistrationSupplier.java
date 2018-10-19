package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;

import simplesocial.socialserver.SimpleSocialManager;

public class RegistrationSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;
	
	public RegistrationSupplier(Socket communicationSocket)
	{
		this.communicationSocket = communicationSocket;
		this.manager = SimpleSocialManager.getManager();
	}

	@Override
	public void run()
	{
		String username = null;
		String password = null;
		try
		{
			BufferedReader reader = new BufferedReader(
										new InputStreamReader(
											this.communicationSocket.getInputStream()));
			
			// Leggo username e password
			username = reader.readLine();
			password = reader.readLine();
			
			String reply = null;
			if (!manager.userExists(username))
			{				
				manager.registerUser(username, password);
				reply = "REGISTERED";
			}
			else
			{
				reply = "USERNAME_ALREADY_TAKEN";
			}
			
			// Scrivo la risposta
			BufferedWriter writer = new BufferedWriter(
										new OutputStreamWriter(
											this.communicationSocket.getOutputStream()));
			
			writer.write(reply);
			writer.newLine();
			
			writer.flush();
		}
		catch (IOException e)
		{
			System.out.println("Si è verificato un errore durante la registrazione dell'utente\"" + username + "\".");
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
