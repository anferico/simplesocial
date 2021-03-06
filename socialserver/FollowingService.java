package simplesocial.socialserver;

import java.rmi.*;
import java.rmi.server.*;
import simplesocial.socialclient.*;

public class FollowingService extends RemoteServer implements IFollowingService
{
    private static final long serialVersionUID = 1L;
    private SimpleSocialManager manager; 
    
    public FollowingService()
    {
        this.manager = SimpleSocialManager.getManager();
    }
    
    @Override
    public void registerCallback(String username, ISocialFollower stub)
            throws RemoteException
    {
        SocialUser user = manager.userFromUsername(username);
        if (user != null)
        {
            user.setStub(stub);
            
            // Retrieve available contents for 'username'
            for (String content : manager.getPendingContents(username))
            {
                user.getStub().onNewContentPublished(content);
            }
        }                
    }
    
    @Override
    public void startFollowing(String yourToken, String theirUsername)
            throws RemoteException, SocialException
    {
        SocialUser interestedUser = manager.userFromToken(yourToken);
        SocialUser interestingUser = manager.userFromUsername(theirUsername);
        if (interestedUser == null)
        {
            throw new ExpiredTokenException();
        }
        else if (interestingUser == null)
        {
            throw new SocialException("You can't follow an unregistered user");
        }
        else
        {                        
            interestingUser.addFollower(interestedUser.getUsername());
        }
    }

}
