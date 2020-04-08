package simplesocial.socialserver.suppliers;

import java.io.*;
import java.net.*;
import java.util.*;

import simplesocial.socialserver.*;

public class UsersSearchSupplier implements Runnable
{
    private Socket communicationSocket = null;
    private SimpleSocialManager manager;    
    
    public UsersSearchSupplier(Socket communicationSocket)
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
            
            // Read the search key
            String filter = reader.readLine();
            if (filter.equals("<nofilter>"))
            {
                // Special value corresponding to "no filters"
                filter = null;
            }
            // Read the user token        
            String token = reader.readLine();
            
            // The reply I'll be sending over the connection
            String reply = null;
            
            // The user who issued the search
            SocialUser user = manager.userFromToken(token);
            if (user == null)
            {
                // The user appears to be offline
                reply = "TOKEN_EXPIRED";
            }
            else
            {   
                // The user is online                               
                
                // Build the resulting list 
                StringBuilder friendsList = new StringBuilder();
                
                List<SocialUser> ssUsers = manager.getUsers(filter);
                for (int i = 0; i < ssUsers.size(); i++)
                {
                    SocialUser su = ssUsers.get(i);                    
                    friendsList.append(su.getUsername());
                    
                    if (i < ssUsers.size() - 1)
                    {
                        // '-' separates each user from the others
                        friendsList.append("-");
                    }
                }                
                
                // The content of the reply is the list of filtered users
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
            System.out.println("An error occurred while retrieving the list of users");
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
