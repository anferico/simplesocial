package simplesocial.socialclient;

import java.io.*;
import java.net.*;
import java.util.*;

public class FriendshipRequestsListener implements Runnable
{
	private ServerSocket welcomingSocket;
	private List<String> pendingFriendshipRequests;
	
	public FriendshipRequestsListener(ServerSocket welcomingSocket, List<String> pendingFriendshipRequests)
	{
		this.welcomingSocket = welcomingSocket;
		this.pendingFriendshipRequests = pendingFriendshipRequests;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				Socket communicationSocket = welcomingSocket.accept();
				
				BufferedReader reader = new BufferedReader(
					new InputStreamReader(
                        communicationSocket.getInputStream()
                    )
                );
				
				// Read the username of the user who sent a friendship request
				String potentialFriend = reader.readLine();
				pendingFriendshipRequests.add(potentialFriend);
				
				communicationSocket.close();
			}
			catch (IOException e)
			{
				System.out.println("An error occurred in FriendshipRequestsListener.");
				e.printStackTrace();
				
				try
				{
					if (welcomingSocket != null) { welcomingSocket.close(); }
				}
				catch (IOException f) { ; }
			}
		}
	}

}
