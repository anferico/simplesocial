package simplesocial.socialclient;

public class ExpiredTokenException extends SocialException
{
	private static final long serialVersionUID = 1L;

	public ExpiredTokenException()
	{
		super("La sessione è scaduta, si prega di effettuare di nuovo il login.");
	}
}
