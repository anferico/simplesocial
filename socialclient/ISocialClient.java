package simplesocial.socialclient;

import java.util.*;

public interface ISocialClient
{
    public void register(String username, String password) throws SocialException;
    
    public void login(String username, String password)    throws SocialException;
    
    public void sendFriendshipRequest(String username) throws SocialException;
    
    public List<String> getPendingFriendshipRequests() throws SocialException;
    
    public void respondToFriendshipRequest(String username, boolean choice)    throws SocialException;
    
    public List<Friend> getFriends() throws SocialException;
    
    public String[] findUsers(String searchKey) throws SocialException;
    
    public void publishContent(String content) throws SocialException;
    
    public void followUser(String username)    throws SocialException;
    
    public List<String> getContents() throws SocialException;
    
    public void logout() throws SocialException;
}
