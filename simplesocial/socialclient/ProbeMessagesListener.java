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
				// Accolgo richieste di connessione dal server, per dimostrare
				// che sono online				
				Socket dummySock = welcomingSocket.accept();
				dummySock.close();				
			}
			catch (IOException e)
			{
				System.out.println("Si è verificato un errore in ProbeMessagesListener.");
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
