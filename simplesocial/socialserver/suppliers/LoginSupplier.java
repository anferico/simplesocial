package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;
import java.util.*;

import simplesocial.socialserver.*;

public class LoginSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;
	
	public LoginSupplier(Socket communicationSocket)
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
			
			// Leggo username e password dalla connessione stabilita col client
			String username = reader.readLine();
			String password = reader.readLine();
			// Leggo indirizzo e porta della socket sulla quale il server cercherà
			// di aprire una connessione per verificare se l'utente è online
			InetAddress probingAddress = InetAddress.getByName(reader.readLine());
			int probingPort = Integer.parseInt(reader.readLine());
			// Leggo indirizzo e porta della socket sulla quale il server cercherà
			// di aprire una connessione per spedire le richieste di amicizia indirizzate
			// a questo utente
			InetAddress requestsAddress = InetAddress.getByName(reader.readLine());
			int requestsPort = Integer.parseInt(reader.readLine());
			
			String reply = null;
			if (!manager.userExists(username) || !manager.isValidPassword(username, password))
			{
				// L'utente non esiste oppure username e password non corrispondono
				reply = "INVALID_CREDENTIALS";
			}
			else if (manager.isUserOnline(username))
			{
				// L'utente ha già effettuato il login
				reply = "ALREADY_LOGGED_IN";
			}
			else
			{
				UUID token = UUID.randomUUID();
				SocialUser loggedInUser = manager.userFromUsername(username);
				// Imposto lo stato dell'utente a 'online', assegnandogli un token
				loggedInUser.setSessionToken(token.toString());
				// Registro gli indirizzi ai quali potrò contattarlo in caso di necessità
				loggedInUser.setProbingSockAddress(
					new InetSocketAddress(probingAddress, probingPort)
				);
				loggedInUser.setFriendshipRequestsSockAddress(
					new InetSocketAddress(requestsAddress, requestsPort)
				);
				
				reply = token.toString();
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
			System.out.println("Si è verificato un errore durante la registrazione dell'utente.");
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
