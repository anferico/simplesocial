package simplesocial.socialclient;

public class Friend
{
	private String username;
	private boolean online;
	
	public Friend(String username, boolean isOnline)
	{
		this.username = username;
		this.online = isOnline;
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public boolean isOnline()
	{
		return online;
	}
}
