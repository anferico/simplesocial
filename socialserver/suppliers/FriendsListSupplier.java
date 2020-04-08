package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;
import java.util.*;

import simplesocial.socialserver.*;

public class FriendsListSupplier implements Runnable
{
	private Socket communicationSocket = null;
	private SimpleSocialManager manager;	
	
	public FriendsListSupplier(Socket communicationSocket)
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
			
			// Read the dummy content 'GET_FRIENDS'
            String command = reader.readLine();
            // Read the user token
			String token = reader.readLine();
			
			// The reply to be sent over the connection
			String reply = null;
			
			// The user who issued the request
			SocialUser user = manager.userFromToken(token);
			if (user == null)
			{
				// The user appears to be offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// The user is online 				    			   
				
				String username = user.getUsername();
				
				// Build the friends list
				StringBuilder friendsList = new StringBuilder("");
				
				List<SocialUser> theirFriends = manager.getFriends(username);
				for (int i = 0; i < theirFriends.size(); i++)
				{
					SocialUser su = theirFriends.get(i);
					if (su.isOnline())
					{
						// '!' intimates that the user is online
						friendsList.append("!");
					}
					
					friendsList.append(su.getUsername());
					
					if (i < theirFriends.size() - 1)
					{
						// '-' separates a friend from the others
						friendsList.append("-");
					}
				}				
				
    			// The content of the reply becomes the list of friends
    			reply = friendsList.toString();        			        						
			}
			
			// Send the reply to the client
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
			System.out.println("An error occurred while trying to retrieve the list of friends.");
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
