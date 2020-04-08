package simplesocial.socialserver;

import java.io.*;

public class PendingContent implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private String content;
	private String recipient;
	
	public PendingContent(String content, String recipient)
	{
		this.content = content;
		this.recipient = recipient;
	}

	public String getContent()
	{
		return content;
	}
	
	public String getRecipient()
	{
		return recipient;
	}
}
