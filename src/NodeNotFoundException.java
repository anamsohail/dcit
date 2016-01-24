
public class NodeNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int id;
	
	public NodeNotFoundException(int id) {
		this.id = id;
	}
	
	public String getMessage() {
		return "Unknown ID: " + this.id;
	}

}
