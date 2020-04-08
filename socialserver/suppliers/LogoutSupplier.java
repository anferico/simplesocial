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
                    this.communicationSocket.getInputStream()
                )
            );
			
			// Read the dummy content 'LOGOUT'
            String command = reader.readLine();
            // Read the user token
			String token = reader.readLine();
			
			// The reply to be sent over the connection
			String reply = null;
			
			// The user who issued the logout request
			SocialUser user = manager.userFromToken(token);
			if (user == null)
			{
				// The user appears to be offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// The user is online
				 				      
				// Mark the user state as offline
				user.setSessionToken(null);
				user.setProbingSockAddress(null);
				user.setFriendshipRequestsSockAddress(null);
    			
    			reply = "LOGGED_OUT";
			}
						
			// Send the reply to the client. It contains the outcome of the request
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
			System.out.println("An error occurred while trying to log out.");
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
