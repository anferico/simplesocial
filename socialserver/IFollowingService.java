package simplesocial.socialserver;

import simplesocial.socialclient.*;

import java.rmi.*;

public interface IFollowingService extends Remote
{
	public void registerCallback(String username, ISocialFollower stub)
			throws RemoteException;
	
	public void startFollowing(String yourToken, String theirUsername)
			throws RemoteException, SocialException;
}
