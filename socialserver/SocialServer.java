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
    // Number of hours after which a pending friendship
    // request is automatically deleted
    private static int UNCONFIRMED_FSREQUESTS_LIFETIME = 72;
    
    // Keeps track of all the users and their friends
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
                    System.out.println("Argument must be a positive integer number.");
                    System.exit(0);
                }
                
                SocialServer.UNCONFIRMED_FSREQUESTS_LIFETIME = fsReqLife;
            }
            catch (NumberFormatException e)
            {
                System.out.println("Argument must be an integer number.");
                System.exit(0);
            }
        }    
        
        // Channels where we listen for a variety of requests
        ServerSocketChannel regChannel = null;   // For 'registration' requests
        ServerSocketChannel lginChannel = null;  // For 'login' requests
        ServerSocketChannel fsReqChannel = null; // For 'send friendship' requests
        ServerSocketChannel fsGrtChannel = null; // For 'accept/deny friendship' requests
        ServerSocketChannel frndsChannel = null; // For 'display list of friends' requests
        ServerSocketChannel usrsChannel = null;  // For 'search for a user' requests
        ServerSocketChannel cntChannel = null;   // For 'post something' requests
        ServerSocketChannel lgoutChannel = null; // For 'logout' requests
        
        // Thread pool to which tasks related to the different types of requests are submitted
        ThreadPoolExecutor pool = null;
        
        // Multicast socket where we'll be sending keep-alive messages
        MulticastSocket mcGroup = null;
        
        // Channel where I'll be receiving replies to keep-alive messages
        DatagramChannel keepAliveRespChannel = null;
        
        try
        {
            // Export the service that the client will use for:
            // - registering a callback
            // - following a user
            LocateRegistry.createRegistry(Constants.SERVER_RMI_REGISTRY_PORT);
            Registry registry = LocateRegistry.getRegistry(Constants.SERVER_RMI_REGISTRY_PORT);
            
            FollowingService service = new FollowingService();
            IFollowingService stub = (IFollowingService) UnicastRemoteObject.exportObject(
                service, 
                Constants.SERVER_RMI_REGISTRY_PORT
            );
            
            registry.rebind("FOLLOWING_SERVICE", stub);
        }
        catch (RemoteException f)
        {
            System.out.println("An error occurred while exporting the service.");
        }
        
        try
        {        
            // Open selector
            Selector sel = Selector.open();
            
            // Open and register the channels where I'll be listening for the 
            // different requests
            
            regChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);
            regChannel.register(sel, SelectionKey.OP_ACCEPT);
            regChannel.socket().bind(
                new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_REG_PORT)
            );
            
            lginChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);
            lginChannel.register(sel, SelectionKey.OP_ACCEPT);
            lginChannel.socket().bind(
                new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_LOGIN_PORT)
            );
            
            fsReqChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);
            fsReqChannel.register(sel, SelectionKey.OP_ACCEPT);
            fsReqChannel.socket().bind(
                new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_FSREQUEST_PORT)
            );                        
            
            fsGrtChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);    
            fsGrtChannel.register(sel, SelectionKey.OP_ACCEPT);
            fsGrtChannel.socket().bind(
                new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_FSGRANT_PORT)
            );    
            
            frndsChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);        
            frndsChannel.register(sel, SelectionKey.OP_ACCEPT);
            frndsChannel.socket().bind(
                new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_FRIENDS_PORT)
            );    

            usrsChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);
            usrsChannel.register(sel, SelectionKey.OP_ACCEPT);
            usrsChannel.socket().bind(
                new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_USERS_PORT)
            );    

            cntChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);
            cntChannel.register(sel, SelectionKey.OP_ACCEPT);
            cntChannel.socket().bind(
                new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_CONTENT_PORT)
            );    

            lgoutChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);    
            lgoutChannel.register(sel, SelectionKey.OP_ACCEPT);
            lgoutChannel.socket().bind(
                new InetSocketAddress(Constants.SERVER_NAME, Constants.SERVER_LOGOUT_PORT)
            );    
            
            keepAliveRespChannel = (DatagramChannel) DatagramChannel.open().configureBlocking(false);
            keepAliveRespChannel.register(sel, SelectionKey.OP_READ);
            keepAliveRespChannel.bind(
                new InetSocketAddress(
                    Constants.SERVER_NAME,
                    Constants.SERVER_KEEPALIVE_RESPONSES_PORT
                )
            );
            
            mcGroup = new MulticastSocket();
            
            // Last time I sent a keep-alive message
            long lastKeepAliveDispatch = new Date().getTime();
            // Users who haven't replied to the last keep-alive message
            List<SocialUser> inactiveUsers = Collections.emptyList();            
            
            pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            
            while (true)
            {
                
                // Delete "expired" friendship requests
                ssManager.deleteExpiredFriendshipRequests(UNCONFIRMED_FSREQUESTS_LIFETIME);
                
                // Check if there are connection requests
                sel.selectNow();
                Set<SelectionKey> keys = sel.selectedKeys();
                Iterator<SelectionKey> keysIterator = keys.iterator();
                
                while (keysIterator.hasNext())
                {
                    SelectionKey key = (SelectionKey) keysIterator.next();                    
                    keysIterator.remove();
                    
                    if (key.isAcceptable())
                    {
                        // Before serving any request, update the state of each user
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
                        
                        // Submit the task that'll handle the requested operation
                        pool.execute(task);                        
                    }
                    else if (key.isReadable())
                    {                
                        // A user has replied to my keep-alive message

                        DatagramChannel channel = (DatagramChannel) key.channel();
                        ByteBuffer bBuf = ByteBuffer.allocate(1024);
                        bBuf.clear();                        
                        // Read the content of the received datagram                        
                        channel.receive(bBuf);
                        
                        byte[] receiveBuf = new byte[1024];
                        bBuf.flip();    
                        for (int i = 0; bBuf.hasRemaining(); i++)
                        {
                            receiveBuf[i] = bBuf.get();
                        }
                        
                        
                        // The username contained in the received datagram (as per protocol)
                        String username = new String(receiveBuf).trim();
                        
                        // Remove the user from the list of inactive users
                        SocialUser activeUser = ssManager.userFromUsername(username);
                        inactiveUsers.remove(activeUser);                        
                    }
                    
                }
                
                // If at least 10 seconds have elapsed since the last time
                // I sent a keep-alive message, send a new one
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
                    
                    // "Expel" all the users who haven't replied to the keep-alive message
                    for (SocialUser inactiveUser : inactiveUsers)
                    {
                        inactiveUser.setSessionToken(null);
                        inactiveUser.setProbingSockAddress(null);
                        inactiveUser.setFriendshipRequestsSockAddress(null);
                    }
                    
                    lastKeepAliveDispatch = new Date().getTime();                    
                    inactiveUsers = ssManager.getOnlineUsers();
                    System.out.println("Number of online users: " + inactiveUsers.size());
                    System.out.flush();
                    
                    // Perform a backup of the social network
                    ssManager.backup();
                }
            }
            
        }
        catch (IOException e)
        {
            System.out.println("An error occurred. Details: " + e.getMessage());
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
