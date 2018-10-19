package simplesocial.socialserver;

import java.io.*;
import java.util.*;

public class Friendship implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private String username1;
	private String username2;
	private Date requestDate;
	
	public Friendship(String username1, String username2)
	{
		this.username1 = username1;
		this.username2 = username2;
		this.requestDate = new Date();
	}
	
	// Restituisce true se questa amicizia lega 'username1' e 'username2', false
	// altrimenti
	public boolean involves(String username1, String username2) 
	{
		return (this.username1 == username1 && this.username2 == username2)
				|| (this.username1 == username2 && this.username2 == username1);
	}
	
	public String getFirstUser()
	{
		return username1;
	}
	
	public String getSecondUser()
	{
		return username2;
	}

	public Date getRequestDate()
	{
		return requestDate;
	}
}
