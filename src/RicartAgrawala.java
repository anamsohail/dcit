import java.util.LinkedList;
import java.util.Queue;

public class RicartAgrawala extends DistributedReadWrite {

	/**
	 * A class that will sleep for a random amount of time before requesting for
	 * the word string.
	 *
	 * Unlike in the CME algorithm, each node uses their own clocks to determine
	 * when they will perform read-write operations and when the 20 seconds is over.
	 * 
	 */
	public class Sleeper implements Runnable {
		private int waitTime;

		public Sleeper(int waitTime) {
			this.waitTime = waitTime;
		}

		@Override
		public void run() {
			try {
				// Wait until we perform our next read-write operation.
				Thread.sleep(this.waitTime * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// If we have waited past the 20 second point,
			// and we have already asked for the final word string
			// from the master node, then we do nothing.
			if (RicartAgrawala.this.awaitingFinalString) {
				return;
			}

			// After waiting, send a message to all nodes in the network,
			// requesting exclusive access to the word string.
			int timeStamp = RicartAgrawala.this.logicalTime;
			for (Node node : RicartAgrawala.this.nodes) {
				RicartAgrawala.this.requestWordString(node, timeStamp);
				
				// We increase our logical time after every request.
				RicartAgrawala.this.logicalTime++;
			}

		}

	}

	/**
	 * Used to keep track of the nodes making requests and the
	 * time stamps at which they made their requests.
	 */
	public class Request {
		public Node node;
		public int timeStamp;
		
		public Request(Node node, int timeStamp) {
			this.node = node;
			this.timeStamp = timeStamp;
		}
	}

	protected static final String ALGORITHM = "Ricart-Agrawala";
	
	// A boolean to check whether the node is in the critical section or not.
	private boolean hasString;
	
	// Used to make sure we don't perform any read-write operations upon
	// receiving the final string from the master node.
	private boolean awaitingFinalString;
	
	// A queue/list were we keep count of the "OK" responses we receive
	// from the other nodes upon requesting for the word string.
	private Queue<Node> responseQueue = new LinkedList<Node>();
	
	// A queue for storing incoming requests with a lower priority than ours.
	private Queue<Request> requestQueue = new LinkedList<Request>();
	
	// The thread that will keep track of the 20 seconds.
	private Thread timer;
	
	// The logical time (and not wall-clock time).  This value may exceed 20 during
	// execution (seen in the trace log).
	private int logicalTime;
	
	// The thread that is responsible for waiting until the next read-write operation.
	private Thread sleeper;

	public RicartAgrawala(Node node) {
		super(node);
	}

	/**
	 * Start the distributed read-write process.
	 */
	public void start() {
		super.start();

		System.out.println("Starting with algorithm: " + ALGORITHM);
		System.out.println(this.node);
		System.out.println("------------");

		// Reset all fields.
		this.hasString = this.node.isMasterNode();
		this.awaitingFinalString = false;
		this.responseQueue.clear();
		this.requestQueue.clear();
		this.doneNodes.clear();

		// The master node does not need to do anything at this stage.
		if (!this.node.isMasterNode()) {
			this.logicalTime = this.getRandomWaitingTime();
			this.sleeper = new Thread(new Sleeper(this.logicalTime));
			this.sleeper.start();
			
			// Non-master nodes will individually keep track of a 20-second clock.
			// At the end of the 20 seconds, they will all request the final word
			// string from the master node.
			this.timer = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(20 * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					RicartAgrawala.this.awaitingFinalString = true;
					RicartAgrawala.this.requestFinalString();
				}

			});

			this.timer.start();
		}
	}

	/**
	 * Called when a non-master node wants exclusive access
	 * to the word string.
	 * 
	 * The "OK" replies are handled in the method receiveWordStringOK()
	 */
	public void requestAllForWordString() {
		if (this.awaitingFinalString) {
			return;
		}

		// Send the request to all the nodes in the network.
		int timeStamp = this.logicalTime;
		for (Node node : this.nodes) {
			this.requestWordString(node, timeStamp);
			this.logicalTime++;
		}
	}

	/**
	 * Receive "OK" message from a node.
	 * 
	 * @param senderId
	 * @param timeStamp
	 */
	public void receiveWordStringOK(int senderId, int timeStamp) {
		// Increase the logical time upon receiving a message.
		this.logicalTime++;
		Node node = this.findNodeById(senderId);
		if (node == null) {
			new Exception("Unknown address: " + senderId).printStackTrace();
		}

		if (this.responseQueue.contains(node)) {
			new Exception("Duplicate node " + senderId).printStackTrace();
		}

		System.out.println(String.format("receive OK t: %d id: %d", timeStamp, node.id));

		// Add this response to the queue and wait for the rest of the nodes.
		this.responseQueue.add(node);

		// We have access to the word string if all nodes respond with "OK"
		if (this.responseQueue.size() == this.nodes.size()) {
			this.hasString = true;
			System.out.println("--- Entering Critical Section ---");

			// Now that we are in the critical section, we can simply fetch the
			// word string from the master.
			this.getWordStringFromMaster();
		}
	}

	/**
	 * Called in non-master nodes when we receive the word string
	 * from the master node.
	 * Called in the master node when a non-master node is returning
	 * the newly edited word string.
	 */
	@Override
	public void receiveWordString(String wordString) {
		if (this.node.isMasterNode()) {
			// Save the new string.
			System.out.println("Updated word string: " + wordString);
			this.wordString = wordString;
			this.hasString = true;
		} else {
			if (this.awaitingFinalString) {
				
				// If we were waiting for the final string, then check if all of
				// the words that we have appended are in this string.
				this.checkFinalString(wordString);
				
			} else {
				
				// Add a random word to the string, and send it back to the
				// master node.
				System.out.println("old string: " + wordString);
				wordString = this.appendRandomWord(wordString);

				this.sendWordString(wordString, this.masterNode);
				System.out.println("new string: " + wordString);

				// Give up ownership of the word string.
				System.out.println("--- Exiting Critical Section ---");
				this.hasString = false;

				int seconds = this.getRandomWaitingTime();
				System.out.println("Waiting for " + seconds + " seconds");
				this.logicalTime += seconds;

				// Now that we are done with the word string, we must send "OK" to all queued.
				while (!this.requestQueue.isEmpty()) {
					Request request = this.requestQueue.poll();
					this.sendWordStringOK(request.node, request.timeStamp);
				}

				// Sleep until the next time we will do another read-write.
				this.sleeper = new Thread(new Sleeper(seconds));
				this.sleeper.start();
			}
		}
	}

	@Override
	public void requestWordString(Node node, int timeStamp) {
		System.out.println(String.format("send REQUEST t: %d id: %d", timeStamp, node.id));
		super.requestWordString(node, timeStamp);
	}

	@Override
	public void receiveTimeAdvanceGrant(int time) {
		// Do nothing.
	}

	/**
	 * Received a request from a node for the word string.
	 */
	@Override
	public void receiveWordStringRequest(int requesterId, int timeStamp) {
		Node node = this.findNodeById(requesterId);
		if (node == null) {
			new Exception("Unknown id: " + requesterId).printStackTrace();
		}

		System.out.println(String.format("receive REQUEST t: %d id: %d", timeStamp, node.id));
		if (this.node.isMasterNode()) {
			
			// The master node never needs to keep the word string, so we always send "OK"
			this.sendWordStringOK(node, timeStamp);
			
		} else {
			if (this.hasString) {
				// A node has requested for the word string while I am still performing my read-write.
				// Queue this node until we are done.
				System.out.println("I have the string. Queue Request from " + requesterId);
				this.requestQueue.add(new Request(node, timeStamp));
			} else {
				
				Request request = new Request(node, timeStamp);
				if (this.hasPriority(request)) {
					System.out.println("I have higher priority over " + requesterId + ". Queueing.");
					this.requestQueue.add(request);
				} else {
					this.sendWordStringOK(node, timeStamp);
				}
			}
		}

		// Set our new logical time.
		this.logicalTime = Math.max(this.logicalTime, timeStamp) + 1;
	}

	/**
	 * This method is called by non-master nodes.  It should
	 * only be called if we have received an "OK" reply from
	 * all other nodes.
	 */
	@Override
	protected void getWordStringFromMaster() {
		if (this.responseQueue.size() != this.nodes.size()) {
			System.out.println("ERROR: Did not receive OK from all nodes!");
		}

		// Clear the response queue for the next request.
		this.responseQueue.clear();
		
		super.getWordStringFromMaster();
	}

	/**
	 * If we have priority over the given request, then we will queue the request.
	 */
	private boolean hasPriority(Request request) {
		return ((this.logicalTime < request.timeStamp)
				|| (this.logicalTime == request.timeStamp && this.node.id < request.node.id));
	}

	/**
	 * Tell the requesting node that it can have access to the word string.
	 * 
	 * @param node
	 * @param timeStamp
	 */
	private void sendWordStringOK(Node node, int timeStamp) {
		System.out.println(String.format("send OK t: %d id: %d", timeStamp, node.id));
		XmlRpcSender.execute("wordStringResponse", new Object[] { this.node.id, this.logicalTime }, node);
		this.logicalTime++;
	}

}
