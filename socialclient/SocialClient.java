package simplesocial.socialclient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.*;

import simplesocial.*;
import simplesocial.socialserver.*;

public class SocialClient implements ISocialClient
{    
    private String myUsername;       // The username I chose
    private String token;              // Login token
    private Date tokenReceptionDate; // Date and time of reception of the token

    // Contents published by users I follow
    private List<String> availableContents;
    // Pending friendship requests
    private List<String> pendingFriendshipRequests;
    // Thread that receives messages sent to the multicast group
    private Thread mcGroupMemberThread = null;
    // Thread that responds to "probing" requests from the server
    private Thread probeListenerThread = null;
    // Thread that catches new friendship requests
    private Thread requestsListenerThread = null;
    
    public SocialClient() { }
    
    @Override
    public void register(String username, String password) throws SocialException
    {
        // Socket for communicating with the server
        Socket sock = null;
        try
        {
            sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_REG_PORT);
        }
        catch (IOException e)
        {                        
            throw new SocialException("Unable to connect to server.");
        }
        
        // Begin communicating with the server
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String reply = null;
        
        try
        {
            // Send username and password to the server
            
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            
            writer.write(username);
            writer.newLine();
            writer.write(password);
            writer.newLine();
                        
            writer.flush();
            
            // Wait for the server's reply
            reader = new BufferedReader(
                new InputStreamReader(
                    sock.getInputStream()
                )
            );            
            reply = reader.readLine();            
        }
        catch (IOException e)
        {
            throw new SocialException("An error occurred during the registration.");
        }
        finally
        {
            // Close the connection with the server
            try
            {
                if (sock != null) { sock.close(); }
            }
            catch (IOException e){ ; }
        }
        
        // Analyze the server's reply
        if (reply.equals("USERNAME_ALREADY_TAKEN"))
        {
            throw new SocialException(
                "The chosen username is not available. Please choose another one."
            );
        }
        
    }

    @Override
    public void login(String username, String password)    throws SocialException
    {
        // Socket for communicating with the server
        Socket sock = null;
        try
        {
            sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_LOGIN_PORT);
        }
        catch (IOException e)
        {
            throw new SocialException("Unable to connect to server.");
        }

        // Begin communicating with the server
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String reply = null;
        
        // Socket used for receiving probing requests from the server
        ServerSocket probeSock = null;
        // Socket used for receiving friendship requests
        ServerSocket requestsSock = null;
        try
        {
            probeSock = new ServerSocket(0);
            requestsSock = new ServerSocket(0);
            
            // Send: 
            // - username 
            // - password
            // - socket address of 'probeSock'
            // - socket address of 'requestsSock'
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            writer.write(username);
            writer.newLine();
            writer.write(password);
            writer.newLine();
            writer.write(probeSock.getInetAddress().getHostAddress());
            writer.newLine();
            writer.write(String.valueOf(probeSock.getLocalPort()));
            writer.newLine();
            writer.write(requestsSock.getInetAddress().getHostAddress());
            writer.newLine();
            writer.write(String.valueOf(requestsSock.getLocalPort()));
            writer.newLine();
            
            writer.flush();
            
            // Wait for the server's reply
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            reply = reader.readLine();
        }
        catch (IOException e)
        {
            throw new SocialException("An error occurred during login.");
        }
        finally
        {
            // Close the connection with the server
            try
            {
                if (sock != null) { sock.close(); }
            }
            catch (IOException e){ ; }
        }
        
        if (reply.equals("INVALID_CREDENTIALS"))
        {
            try
            {
                if (probeSock != null) { probeSock.close(); }
                if (requestsSock != null) { requestsSock.close(); }
            }
            catch (IOException e){ ; }
            
            throw new SocialException("Invalid username or password.");
        }
        else if (reply.equals("ALREADY_LOGGED_IN"))
        {
            try
            {
                if (probeSock != null) { probeSock.close(); }
                if (requestsSock != null) { requestsSock.close(); }
            }
            catch (IOException e){ ; }
            
            throw new SocialException("The user associated with this account has already logged in.");
        }
        else
        {
            // Save username and session token
            myUsername = username;
            token = reply;
            tokenReceptionDate = new Date();
        }
                
                
        // Login succeded
                        
        
        try
        {
            // Retrieve pending friendship requests and new contents
            ObjectInputStream inStream = new ObjectInputStream(
                new FileInputStream(myUsername + ".pendingRequestsAndContents")
            );
            pendingFriendshipRequests = (List<String>) inStream.readObject();
            availableContents = (List<String>) inStream.readObject();

            inStream.close();
        }
        catch (Exception e)
        {
            // Nothing was saved, so nothing to retrieve
            
            availableContents = Collections.synchronizedList(new ArrayList<String>());
            pendingFriendshipRequests = Collections.synchronizedList(new ArrayList<String>());
        }
        
        
        try
        {
            // Register notification callbacks
            Registry registry = LocateRegistry.getRegistry(Constants.SERVER_RMI_REGISTRY_PORT);
            
            IFollowingService followingService = (IFollowingService) registry.lookup("FOLLOWING_SERVICE");
            
            SocialFollower stub = new SocialFollower(availableContents);            
            followingService.registerCallback(username, stub);    
        }
        catch (RemoteException | NotBoundException e)
        {
            try
            {
                if (probeSock != null) { probeSock.close(); }
                if (requestsSock != null) { requestsSock.close(); }
            }
            catch (IOException f){ ; }
            
            throw new SocialException("An error occurred while finalizing the login procedure.");            
        }
        
        
        // Start the thread that will reply to keep-alive messages
        MulticastGroupListener mcGroupMember = new MulticastGroupListener(username);
        if (mcGroupMemberThread != null)
        {
            mcGroupMemberThread.interrupt();
        }
        mcGroupMemberThread = new Thread(mcGroupMember);
        mcGroupMemberThread.start();        
        
        // Start the thread that will respond to probing requests from the server
        ProbeMessagesListener probeListener = new ProbeMessagesListener(probeSock);
        if (probeListenerThread != null)
        {
            probeListenerThread.interrupt();
        }
        probeListenerThread = new Thread(probeListener);
        probeListenerThread.start();
        
        // Start the thread that will receive friendship requests
        FriendshipRequestsListener requestsListener = new FriendshipRequestsListener(
            requestsSock, 
            pendingFriendshipRequests
        );
        if (requestsListenerThread != null)
        {
            requestsListenerThread.interrupt();
        }
        requestsListenerThread = new Thread(requestsListener);
        requestsListenerThread.start();
        
    }

    @Override
    public void sendFriendshipRequest(String username) throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        // Socket for communicating with the server
        Socket sock = null;
        try
        {
            sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_FSREQUEST_PORT);
        }
        catch (IOException e)
        {
            throw new SocialException("Unable to connect to server.");
        }

        // Begin communicating with the server
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String reply = null;
        
        try
        {
            // Send the username of the user I want to add as a friend
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            writer.write(username);
            writer.newLine();
            // Attach the token
            writer.write(token.toString());
            writer.newLine();    
            
            writer.flush();
            
            // Wait for the server's reply
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            reply = reader.readLine();
        }
        catch (IOException e)
        {
            throw new SocialException("An error occurred while sending the friendship request.");
        }
        finally
        {
            // Close the connection with the server
            try
            {
                if (sock != null) { sock.close(); }
            }
            catch (IOException e){ ; }
        }
        
        // Analyze the server's reply
        if (!reply.equals("FRIENDSHIP_REQUEST_FORWARDED"))
        {
            SocialException se = null;
            if (reply.equals("TOKEN_EXPIRED"))
            {
                se = new ExpiredTokenException();
            }
            else if (reply.equals("FRIENDSHIP_REQUEST_ALREADY_SENT"))
            {
                se = new SocialException("You already sent a friendship request to this user in the past.");
            }
            else if (reply.equals("USER_OFFLINE"))
            {
                se = new SocialException("The user is offline. Try again later.");
            }
            else if (reply.equals("UNKNOWN_USER"))
            {
                se = new SocialException("The specified user does not exist.");
            }
            
            throw se;
        }
    }
    
    @Override
    public List<String> getPendingFriendshipRequests() throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        return pendingFriendshipRequests;
    }

    @Override
    public void respondToFriendshipRequest(String username, boolean choice) throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        // Socket for communicating with the server
        Socket sock = null;
        try
        {
            sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_FSGRANT_PORT);
        }
        catch (IOException e)
        {
            throw new SocialException("Unable to connect to server.");
        }

        // Begin communicating with the server
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String reply = null;
        
        try
        {
            // Send the username and the choice to the server
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            writer.write(username);
            writer.newLine();
            writer.write(String.valueOf(choice));
            writer.newLine();
            // Attach the token
            writer.write(token.toString());
            writer.newLine();                
            
            writer.flush();
            
            // Wait for the server's reply
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            reply = reader.readLine();
        }
        catch (IOException e)
        {
            throw new SocialException("An error occurred while accepting/denying the friendship request.");
        }
        finally
        {
            // Close the connection with the server
            try
            {
                if (sock != null) { sock.close(); }
            }
            catch (IOException e){ ; }
        }
        
        // Analyze the server's reply
        if (!reply.equals("FRIENDS_UPDATED"))
        {    
            SocialException se = null;
            if (reply.equals("TOKEN_EXPIRED"))
            {
                se = new ExpiredTokenException();
            }
            else if (reply.equals("MISSING_ORIGINAL_REQUEST"))
            {
                se = new SocialException("The friendship request has expired or it doesn't exist.");
            }
            else if (reply.equals("ALREADY_FRIENDS"))
            {
                se = new SocialException("You already replied to this friendship request in the past.");
            }
            
            throw se;            
        }
            
        pendingFriendshipRequests.removeIf(pf -> pf.equals(username));
    }

    @Override
    public List<Friend> getFriends() throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        // Socket for communicating with the server
        Socket sock = null;
        try
        {
            // java.net.ConnectException: Connection refused (the second time)
            sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_FRIENDS_PORT);
        }
        catch (IOException e)
        {
            throw new SocialException("Unable to connect to server.");
        }
        
        // Begin communicating with the server
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String reply = null;
        
        try
        {
            // Send the request to the server
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            writer.write("GET_FRIENDS");
            writer.newLine();
            // Attach the token
            writer.write(token.toString());
            writer.newLine();        
            
            writer.flush();
            
            // Wait for the server's reply
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            reply = reader.readLine();
        }
        catch (IOException e)
        {
            throw new SocialException("An error occurred while trying to retrieve a list of your friends.");
        }
        finally
        {
            // Close the connection with the server
            try
            {
                if (sock != null) { sock.close(); }
            }
            catch (IOException e){ ; }
        }
        
        // Analyze the server's reply
        if (reply.equals("TOKEN_EXPIRED"))
        {
            throw new ExpiredTokenException();
        }

        
        // Loop over the friends list sent by the server
        List<Friend> friends = new ArrayList<Friend>();
        if (reply.length() > 0)
        {
            for (String friend : reply.split("-"))
            {
                boolean isOnline = friend.startsWith("!");
                if (isOnline)
                {
                    friends.add(new Friend(friend.substring(1), true));
                }
                else
                {
                    friends.add(new Friend(friend, false));
                }
            }
        }
        
        return friends;
    }

    @Override
    public String[] findUsers(String searchKey)    throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        // Socket for communicating with the server
        Socket sock = null;
        try
        {
            sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_USERS_PORT);
        }
        catch (IOException e)
        {
            throw new SocialException("Unable to connect to server.");
        }
        
        
        // Begin communicating with the server
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String reply = null;
        
        try
        {
            // Send the search key to the server
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            writer.write(searchKey);
            writer.newLine();
            // Attach the token
            writer.write(token.toString());
            writer.newLine();                
            
            writer.flush();
            
            // Wait for the server's reply
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            reply = reader.readLine();
        }
        catch (IOException e)
        {
            throw new SocialException("An error occured while searching for a user.");
        }
        finally
        {
            // Close the connection with the server
            try
            {
                if (sock != null) { sock.close(); }
            }
            catch (IOException e){ ; }
        }
        
        // Analyze the server's reply
        if (reply.equals("TOKEN_EXPIRED"))
        {
            throw new ExpiredTokenException();
        }    

        String[] users = reply.split("-");
        
        return users;
    }

    @Override
    public void publishContent(String content) throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        // Socket for communicating with the server
        Socket sock = null;
        try
        {
            sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_CONTENT_PORT);
        }
        catch (IOException e)
        {
            throw new SocialException("Unable to connect to server.");
        }

        // Begin communicating with the server
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String reply = null;
        
        try
        {
            // Send the content of my post to the server
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            writer.write(content);
            writer.newLine();
            // Attach the token
            writer.write(token.toString());
            writer.newLine();            
            
            writer.flush();
            
            // Wait for the server's reply
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            reply = reader.readLine();
        }
        catch (IOException e)
        {
            throw new SocialException("An error occurred while trying to post the content.");
        }
        finally
        {
            // Close the connection with the server
            try
            {
                if (sock != null) { sock.close(); }
            }
            catch (IOException e){ ; }
        }
        
        // Analyze the server's reply
        if (reply.equals("TOKEN_EXPIRED"))
        {
            throw new ExpiredTokenException();
        }
    }

    @Override
    public void followUser(String username)    throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        try
        {
            Registry registry = LocateRegistry.getRegistry(Constants.SERVER_RMI_REGISTRY_PORT);
            
            IFollowingService followingService = (IFollowingService) registry.lookup("FOLLOWING_SERVICE");
            followingService.startFollowing(token, username);            
        }
        catch (RemoteException | NotBoundException e)
        {
            throw new SocialException("An error occurred while trying to follow \"" + username + "\".");
        }
    }

    @Override
    public List<String> getContents() throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        // Contents received from the server are stored locally. If a user logs out before reading
        // them, such contents are stored to disk. Once read, a message is deleted.
        
        List<String> result = new ArrayList<String>(availableContents.size());
        result.addAll(availableContents);
        
        availableContents.clear();
        
        return result;
    }

    @Override
    public void logout() throws SocialException
    {
        // Check the validity of the token
        if (!isTokenValid())
        {
            throw new ExpiredTokenException();
        }
        
        // Socket for communicating with the server
        Socket sock = null;
        try
        {
            sock = new Socket(Constants.SERVER_NAME, Constants.SERVER_LOGOUT_PORT);
        }
        catch (IOException e)
        {
            throw new SocialException("Unable to connect to server.");
        }

        // Begin communicating with the server
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String reply = null;
        
        try
        {
            // Send the logout request to the server
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            writer.write("LOGOUT");
            writer.newLine();
            // Attach the token
            writer.write(token);
            writer.newLine();            
            
            writer.flush();
            
            // Wait for the server's reply
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            reply = reader.readLine();
        }
        catch (IOException e)
        {
            throw new SocialException("An error occurred while trying to log out.");
        }
        finally
        {
            // Close the connection with the server
            try
            {
                if (sock != null) { sock.close(); }
            }
            catch (IOException e){ ; }
        }
        
        // Analyze the server's reply
        if (reply.equals("TOKEN_EXPIRED"))
        {
            throw new ExpiredTokenException();
        }
        
        
        // Stop the threads that were started right after the login
        mcGroupMemberThread.interrupt();
        probeListenerThread.interrupt();
        requestsListenerThread.interrupt();
        
        // Save unread contents and pending friendship requests to disk
        try
        {
            ObjectOutputStream outStream = new ObjectOutputStream(
                new FileOutputStream(myUsername + ".pendingRequestsAndContents", false)
            );
            outStream.writeObject(pendingFriendshipRequests);
            outStream.writeObject(availableContents);
                        
            outStream.close();
        }
        catch (IOException e)
        {
            throw new SocialException("An error occurred while finalizing the logout procedure.");
        }
        
        myUsername = null;
        token = null;
        tokenReceptionDate = null;
    }
    
    private boolean isTokenValid()
    {
        if (token == null)
        {
            return false;
        }
        
        long offset = new Date().getTime() - tokenReceptionDate.getTime();
        return (offset / (1000 * 60 * 60)) < 24;
    }

}
