package bc19;
@SuppressWarnings("serial")
public class BCException extends RuntimeException {
	public BCException(String errorMessage) {
		super(errorMessage);
	}
}