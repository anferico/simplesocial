package simplesocial.socialserver;

import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleSocialManager implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private static SimpleSocialManager instance = null;
    private List<SocialUser> users;
    private List<Friendship> estabilishedFriendships;
    private List<Friendship> pendingFriendships;
    private List<PendingContent> pendingContents; 
    
    private SimpleSocialManager()
    {
        restore();
    }
    
    // Return the only instance of this class (in accordance to the Singleton pattern)
    public static synchronized SimpleSocialManager getManager()
    {
        if (instance == null)
        {
            SimpleSocialManager.instance = new SimpleSocialManager();
        }
        
        return instance;
    }
    
    // Performs a backup of the social network
    public void backup()
    {                
        try
        {
            ObjectOutputStream outStream = new ObjectOutputStream(
                new FileOutputStream("networkState", false)
            );
            outStream.writeObject(this);
            
            outStream.close();
        }
        catch (Exception e)
        {
            System.out.println("Backup failed. Details: " + e.getMessage());
        }
    }
    
    // Restore the last state of the social network
    private void restore()
    {
        try
        {
            ObjectInputStream inStream = new ObjectInputStream(new FileInputStream("networkState"));
            SimpleSocialManager lastSnapshot = (SimpleSocialManager) inStream.readObject();
            
            inStream.close();
            
            // Restore network state
            users = lastSnapshot.users;
            estabilishedFriendships = lastSnapshot.estabilishedFriendships;
            pendingFriendships = lastSnapshot.pendingFriendships;
            pendingContents = lastSnapshot.pendingContents;
        }
        catch (Exception e)
        {
            // The state of the network had never been saved before
            
            users = Collections.synchronizedList(new ArrayList<SocialUser>());
            estabilishedFriendships = Collections.synchronizedList(new ArrayList<Friendship>());
            pendingFriendships = Collections.synchronizedList(new ArrayList<Friendship>());
            pendingContents = Collections.synchronizedList(new ArrayList<PendingContent>());
        }
    }
    
    // Add a new user to the list of registered user
    public void registerUser(String username, String password)
    {
        if (!userExists(username))
        {
            users.add(new SocialUser(username, password));
        }
    }
    
    // Retrieves the instance of 'SocialUser' that's associated with
    // the given username
    public SocialUser userFromUsername(String username)
    {
        for (SocialUser u : users)
        {
            if (u.getUsername().equals(username))
            {
                return u;
            }
        }
        
        return null;
    }
    
    // Retrieves the instance of 'SocialUser' that's associated with
    // the given token
    public SocialUser userFromToken(String token)
    {
        for (SocialUser u : users)
        {
            String aToken = u.getSessionToken();
            if (aToken != null)
            {    
                if (aToken.equals(token))
                {
                    return u;
                }
            }
        }
        
        return null;
    }
    
    // Tells whether a user with the given username is registered or not
    public boolean userExists(String username)
    {
        return userFromUsername(username) != null;
    }
    
    // Tells if the user associated with the given username is currently online
    public boolean isUserOnline(String username)
    {
        if (userExists(username))
        {
            return userFromUsername(username).isOnline();
        }
        else
        {
            return false;            
        }
    }
    
    // Tells if the user associated with the given username is registered
    // and has chosen the given password
    public boolean isValidPassword(String username, String password)
    {
        SocialUser u = userFromUsername(username);
        if (u != null)
        {
            return u.getPassword().equals(password);
        }
        else
        {
            return false;
        }
    }
    
    // Checks whether 'user1' and 'user2' are friends or not
    public boolean areFriends(String user1, String user2)
    {
        for (Friendship ef : estabilishedFriendships)
        {
            if (ef.involves(user1, user2))
            {
                return true;
            }
        }
        return false;
    }
    
    // Checks if 'who' sent a friendship request to 'toWhom'
    public boolean friendshipRequested(String who, String toWhom)
    {
        for (Friendship pf : pendingFriendships)
        {
            if (pf.getFirstUser().equals(who) && pf.getSecondUser().equals(toWhom))
            {
                return true;
            }
        }
        return false;
    }
    
    // Finalizes a pending friendship request based on the value of 'grant'. If 'grant' is true,
    // then 'addresser' and 'addressee' become friends, otherwise nothing changes
    public void finalizeFriendshipRequest(String addresser, String addressee, boolean grant)
    {            
        if (grant == true)
        {
            // Estabilish friendship
            estabilishedFriendships.add(new Friendship(addresser, addressee));            
        }
        
        // Remove the friendship request
        pendingFriendships.removeIf(pf ->
            pf.getFirstUser().equals(addresser) && pf.getSecondUser().equals(addressee)
        );
    }
    
    // Takes note of the friendship request sent from 'addresser' to 'addressee'
    public void registerFriendshipRequest(String addresser, String addressee)
    {    
        pendingFriendships.add(new Friendship(addresser, addressee));
    }
    
    // Returns the list of friends of a given user
    public List<SocialUser> getFriends(String username)
    {
        List<SocialUser> friends = new ArrayList<SocialUser>();
        
        for (Friendship ef : estabilishedFriendships)
        {
            if (ef.getFirstUser().equals(username))
            {
                friends.add(userFromUsername(ef.getSecondUser()));
            }
            else if (ef.getSecondUser().equals(username))
            {
                friends.add(userFromUsername(ef.getFirstUser()));
            }
        }
        
        return friends;
    }
    
    // Returns a list of all registered users, filtered by the given search key
    public List<SocialUser> getUsers(String filter)
    {
        List<SocialUser> filteredUsers = new ArrayList<SocialUser>();
        
        for (SocialUser su : users)
        {
            if (filter != null)
            {
                if (su.getUsername().contains(filter))
                {
                    filteredUsers.add(su);                    
                }
            }
            else
            {
                filteredUsers.add(su);
            }
        }
        
        return filteredUsers;
    }
    
    // Returns the list of online users
    public List<SocialUser> getOnlineUsers()
    {
        List<SocialUser> onlineUsers = new ArrayList<SocialUser>();
        for (SocialUser su : users)
        {
            if (su.isOnline())
            {
                onlineUsers.add(su);
            }
        }
        
        return onlineUsers;
    }
    
    // Stores some content that 'recipient' is interested in
    public void storeContent(String content, String recipient)
    {
        pendingContents.add(new PendingContent(content, recipient));
    }
    
    // Returns (and deletes) all the contents available for a given user to see
    public List<String> getPendingContents(String username)
    {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < pendingContents.size(); i++)
        {
            PendingContent content = pendingContents.get(i);
            if (content.getRecipient().equals(username))
            {
                // This content is for 'username'
                
                result.add(content.getContent());
                pendingContents.remove(i);
                i--;
            }
        }
        
        return result;
    }
    
    // Update the state of all users
    public void updateUsersState()
    {
        for (SocialUser user : users)
        {
            if (user.isOnline())
            {
                // User appears to be online, so I make sure that's
                // actually the case
                
                try
                {
                    Socket dummySock = new Socket();
                    dummySock.connect(user.getProbingSockAddress());
                    dummySock.close();                
                }
                catch (IOException e)
                {
                    // User is NOT online, so I set their state as offline
                    
                    user.setSessionToken(null);
                    user.setProbingSockAddress(null);
                    user.setFriendshipRequestsSockAddress(null);
                }
            }
        }
    }
    
    // Delete friendship requests that are older than 'expireTime'
    public void deleteExpiredFriendshipRequests(int expireTime)
    {
        for (int i = 0; i < pendingFriendships.size(); i++)
        {
            Friendship pf = pendingFriendships.get(i);
            
            // "Age" (in hours) of the friendship request
            long age = (new Date().getTime() - pf.getRequestDate().getTime()) / (1000 * 60 * 60);
            if (age >= expireTime)
            {
                pendingFriendships.remove(i);
                i--;
            }
        }
    }
    
}
