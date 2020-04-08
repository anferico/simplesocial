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
                    this.communicationSocket.getInputStream()
                )
            );
			
			// Read username and password
			String username = reader.readLine();
			String password = reader.readLine();
            // Read the address of the socket that the server will use to probe the
            // state (online or offline) of the user
			InetAddress probingAddress = InetAddress.getByName(reader.readLine());
			int probingPort = Integer.parseInt(reader.readLine());
            // Read the address of the socket that the server will use to send friendship
            // requests sent to the user
			InetAddress requestsAddress = InetAddress.getByName(reader.readLine());
			int requestsPort = Integer.parseInt(reader.readLine());
			
			String reply = null;
			if (!manager.userExists(username) || !manager.isValidPassword(username, password))
			{
				// The user does not exist OR the given credentials are wrong
				reply = "INVALID_CREDENTIALS";
			}
			else if (manager.isUserOnline(username))
			{
				// The user is already logged in
				reply = "ALREADY_LOGGED_IN";
			}
			else
			{
				UUID token = UUID.randomUUID();
				SocialUser loggedInUser = manager.userFromUsername(username);
				// Set the user state as online by providing them a token
				loggedInUser.setSessionToken(token.toString());
				loggedInUser.setProbingSockAddress(
					new InetSocketAddress(probingAddress, probingPort)
				);
				loggedInUser.setFriendshipRequestsSockAddress(
					new InetSocketAddress(requestsAddress, requestsPort)
				);
				
				reply = token.toString();
			}
			
			// Send the reply to the user
			BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(
                    this.communicationSocket.getOutputStream()
                )
            );
			writer.write(reply);
			writer.newLine();
			writer.flush();
			
		}
		catch (IOException e)
		{
			System.out.println("An error occurred while trying to log in.");
		}
		finally
		{
			try
			{
				// Close the connection with the client
				if (this.communicationSocket != null) { this.communicationSocket.close(); }				
			}
			catch (IOException e) { ; }
		}
	}

}
