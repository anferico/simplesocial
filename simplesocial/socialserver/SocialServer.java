package simplesocial.socialserver;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.*;

import simplesocial.*;
import simplesocial.socialserver.suppliers.*;

public class SocialServer
{
	// Indica il numero di ore che possono trascorrere prima che 
	// una richiesta di amicizia non riscontrata venga eliminata
	private static int UNCONFIRMED_FSREQUESTS_LIFETIME = 72;
	
	// Registra gli utenti e i legami di amicizia che hanno stabilito, inoltre
	// contiene una varietà di metodi di utilità
	private static SimpleSocialManager ssManager = SimpleSocialManager.getManager();	
	
	public static void main(String[] args)
	{
		if (args.length > 0)
		{
			try
			{				
				int fsReqLife = Integer.parseInt(args[0]);
				if (fsReqLife <= 0)
				{
					System.out.println("L'argomento deve essere un numero intero positivo.");
					System.exit(0);
				}
				
				SocialServer.UNCONFIRMED_FSREQUESTS_LIFETIME = fsReqLife;
			}
			catch (NumberFormatException e)
			{
				System.out.println("L'argomento deve essere un numero intero.");
				System.exit(0);
			}
		}	
		
		// I vari canali sui quali accoglierò le diverse operazioni
		ServerSocketChannel regChannel = null;   // Per le richieste di 'registrazione'
		ServerSocketChannel lginChannel = null;  // Per le richieste di 'login'
		ServerSocketChannel fsReqChannel = null; // Per le richieste di 'invio di una nuova richiesta di amicizia'
		ServerSocketChannel fsGrtChannel = null; // Per le richieste di 'risposta a una richiesta di amicizia'
		ServerSocketChannel frndsChannel = null; // Per le richieste di 'ottieni lista amici'
		ServerSocketChannel usrsChannel = null;  // Per le richieste di 'ottieni lista utenti'
		ServerSocketChannel cntChannel = null;   // Per le richieste di 'pubblicazione nuovo contenuto'
		ServerSocketChannel lgoutChannel = null; // Per le richieste di 'logout'
		
		// Il thread pool al quale sottometto i task relativi alle diverse operazioni
		ThreadPoolExecutor pool = null;
		
		// La socket multicast su cui spedirò i keep-alive		
		MulticastSocket mcGroup = null;
		
		// Il canale su cui ricevo le risposte ai keep-alive
		DatagramChannel keepAliveRespChannel = null;
		
		try
		{
			// Esporto il servizio che userà il client per:
			// - registrare una callback
			// - registrare il proprio interesse per un utente
			LocateRegistry.createRegistry(Constants.SERVER_RMI_REGISTRY_PORT);
			Registry registry = LocateRegistry.getRegistry(Constants.SERVER_RMI_REGISTRY_PORT);
			
			FollowingService service = new FollowingService();
			IFollowingService stub = (IFollowingService) UnicastRemoteObject.exportObject(service, Constants.SERVER_RMI_REGISTRY_PORT);
			
			registry.rebind("FOLLOWING_SERVICE", stub);
		}
		catch (RemoteException f)
		{
			System.out.println("Si è verificato un errore nell'esportazione del servizio.");
		}
		
		try
		{		
			// Apro il selettore
			Selector sel = Selector.open();
			
			// Apro e registro i vari canali sui quali accoglierò le richieste di operazioni
			
			regChannel = (ServerSocketChannel) ServerSocketChannel.open()
					.configureBlocking(false);
			regChannel.register(sel, SelectionKey.OP_ACCEPT);
			regChannel.socket().bind(new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_REG_PORT));
			
			lginChannel = (ServerSocketChannel) ServerSocketChannel.open()
					.configureBlocking(false);
			lginChannel.register(sel, SelectionKey.OP_ACCEPT);
			lginChannel.socket().bind(new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_LOGIN_PORT));
			
			fsReqChannel = (ServerSocketChannel) ServerSocketChannel.open()
					.configureBlocking(false);
			fsReqChannel.register(sel, SelectionKey.OP_ACCEPT);
			fsReqChannel.socket().bind(new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_FSREQUEST_PORT));						
			
			fsGrtChannel = (ServerSocketChannel) ServerSocketChannel.open()
					.configureBlocking(false);	
			fsGrtChannel.register(sel, SelectionKey.OP_ACCEPT);
			fsGrtChannel.socket().bind(new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_FSGRANT_PORT));	
			
			frndsChannel = (ServerSocketChannel) ServerSocketChannel.open()
					.configureBlocking(false);		
			frndsChannel.register(sel, SelectionKey.OP_ACCEPT);
			frndsChannel.socket().bind(new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_FRIENDS_PORT));	

			usrsChannel = (ServerSocketChannel) ServerSocketChannel.open()
					.configureBlocking(false);
			usrsChannel.register(sel, SelectionKey.OP_ACCEPT);
			usrsChannel.socket().bind(new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_USERS_PORT));	

			cntChannel = (ServerSocketChannel) ServerSocketChannel.open()
					.configureBlocking(false);
			cntChannel.register(sel, SelectionKey.OP_ACCEPT);
			cntChannel.socket().bind(new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_CONTENT_PORT));	

			lgoutChannel = (ServerSocketChannel) ServerSocketChannel.open()
					.configureBlocking(false);	
			lgoutChannel.register(sel, SelectionKey.OP_ACCEPT);
			lgoutChannel.socket().bind(new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_LOGOUT_PORT));	
			
			keepAliveRespChannel = (DatagramChannel) DatagramChannel.open()
					.configureBlocking(false);
			keepAliveRespChannel.register(sel, SelectionKey.OP_READ);
			keepAliveRespChannel.bind(
				new InetSocketAddress(
					Constants.SERVER_NAME,
					Constants.SERVER_KEEPALIVE_RESPONSES_PORT
				)
			);
			
			mcGroup = new MulticastSocket();
			
			// L'ultimo istante in cui ho spedito un messaggio di keep-alive
			long lastKeepAliveDispatch = new Date().getTime();
			// Gli utenti che non hanno ancora risposto all'ultimo messaggio
			// di keep-alive spedito
			List<SocialUser> inactiveUsers = Collections.emptyList();			
			
			pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
			
			while (true)
			{
				
				// Elimino le richieste di amicizia scadute
				ssManager.deleteExpiredFriendshipRequests(UNCONFIRMED_FSREQUESTS_LIFETIME);
				
				// Verifico se esistono richieste di connessione
				sel.selectNow();
				Set<SelectionKey> keys = sel.selectedKeys();
				Iterator<SelectionKey> keysIterator = keys.iterator();
				
				while (keysIterator.hasNext())
				{
					SelectionKey key = (SelectionKey) keysIterator.next();					
					keysIterator.remove();
					
					if (key.isAcceptable())
					{

						// Prima di servire qualsiasi richiesta, aggiorno lo stato 
						// di tutti gli utenti
						ssManager.updateUsersState();
						
						ServerSocketChannel channel = (ServerSocketChannel) key.channel();
						
						Socket communicationSocket = channel.accept().socket();
						int boundedPort = communicationSocket.getLocalPort();
											
						Runnable task = null;
						
						switch (boundedPort)
						{
							case Constants.SERVER_REG_PORT:
								task = new RegistrationSupplier(communicationSocket);
								break;
								
							case Constants.SERVER_LOGIN_PORT:
								task = new LoginSupplier(communicationSocket);
								break;
								
							case Constants.SERVER_FSREQUEST_PORT:								
								task = new FriendshipRequestSupplier(communicationSocket);
								break;
								
							case Constants.SERVER_FSGRANT_PORT:
								task = new FriendshipGrantSupplier(communicationSocket);
								break;
								
							case Constants.SERVER_FRIENDS_PORT:
								task = new FriendsListSupplier(communicationSocket);
								break;
								
							case Constants.SERVER_USERS_PORT:
								task = new UsersSearchSupplier(communicationSocket);
								break;
								
							case Constants.SERVER_CONTENT_PORT:
								task = new PostingSupplier(communicationSocket);
								break;
								
							case Constants.SERVER_LOGOUT_PORT:
								task = new LogoutSupplier(communicationSocket);
								break;								
						}
						
						// Sottometto il task che gestirà l'operazione richiesta
						pool.execute(task);						
					}
					else if (key.isReadable())
					{				
						// Qualcuno ha risposto al keep-alive, quindi lo rimuovo
						// dagli utenti che considero inattivi
						
						DatagramChannel channel = (DatagramChannel) key.channel();
						ByteBuffer bBuf = ByteBuffer.allocate(1024);
						bBuf.clear();						
						// Leggo il contenuto del datagram spedito						
						channel.receive(bBuf);
						
						byte[] receiveBuf = new byte[1024];
						bBuf.flip();	
						for (int i = 0; bBuf.hasRemaining(); i++)
						{
							receiveBuf[i] = bBuf.get();
						}
						
						
						// L'username contenuto nel datagram spedito (come da protocollo)
						String username = new String(receiveBuf).trim();
						
						// Tolgo l'utente dalla lista di quelli risultanti inattivi
						SocialUser activeUser = ssManager.userFromUsername(username);
						inactiveUsers.remove(activeUser);						
					}
					
				}
				
				// Se sono passati 10 secondi dall'ultima spedizione, 
				// invio un nuovo messaggio di keep-alive
				long elapsedSeconds = (new Date().getTime() - lastKeepAliveDispatch) / 1000;
				if (elapsedSeconds >= 10)
				{
					byte[] buf = "KEEP_ALIVE".getBytes();
					DatagramPacket keepAlivePacket = new DatagramPacket(buf, buf.length);
					keepAlivePacket.setSocketAddress(
						new InetSocketAddress(
							Constants.SERVER_MULTICAST_GROUP_ADDRESS, 
							Constants.SERVER_MULTICAST_GROUP_PORT
						)
					);
					mcGroup.send(keepAlivePacket);
					
					// "Sbatto fuori" tutti gli utenti che non hanno risposto
					// all'ultimo keep-alive
					for (SocialUser inactiveUser : inactiveUsers)
					{
						inactiveUser.setSessionToken(null);
						inactiveUser.setProbingSockAddress(null);
						inactiveUser.setFriendshipRequestsSockAddress(null);
					}
					
					lastKeepAliveDispatch = new Date().getTime();					
					inactiveUsers = ssManager.getOnlineUsers();
					System.out.println("Numero di utenti rimasti online: " + inactiveUsers.size());
					System.out.flush();
					
					// Faccio anche un backup della rete sociale
					ssManager.backup();
				}
			}
			
		}
		catch (IOException e)
		{
			System.out.println("Si è verificato un errore. Dettagli: " + e.getMessage());
			System.exit(0);
		}
		finally
		{
			try
			{				
				if (regChannel != null) { regChannel.close(); }
				if (lginChannel != null) { lginChannel.close(); }
				if (fsReqChannel != null) { fsReqChannel.close(); }
				if (fsGrtChannel != null) { fsGrtChannel.close(); }
				if (frndsChannel != null) { frndsChannel.close(); }
				if (usrsChannel != null) { usrsChannel.close(); }
				if (cntChannel != null) { cntChannel.close(); }
				if (lgoutChannel != null) { lgoutChannel.close(); }
				if (keepAliveRespChannel != null) { keepAliveRespChannel.close(); }
				if (pool != null) { pool.shutdown(); }
				if (mcGroup != null) { mcGroup.close(); }
			}
			catch (IOException e) { ; }
		}
	}

}
