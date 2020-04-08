package simplesocial.socialclient;

import java.io.*;
import java.net.*;

public class ProbeMessagesListener implements Runnable
{
	private ServerSocket welcomingSocket;
	
	public ProbeMessagesListener(ServerSocket welcomingSocket)
	{
		this.welcomingSocket = welcomingSocket;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
                // Set out to reply to probing requests from the server
                Socket dummySock = welcomingSocket.accept();
    
				dummySock.close();				
			}
			catch (IOException e)
			{
				System.out.println("An error occurred in ProbeMessagesListener.");
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
