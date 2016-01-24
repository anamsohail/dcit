import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class DistributedReadWrite {
	
	/**
	 * Class for keeping track of appended words.
	 */
	private class IndexedWord {
		public int index;
		public String value;

		public IndexedWord(int index, String value) {

			this.index = index;
			this.value = value;
		}
	}
	
	protected static final String ALGORITHM = "";
	protected List<Node> nodes = new ArrayList<Node>();
	protected List<Node> doneNodes = new ArrayList<Node>();
	protected Node node;
	protected Node masterNode;
	protected String wordString;
	protected Sender sender;
	protected boolean awaitingFinalString;

	private static final int WAIT_MIN = 1;
	private static final int WAIT_MAX = 10;
	private final String WORDS[] = { "apple", "banana", "carrot", "date", "eggplant", "fig", "guava" };
	private Random rand = new Random();
	private List<IndexedWord> appended = new ArrayList<IndexedWord>();
	
	public static DistributedReadWrite create(Node node, Algorithm algorithm) {
		if (algorithm == Algorithm.CENTRALIZED_MUTUAL_EXCLUSION) {
			return new CentralizedMutualExclusion(node);
		} else {
			return new RicartAgrawala(node);
		}
	}

	/**
	 * Start the distributed read-write process.
	 */
	public void start() {
		this.appended.clear();
		this.wordString = "";
	}

	/**
	 * Appends a random word to the word string.
	 * 
	 * @param wordString
	 * @return the updated word string.
	 */
	protected String appendRandomWord(String wordString) {
		String[] tokens = wordString.split(" ");
		String newWord = WORDS[new Random().nextInt(WORDS.length)];
		wordString += (tokens.length == 0 ? "" : " ") + newWord;
		this.appended.add(new IndexedWord(tokens.length, newWord));
		return wordString;
	}

	/**
	 * Checks the final string from the master node.
	 */
	protected void checkFinalString(String wordString) {
		System.out.println("-------------------");
		System.out.println("final string: " + wordString);
		// Check that all appended words are in the final string.
		String[] finalTokens = wordString.split(" ");
		this.printStringList("words appended by this node:", this.appended);

		List<IndexedWord> missing = new ArrayList<IndexedWord>();
		for (IndexedWord word : this.appended) {
			if (!finalTokens[word.index].equals(word.value)) {
				missing.add(word);
			}
		}

		System.out.println("-------------------");
		if (missing.size() == 0) {
			System.out.println("All words are included in the final string.");
		} else {
			System.out.println("Some words are missing from the final string.");
			this.printStringList("Words missing from final string:", missing);
		}
	}

	/**
	 * Find the locally stored node from an IP address and port.
	 * 
	 * @param ip
	 * @param port
	 * @return The node if found, otherwise null.
	 */
	protected Node findNode(String ip, int port) {
		for (Node node : this.nodes) {
			if (node.ip.equals(ip) && node.port == port) {
				return node;
			}
		}

		// Check if requested node is this instance.
		if (this.node.ip.equals(ip) && port == this.node.port) {
			return this.node;
		}

		// Node not found.
		return null;
	}

	protected void requestFinalString() {
		if (this.node.isMasterNode()) {
			return;
		}

		System.out.println("Requesting final string");
		this.sender.execute("strRequestFinal", new Object[] { this.node.ip, this.node.port }, this.masterNode);
	}

	protected void sendFinalString(String ip, int port) {
		System.out.println("Request for final string");
		Node node = this.findNode(ip, port);
		if (node != null) {
			this.doneNodes.add(node);
			System.out.println(this.doneNodes.size() + " of " + this.nodes.size());
			if (this.doneNodes.size() == this.nodes.size()) {
				for (Node n : this.nodes) {
					this.sendWordString(this.wordString, n);
				}
			}
		} else {
			Exception e = new Exception("Node not found: " + ip + ":" + port);
			e.printStackTrace();
		}
	}

	protected void sendWordString(String ip, int port) {
		this.sendWordString(this.wordString, this.findNode(ip, port));
	}

	protected void sendWordString(String value, Node destination) {
		this.sender.execute("strUpdate", new Object[] { value }, destination);
	}

	/**
	 * Gets a random waiting time.
	 * 
	 * @return the logical waiting time.
	 */
	protected int getRandomWaitingTime() {
		return this.rand.nextInt(WAIT_MAX - WAIT_MIN) + WAIT_MIN;
	}

	protected void getWordStringFromMaster() {
		this.sender.execute("strRequestMaster", new Object[] { this.node.ip, this.node.port }, this.masterNode);
	}

	protected DistributedReadWrite(Node node) {
		this.node = node;
		this.masterNode = node.getMasterNode();
		this.nodes = node.nodes;
		this.sender = this.node.sender;
	}

	/**
	 * Requests for the master node's word string.
	 * 
	 * @return the master node's word string.
	 */
	protected void requestWordString(Node node, int timeStamp) {
		this.sender.execute("strRequest", new Object[] { this.node.ip, this.node.port, timeStamp }, node.ip, node.port);
	}

	/**
	 * Prints a list of strings in the format: [caption]: [word] [word]
	 * [word]...
	 * 
	 * @param caption
	 * @param list
	 */
	private void printStringList(String caption, List<IndexedWord> list) {
		System.out.print(caption);
		for (IndexedWord word : list) {
			System.out.print(" " + word.value);
		}
		System.out.println();
	}

	/**
	 * Received a request from a node for the word string.
	 * 
	 * @param ip
	 * @param port
	 * @param timeStamp
	 */
	abstract void receiveWordStringRequest(String ip, int port, int timeStamp);

	abstract void receiveWordString(String wordString);

	abstract void receiveTimeAdvanceGrant(int time);
}
