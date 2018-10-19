package simplesocial.socialclient;

import java.io.*;
import java.net.*;
import simplesocial.*;

public class MulticastGroupListener implements Runnable
{
	private String username;
	
	public MulticastGroupListener(String username)
	{
		this.username = username;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				MulticastSocket mcSock = new MulticastSocket(Constants.SERVER_MULTICAST_GROUP_PORT);
				
				// Entro a far parte del gruppo di multicast
				mcSock.joinGroup(InetAddress.getByName(Constants.SERVER_MULTICAST_GROUP_ADDRESS));
				
				
				byte[] buf = new byte[512];
				DatagramPacket keepAlivePacket = new DatagramPacket(buf, buf.length); 
				// Ricevo il messaggio di keep-alive
				mcSock.receive(keepAlivePacket);
				
				DatagramSocket respSocket = new DatagramSocket();
				
				// Costruisco un nuovo datagram UDP con l'username dell'utente
				byte[] usernameBuf = username.getBytes();
				DatagramPacket respPacket = new DatagramPacket(usernameBuf, usernameBuf.length);
				
				respPacket.setAddress(InetAddress.getByName(Constants.SERVER_NAME));
				respPacket.setPort(Constants.SERVER_KEEPALIVE_RESPONSES_PORT);
				respSocket.send(respPacket);
				
				respSocket.close();
				mcSock.close();
			}
			catch (IOException e)
			{
				System.out.println("Si è verificato un errore in MulticastGroupListener.");
				e.printStackTrace();
			}
		}
	}

}
