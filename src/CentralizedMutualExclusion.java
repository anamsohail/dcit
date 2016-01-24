import java.util.LinkedList;
import java.util.Queue;

public class CentralizedMutualExclusion extends DistributedReadWrite {

	public class TimeAdvanceGrant implements Runnable {
		private int logicalTime;

		public TimeAdvanceGrant(int logicalTime) {
			this.logicalTime = logicalTime;
		}

		@Override
		public void run() {
			System.out.println("Time: " + this.logicalTime);

			if (this.logicalTime == 20) {
				CentralizedMutualExclusion.this.awaitingFinalString = true;
				CentralizedMutualExclusion.this.requestFinalString();
				return;
			}

			if (CentralizedMutualExclusion.this.nextRequestTime == this.logicalTime) {
				CentralizedMutualExclusion.this.requestWordString(CentralizedMutualExclusion.this.masterNode,
						this.logicalTime);
			}
		}
	}

	private int nextRequestTime;
	private Queue<Node> requestQueue = new LinkedList<Node>();
	private Thread timer;
	private Node servicedNode;
	private boolean awaitingFinalString;
	private Thread timeAdvanceGrant;

	protected static final String ALGORITHM = "Centralized Mutual Exclusion";

	public CentralizedMutualExclusion(Node node) {
		super(node);
	}

	@Override
	public void receiveTimeAdvanceGrant(int time) {
		this.timeAdvanceGrant = new Thread(new TimeAdvanceGrant(time));
		this.timeAdvanceGrant.start();
	}

	@Override
	public void receiveWordString(String wordString) {
		if (this.node.isMasterNode()) {
			System.out.println("Updated word string: " + wordString);
			this.wordString = wordString;

			System.out.println("Finished servicing " + this.servicedNode);
			this.servicedNode = null;
			this.checkRequestQueue();
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
				this.nextRequestTime += seconds;
			}
		}
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
			new Exception("Unknown address: " + requesterId).printStackTrace();
		}

		if (this.node.isMasterNode()) {
			System.out.println("Receive request from " + node);
			this.requestQueue.add(node);
			this.checkRequestQueue();
		}
	}

	/**
	 * Start the distributed read-write process.
	 */
	public void start() {
		super.start();
		System.out.println("Starting with algorithm: " + ALGORITHM);
		System.out.println(this.node);
		System.out.println("------------");

		this.nextRequestTime = -1;
		this.servicedNode = null;
		this.requestQueue.clear();
		this.doneNodes.clear();
		this.timer = new Thread(new Runnable() {

			@Override
			public void run() {
				long timeEnd = System.currentTimeMillis() + (20 * 1000);
				int logicalTime = 0;
				while (System.currentTimeMillis() < timeEnd) {
					try {
						Thread.sleep(1000);
						logicalTime += 1;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("Advance time to: " + logicalTime);
					CentralizedMutualExclusion.this.sendTimeAdvance(logicalTime);
				}
			}
		});

		if (this.node.isMasterNode()) {
			this.timer.start();
		} else {
			this.nextRequestTime = this.getRandomWaitingTime();
			System.out.println("Waiting for " + this.nextRequestTime + " seconds");
		}
	}

	private void checkRequestQueue() {
		System.out.println("check request queue");
		// Only the master node should run this code.
		if (!this.node.isMasterNode()) {
			return;
		}
		if (this.servicedNode == null) {
			Node node = this.requestQueue.poll();
			if (node != null) {
				System.out.println("Now servicing " + node);
				this.servicedNode = node;
				synchronized (this.wordString) {
					this.sendWordString(this.wordString, node);
				}
			}
		} else {
			System.out.println("Serviced node is not null: " + this.servicedNode);
		}
	}

	/**
	 * Tells all nodes to move their logical clocks one step forward.
	 */
	private void sendTimeAdvance(int logicalTime) {
		if (!this.node.isMasterNode()) {
			return;
		}

		for (Node node : nodes) {
			this.sender.execute("timeAdvance", new Object[] { logicalTime }, node);
		}
	}
}
