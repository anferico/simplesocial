package simplesocial.socialclient;

import java.rmi.*;

public interface ISocialFollower extends Remote
{
    public void onNewContentPublished(String newContent) throws RemoteException;
}
