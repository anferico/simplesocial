package simplesocial.socialserver;

import java.io.*;
import java.net.*;
import java.util.*;

import simplesocial.socialclient.*;

public class SocialUser implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String password;
    private List<String> followers = Collections.synchronizedList(new ArrayList<String>());
    private transient String sessionToken = null;
    private transient ISocialFollower stub = null;    
    // Socket address where the user responds to TCP connection requests aimed 
    // at probing its state (online or offline)
    private transient InetSocketAddress probingSockAddress = null;
    // Socket address where the user responds to TCP connection requests aimed 
    // at sending the name of a user who just sent a friendship request
    private transient InetSocketAddress friendshipRequestsSockAddress = null;
    
    public SocialUser(String username, String password)
    {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername()
    {
        return this.username;
    }
    
    public String getPassword()
    {
        return this.password;
    }
    
    public List<String> getFollowers()
    {
        return this.followers;
    }
    
    public boolean isOnline()
    {
        return this.sessionToken != null;
    }

    public InetSocketAddress getProbingSockAddress()
    {
        return this.probingSockAddress;
    }

    public void setProbingSockAddress(InetSocketAddress probingSockAddress)
    {
        this.probingSockAddress = probingSockAddress;
    }

    public InetSocketAddress getFriendshipRequestsSockAddress()
    {
        return this.friendshipRequestsSockAddress;
    }

    public void setFriendshipRequestsSockAddress(InetSocketAddress friendshipRequestsSockAddress)
    {
        this.friendshipRequestsSockAddress = friendshipRequestsSockAddress;
    }

    public String getSessionToken()
    {
        return this.sessionToken;
    }
    
    public void setSessionToken(String token)
    {
        this.sessionToken = token;
    }
    
    public ISocialFollower getStub()
    {
        return this.stub;
    }
    
    public void setStub(ISocialFollower stub)
    {
        this.stub = stub;
    }
    
    public void addFollower(String follower)
    {
        if (!followers.contains(follower))
        {
            followers.add(follower);
        }
    }
    
    @Override
    public boolean equals(Object obj)
    {
        SocialUser that = (SocialUser) obj;
        if (that == null)
        {
            return false;
        }
        return this.getUsername().equals(that.getUsername());        
    }
}
