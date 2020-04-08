package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;

import simplesocial.socialserver.*;

public class FriendshipGrantSupplier implements Runnable
{
    private Socket communicationSocket = null;
    private SimpleSocialManager manager;
    
    public FriendshipGrantSupplier(Socket communicationSocket)
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
            
            // Read the username of the sender of the friendship request
            String addresserUsername = reader.readLine();
            // Read the choice that the recipient of the friendship request has made
            boolean addresseeChoice = Boolean.parseBoolean(reader.readLine());
            // Read the user token
            String token = reader.readLine();
            
            // The reply to be sent over the connection
            String reply = null;
            
            // The recipient of the friendship request
            SocialUser addressee = manager.userFromToken(token);
            if (addressee == null)
            {
                // The user appears to be offline
                reply = "TOKEN_EXPIRED";
            }
            else
            {   
                // The user is online
                
                String addresseeUsername = addressee.getUsername();
                if (!manager.friendshipRequested(addresserUsername, addresseeUsername))
                {
                    // There's no friendship request sent from 'addresser' to 'addressee'
                    reply = "MISSING_ORIGINAL_REQUEST";
                }
                else 
                {
                    if (manager.areFriends(addresserUsername, addresseeUsername))
                    {
                        // The two were already friends
                        reply = "ALREADY_FRIENDS";
                    }
                    else
                    {
                        manager.finalizeFriendshipRequest(addresserUsername, addresseeUsername, addresseeChoice);
                        reply = "FRIENDS_UPDATED";
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
            System.out.println("An error occurred while trying to accept/deny the friendship request.");
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
