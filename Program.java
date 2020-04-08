package simplesocial;

import java.io.*;
import java.util.Iterator;

import simplesocial.socialclient.*;

public class Program
{
    public static void main(String[] args) throws IOException
    {
        // Client of the Simple Social application
        ISocialClient socialClient = new SocialClient();
        
        BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
        
        boolean isLoggedIn = false;
        while (true)
        {    
            showMenu(isLoggedIn);
            
            try
            {
                String op = inReader.readLine();
                
                if (isLoggedIn)
                {
                    switch (op)
                    {
                        case "1": 
                            // User wants to send a new friendship request
                            
                            System.out.println("Enter the username of the user you want to send a friendship request to:");
                            String frUsername = inReader.readLine();

                            socialClient.sendFriendshipRequest(frUsername);
                            System.out.println("Friendship request has been sent");
                            break;
                            
                        case "2": 
                            // User wants to accept/deny a friendship request
                            
                            System.out.println("Enter the username of a user who sent you a friendship request:");
                            String frrUsername = inReader.readLine();
                            System.out.println("Do you want to accept the friendship request? [Y/N]:");
                            boolean choice = false, unexpectedChoice = false;
                            do
                            {
                                String choiceString = inReader.readLine();
                                switch (choiceString)
                                {
                                    case "Y":
                                    case "y":
                                        choice = true;
                                        unexpectedChoice = false;
                                        break;
                                    case "N":
                                    case "n":
                                        choice = false;
                                        unexpectedChoice = false;
                                        break;
                                    default:
                                        System.out.println("Enter either \"Y\" or \"N\"");
                                        unexpectedChoice = true;
                                        break;
                                }
                            } while (unexpectedChoice);
                            
                            socialClient.respondToFriendshipRequest(frrUsername, choice);
                            if (choice == true)
                            {
                                System.out.println(frrUsername + " is now your friend");
                            }
                            else
                            {
                                System.out.println("You've denied a friendship request from " + frrUsername);
                            }
                            break;
                            
                        case "3": 
                            // User wants a list of their friends
                            
                            Iterator<Friend> frIter = socialClient.getFriends().iterator();
                            if (frIter.hasNext())
                            {                                                    
                                while (frIter.hasNext())
                                {
                                    Friend f = frIter.next();
                                    String state = f.isOnline() ? "[online] " : "[offline] ";
                                    System.out.println(state + f.getUsername());
                                    System.out.flush();
                                }
                            }
                            else
                            {
                                System.out.println("You don't have any friends yet");
                            }
                            break;
                            
                        case "4": 
                            // User wants to search for a user
                            
                            System.out.println("Enter a search key (leave empty to list all users):");
                            String searchKey = inReader.readLine();
                            if (searchKey.equals("\n"))
                            {
                                searchKey = "<nofilter>";
                            }

                            String[] users = socialClient.findUsers(searchKey);
                            if (users.length > 0)
                            {                                                    
                                for (String usr : users)
                                {
                                    System.out.println(usr);
                                }
                            }
                            else
                            {
                                System.out.println("No results found");
                            }
                            break;
                            
                        case "5": 
                            // User wants to publish something
                            
                            System.out.println("Enter what you want to post:");
                            String content = inReader.readLine();
                            
                            socialClient.publishContent(content);
                            System.out.println("Post was successful");
                            break;
                            
                        case "6": 
                            // User wants to follow a user
                            
                            System.out.println("Enter the username of a user you want to follow:");
                            String follUsername = inReader.readLine();
                            
                            socialClient.followUser(follUsername);
                            System.out.println("You're now following " + follUsername);
                            break;
                            
                        case "7": 
                            // User wants to display pending friendship requests
                            
                            Iterator<String> reqIter = socialClient.getPendingFriendshipRequests().iterator();
                            if (reqIter.hasNext())
                            {                                                    
                                while (reqIter.hasNext())
                                {
                                    String r = reqIter.next();
                                    System.out.println(r);
                                }
                            }
                            else
                            {
                                System.out.println("You have no pending friendship request");
                            }
                            break;
                            
                        case "8": 
                            // User wants to display new contents
                            
                            Iterator<String> contIter = socialClient.getContents().iterator();
                            if (contIter.hasNext())
                            {                                                    
                                while (contIter.hasNext())
                                {
                                    String c = contIter.next();
                                    System.out.println(c);
                                }
                            }
                            else
                            {
                                System.out.println("Nothing to show");
                            }    
                            break;
                            
                        case "9": 
                            // User wants to log out
                            
                            socialClient.logout();
                            System.out.println("Logged out");
                            isLoggedIn = false;
                            break;
                            
                        case "10": 
                            // User wants to exit the program
                            
                            System.exit(0);
                            break;
                            
                        default: 
                            
                            System.out.println("Unexpected choice");
                            break;
                    }
                }
                else
                {
                    switch (op)
                    {
                        case "1": 
                            // User wants to register
                            
                            System.out.println("Choose a username:");
                            String regUsername = inReader.readLine();
                            System.out.println("Choose a password:");
                            String regPassword = inReader.readLine();

                            socialClient.register(regUsername, regPassword);
                            System.out.println("Registration succeded");
                            break;
                            
                        case "2": 
                            // User wants to log in
                            
                            System.out.println("Enter username:");
                            String logUsername = inReader.readLine();
                            System.out.println("Enter password:");
                            String logPassword = inReader.readLine();

                            socialClient.login(logUsername, logPassword);
                            System.out.println("Logged in");
                            isLoggedIn = true;
                            break;
                            
                        case "3": 
                            // User wants to exit the program
                            
                            System.exit(0);
                            break;
                            
                        default: 
                            
                            System.out.println("Unexpected choice");
                            break;
                    }
                }
            }
            catch (SocialException se)
            {
                if (se instanceof ExpiredTokenException)
                {
                    isLoggedIn = false;
                }
                
                System.out.println(se.getMessage());
            }

        }
    }

    private static void showMenu(boolean loggedIn)
    {
        if (loggedIn)
        {
            System.out.println();
            System.out.println("=============================================");
            System.out.println("============== [Simple Social] ==============");
            System.out.println("=============================================");
            System.out.println("[1]  Send a friendship request");
            System.out.println("[2]  Accept/deny a friendship request");
            System.out.println("[3]  Display a list of your friends");
            System.out.println("[4]  Search for a user");
            System.out.println("[5]  Post something");
            System.out.println("[6]  Follow a user");
            System.out.println("[7]  Display friendship requests");
            System.out.println("[8]  Show new contents");
            System.out.println("[9]  Logout");
            System.out.println("[10] Quit");
            System.out.println("=============================================");
        }
        else
        {
            System.out.println();
            System.out.println("=============================================");
            System.out.println("============== [Simple Social] ==============");
            System.out.println("=============================================");
            System.out.println("[1]  Register");
            System.out.println("[2]  Login");
            System.out.println("[3]  Quit");
            System.out.println("=============================================");
        }
    }
}
