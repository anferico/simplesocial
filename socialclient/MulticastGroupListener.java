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
				
				// Join the multicast group
				mcSock.joinGroup(InetAddress.getByName(Constants.SERVER_MULTICAST_GROUP_ADDRESS));
				
				byte[] buf = new byte[512];
				DatagramPacket keepAlivePacket = new DatagramPacket(buf, buf.length); 
				// Receive the keep-alive message
				mcSock.receive(keepAlivePacket);
				
				DatagramSocket respSocket = new DatagramSocket();
				
				// Build a UDP datagram containing the username
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
				System.out.println("An error occurred in MulticastGroupListener.");
				e.printStackTrace();
			}
		}
	}

}
