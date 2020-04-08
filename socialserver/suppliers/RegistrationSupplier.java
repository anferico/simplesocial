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
                    this.communicationSocket.getInputStream()
                )
            );
			
			// Read username and password
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
			
			// Write the reply
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
			System.out.println("An error occurred while registering the user \"" + username + "\".");
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
