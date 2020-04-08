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
                    this.communicationSocket.getInputStream()
                )
            );
			
            // Read the content of the post
            String content = reader.readLine();
            // Read the user token
			String token = reader.readLine();
			
			// The reply to be sent over the connection
			String reply = null;
			
			// The user who issued the posting request
			SocialUser publisher = manager.userFromToken(token);
			if (publisher == null)
			{
				// The user appears to be offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// The user is online
				 				         							
    			for (String follower : publisher.getFollowers())
    			{	
    				SocialUser followerUser = manager.userFromUsername(follower);
    				if (followerUser.isOnline())
    				{
    					// The follower is online
    					
    					// Notify the follower about the newly published content
    					followerUser.getStub().onNewContentPublished(content);
    				}
    				else
    				{
    					// The follower is offline
    					
    					// Save the content for when the follower will be online
    					manager.storeContent(content, follower);
    				}
    			}
    			
    			reply = "CONTENT_PUBLISHED";
			}
						
			// Send the reply to the user. It contains the outcome of the request
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
			System.out.println("An error occurred while trying to publish the content.");
		}
		finally
		{
			try
			{
				// CLose the connection with the client
				if (this.communicationSocket != null) { this.communicationSocket.close(); }				
			}
			catch (IOException e) { ; }
		}
	}

}
