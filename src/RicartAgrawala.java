import java.util.LinkedList;
import java.util.Queue;

public class RicartAgrawala extends DistributedReadWrite {

	/**
	 * A class that will sleep for a random amount of time before requesting for
	 * the word string.
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
				Thread.sleep(this.waitTime * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (RicartAgrawala.this.awaitingFinalString) {
				return;
			}

			int timeStamp = RicartAgrawala.this.logicalTime;
			for (Node node : RicartAgrawala.this.nodes) {
				RicartAgrawala.this.requestWordString(node, timeStamp);
				RicartAgrawala.this.logicalTime++;
			}

		}

	}

	protected static final String ALGORITHM = "Ricart-Agrawala";
	private boolean hasString;
	private boolean awaitingFinalString;
	private Queue<Node> responseQueue = new LinkedList<Node>();
	private Queue<Request> requestQueue = new LinkedList<Request>();
	private Thread timer;
	private int logicalTime;
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

		this.hasString = this.node.isMasterNode();
		this.awaitingFinalString = false;
		this.responseQueue.clear();
		this.requestQueue.clear();
		this.doneNodes.clear();

		if (!this.node.isMasterNode()) {
			this.logicalTime = this.getRandomWaitingTime();
			this.sleeper = new Thread(new Sleeper(this.logicalTime));
			this.sleeper.start();
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
	 * Requests all nodes for access to the word string.
	 * 
	 * @param timeStamp
	 */
	public void requestAllForWordString() {
		if (this.awaitingFinalString) {
			return;
		}

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
		this.logicalTime++;
		Node node = this.findNodeById(senderId);
		if (node == null) {
			new Exception("Unknown address: " + senderId).printStackTrace();
		}

		if (this.responseQueue.contains(node)) {
			new Exception("Duplicate node " + senderId).printStackTrace();
		}

		System.out.println(String.format("receive OK t: %d id: %d", timeStamp, node.id));

		this.responseQueue.add(node);

		// We have access to the word string if all nodes respond with "OK"
		if (this.responseQueue.size() == this.nodes.size()) {
			this.hasString = true;
			System.out.println("--- Entering Critical Section ---");

			this.getWordStringFromMaster();
		}
	}

	@Override
	public void receiveWordString(String wordString) {
		if (this.node.isMasterNode()) {
			System.out.println("Updated word string: " + wordString);
			this.wordString = wordString;
			this.hasString = true;
		} else {
			if (this.awaitingFinalString) {
				this.checkFinalString(wordString);
			} else {
				// Add a random word to the string, and send it back to the
				// master node.
				System.out.println("old string: " + wordString);
				wordString = this.appendRandomWord(wordString);

				this.sendWordString(wordString, this.masterNode);
				System.out.println("new string: " + wordString);

				int seconds = this.getRandomWaitingTime();
				System.out.println("Waiting for " + seconds + " seconds");
				this.logicalTime += seconds;

				// Give up ownership of the word string.
				this.hasString = false;

				// Make the next request.
				System.out.println("--- Exiting Critical Section ---");
				while (!this.requestQueue.isEmpty()) {
					Request request = this.requestQueue.poll();
					this.sendWordStringOK(request.node, request.timeStamp);
				}

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
	 * 
	 * @param ip
	 * @param port
	 * @param timeStamp
	 */
	@Override
	public void receiveWordStringRequest(int requesterId, int timeStamp) {
		Node node = this.findNodeById(requesterId);
		if (node == null) {
			new Exception("Unknown id: " + requesterId).printStackTrace();
		}

		System.out.println(String.format("receive REQUEST t: %d id: %d", timeStamp, node.id));
		if (this.node.isMasterNode()) {
			this.sendWordStringOK(node, timeStamp);
		} else {
			if (this.hasString) {
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

		this.logicalTime = Math.max(this.logicalTime, timeStamp) + 1;
	}

	@Override
	protected void getWordStringFromMaster() {
		if (this.responseQueue.size() != this.nodes.size()) {
			System.out.println("ERROR: Did not receive OK from all nodes!");
		}

		this.responseQueue.clear();
		super.getWordStringFromMaster();
	}

	private boolean hasPriority(Request request) {
		return ((this.logicalTime < request.timeStamp)
				|| (this.logicalTime == request.timeStamp && this.node.id < request.node.id));
	}

	/**
	 * Tell the requesting node that this node does not need the word string.
	 * 
	 * @param node
	 * @param timeStamp
	 */
	private void sendWordStringOK(Node node, int timeStamp) {
		System.out.println("OK to " + node.id + " for " + timeStamp);
		this.sender.execute("strRequestOk", new Object[] { this.node.id, this.logicalTime }, node);
		this.logicalTime++;
	}

}
