package simplesocial;

import java.io.*;
import java.util.Iterator;

import simplesocial.socialclient.*;

public class Program
{
	public static void main(String[] args) throws IOException
	{
		// Client dell'applicazione Simple Social
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
    						// Richiesto invio nuova amicizia
    						
    						System.out.println("Immetti l'username dell'utente a cui spedire la richiesta:");
    						String frUsername = inReader.readLine();

    						socialClient.sendFriendshipRequest(frUsername);
    						System.out.println("Richiesta di amicizia inoltrata");
    						break;
    						
    					case "2": 
    						// Richiesta conferma/rifiuto di una richiesta di amicizia
    						
    						System.out.println("Immetti l'username di un utente che ti ha spedito una richiesta:");
    						String frrUsername = inReader.readLine();
    						System.out.println("Vuoi accettare la richiesta di amicizia? [S/N]:");
    						boolean choice = false, unexpectedChoice= false;
    						do
    						{
        						String choiceString = inReader.readLine();
        						switch (choiceString)
        						{
        							case "S":
        							case "s":
        								choice = true;
        								unexpectedChoice = false;
        								break;
        							case "N":
        							case "n":
        								choice = false;
        								unexpectedChoice = false;
        								break;
        							default:
        								System.out.println("Digita \"S\" o \"N\"");
        								unexpectedChoice = true;
        								break;
        						}
    						} while (unexpectedChoice);
    						
    						socialClient.respondToFriendshipRequest(frrUsername, choice);
    						if (choice == true)
    						{
    							System.out.println("Hai accordato l'amicizia a " + frrUsername);
    						}
    						else
    						{
    							System.out.println("Hai negato l'amicizia a " + frrUsername);
    						}
    						break;
    						
    					case "3": 
    						// Richiesta lista amici
    						
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
    							System.out.println("Attualmente non hai amici");
    						}
    						break;
    						
    					case "4": 
    						// Richiesta lista utenti
    						
    						System.out.println("Immetti una chiave di ricerca, oppure lascia vuoto:");
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
    							System.out.println("La ricerca non ha prodotto risultati");
    						}
    						break;
    						
    					case "5": 
    						// Richiesta pubblicazione contenuto
    						
    						System.out.println("Immetti ciò che vuoi pubblicare:");
    						String content = inReader.readLine();
    						
    						socialClient.publishContent(content);
    						System.out.println("Contenuto pubblicato con successo");
    						break;
    						
    					case "6": 
    						// Richiesta registrazione interesse per un utente
    						
    						System.out.println("Immetti l'username di un utente che vuoi seguire:");
    						String follUsername = inReader.readLine();
    						
    						socialClient.followUser(follUsername);
    						System.out.println("Stai seguendo " + follUsername);
    						break;
    						
    					case "7": 
    						// Richiesta lista delle richieste di amicizia in sospeso
    						
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
    							System.out.println("Non hai alcuna richiesta di amicizia in sospeso");
    						}
    						break;
    						
    					case "8": 
    						// Richiesta lista contenuti in sospeso
    						
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
    							System.out.println("Non hai alcun contenuto da visualizzare");
    						}	
    						break;
    						
    					case "9": 
    						// Richiesto logout
    						
    						socialClient.logout();
    						System.out.println("Logout effettuato");
    						isLoggedIn = false;
    						break;
    						
    					case "10": 
    						// Richiesta l'uscita dal programma
    						
    						System.exit(0);
    						break;
    						
    					default: 
    						
    						System.out.println("Scelta non prevista");
    						break;
    				}
    			}
    			else
    			{
    				switch (op)
    				{
    					case "1": 
    						// Richiesta registrazione
    						
    						System.out.println("Immetti l'username:");
    						String regUsername = inReader.readLine();
    						System.out.println("Immetti la password:");
    						String regPassword = inReader.readLine();

    						socialClient.register(regUsername, regPassword);
    						System.out.println("Registrazione avvenuta con successo");
    						break;
    						
    					case "2": 
    						// Richiesto login
    						
    						System.out.println("Immetti l'username:");
    						String logUsername = inReader.readLine();
    						System.out.println("Immetti la password:");
    						String logPassword = inReader.readLine();

    						socialClient.login(logUsername, logPassword);
    						System.out.println("Login effettuato");
    						isLoggedIn = true;
    						break;
    						
    					case "3": 
    						// Richiesta l'uscita dal programma
    						
    						System.exit(0);
    						break;
    						
    					default: 
    						
    						System.out.println("Scelta non prevista");
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
			System.out.println("[1]  Invia una richiesta di amicizia");
			System.out.println("[2]  Rispondi ad una richiesta di amicizia");
			System.out.println("[3]  Visualizza la lista degli amici");
			System.out.println("[4]  Cerca un utente");
			System.out.println("[5]  Pubblica qualcosa");
			System.out.println("[6]  Segui un utente");
			System.out.println("[7]  Visualizza richieste di amicizia");
			System.out.println("[8]  Visualizza nuovi contenuti");
			System.out.println("[9]  Logout");
			System.out.println("[10] Esci");
			System.out.println("=============================================");
		}
		else
		{
			System.out.println();
			System.out.println("=============================================");
			System.out.println("============== [Simple Social] ==============");
			System.out.println("=============================================");
			System.out.println("[1]  Registrati");
			System.out.println("[2]  Login");
			System.out.println("[3]  Esci");
			System.out.println("=============================================");
		}
		
	}
}
