package simplesocial.socialclient;

public class ExpiredTokenException extends SocialException
{
    private static final long serialVersionUID = 1L;

    public ExpiredTokenException()
    {
        super("Session expired. Please log in again.");
    }
}
