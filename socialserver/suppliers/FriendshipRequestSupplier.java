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
                    this.communicationSocket.getInputStream()
                )
            );
			
			// Read the username of the user to whom the friendship request will be sent
            String addresseeUsername = reader.readLine();
            // Read the user token
			String token = reader.readLine();
			
			// The reply to be sent over the connection
			String reply = null;
			
			// The user who issued the friendship request
			SocialUser addresser = manager.userFromToken(token);
			if (addresser == null)
			{
				// The user appears to be offline
				reply = "TOKEN_EXPIRED";
			}
			else
			{   
				// The user is online
				
    			SocialUser addressee = manager.userFromUsername(addresseeUsername);
    			if (addressee == null)
    			{
    				// We're trying to send a friendship request to an unregistered user
    				reply = "UNKNOWN_USER";
    			}
    			else 
    			{
        			String addresserUsername = addresser.getUsername();
        			
        			if (manager.areFriends(addresserUsername, addresseeUsername) 
    					|| manager.friendshipRequested(addresserUsername, addresseeUsername))
        			{
                        // The two are already friends OR 'addresser' has already sent
                        // a friendship request to 'addressee'
        				reply = "FRIENDSHIP_REQUEST_ALREADY_SENT";
        			}
        			else
        			{        	
        				InetSocketAddress addresseeProbSockAddr;
        				Socket addresseeProbSock = null;
        				boolean addresseeOnline = true;
        				try
        				{
                			// Probe the state of the recipient of the request
            				addresseeProbSockAddr = addressee.getProbingSockAddress();
            				addresseeProbSock = new Socket(
                                addresseeProbSockAddr.getAddress(), 
                                addresseeProbSockAddr.getPort()
                            );					
            				addresseeProbSock.close();
        				}
            			catch (IOException e)
        				{
            				//Tthe recipient of this friendship request is offline
            				reply = "USER_OFFLINE";
            				
            				addresseeOnline = false;
        				}
        				
        				if (addresseeOnline)
        				{
                            // The recipient is online
        					
        					// Register the friendship request
            				manager.registerFriendshipRequest(addresserUsername, addresseeUsername);
            				reply = "FRIENDSHIP_REQUEST_FORWARDED";
        					
            				// Open the socket
            				InetSocketAddress addresseeFriendsSockAddr = addressee.getFriendshipRequestsSockAddress();
            				Socket addresseeFriendsSock = new Socket(addresseeFriendsSockAddr.getAddress(), addresseeFriendsSockAddr.getPort());
            				
                            // Send the friendship request to the recipient
                			BufferedWriter writer = new BufferedWriter(
                				new OutputStreamWriter(
                                    addresseeFriendsSock.getOutputStream()
                                )
                            );                			
                			writer.write(addresserUsername);
                			writer.newLine();
                			writer.flush();
                			            			
                			// CLose the connection with the recipient
                			addresseeFriendsSock.close();
        				}
        			}
        			
				}
			}
						
			// Send the reply to the sender. It contains the outcome of the request
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
			System.out.println("An error occurred while sending the friendship request.");
		}
		finally
		{
			try
			{
				// Close the connection with the sender
				if (this.communicationSocket != null) { this.communicationSocket.close(); }				
			}
			catch (IOException e) { ; }
		}		
	}

}
