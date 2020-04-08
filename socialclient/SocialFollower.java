package simplesocial.socialclient;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class SocialFollower extends UnicastRemoteObject implements ISocialFollower
{
	private static final long serialVersionUID = 1L;
	private List<String> contents;
	
	protected SocialFollower(List<String> contents)	throws RemoteException
	{
		super();
		this.contents = contents;
	}

	@Override
	public void onNewContentPublished(String newContent) throws RemoteException
	{
		contents.add(newContent);
	}

}
