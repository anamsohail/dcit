import java.util.LinkedList;
import java.util.Queue;

/***
 * Class responsible for running the centralized mutual exclusion algorithm.
 */
public class CentralizedMutualExclusion extends DistributedReadWrite {

	/**
	 * This thread is run by non-master nodes.  It decides what
	 * the node should do at a given time.
	 */
	public class TimeAdvanceGrant implements Runnable {
		private int logicalTime;

		public TimeAdvanceGrant(int logicalTime) {
			this.logicalTime = logicalTime;
		}

		@Override
		public void run() {
			System.out.println("Time: " + this.logicalTime);

			// At 20 seconds we are done.  Keep a reminder that we are now waiting
			// for the final word string from the master node, then tell
			// the master node that we want the final string.
			if (this.logicalTime == 20) {
				CentralizedMutualExclusion.this.awaitingFinalString = true;
				CentralizedMutualExclusion.this.requestFinalString();
				return;
			}

			// Earlier, we chose a random waiting time (see CentralizedMutualExclusion.start()
			// and CentralizedMutualExclusion.receiveWordString()).  Tell the master node
			// that we want the word string if this is the scheduled time.
			if (CentralizedMutualExclusion.this.nextRequestTime == this.logicalTime) {
				CentralizedMutualExclusion.this.requestWordString(CentralizedMutualExclusion.this.masterNode,
						this.logicalTime);
			}
		}
	}

	// The logical time at which non-master nodes request the word string.
	private int nextRequestTime;
	
	// The queue where the master node stores requests for the word string.
	private Queue<Node> requestQueue = new LinkedList<Node>();
	
	// The timer, which the master node runs, that sends time updates every second.
	private Thread timer;
	
	// The non-master node that is currently being serviced by the master node.
	private Node servicedNode;
	
	// A flag for non-master nodes that prevents them from performing a
	// read-write operation upon receiving the final word string from the master node.
	private boolean awaitingFinalString;
	
	// The handler for non-master nodes, that determines what action should be done
	// at a given time.
	private Thread timeAdvanceGrant;

	protected static final String ALGORITHM = "Centralized Mutual Exclusion";

	public CentralizedMutualExclusion(Node node) {
		super(node);
	}

	/**
	 * Sent to non-master nodes so they can perform the appropriate
	 * actions according to the time.
	 * 
	 * @param time The new time in seconds
	 */
	@Override
	public void receiveTimeAdvanceGrant(int time) {
		this.timeAdvanceGrant = new Thread(new TimeAdvanceGrant(time));
		this.timeAdvanceGrant.start();
	}

	/**
	 * For non-master nodes, this method is the response to a "wordStringRequest".
	 * For the master node, this is called when a node has returned the word string.
	 */
	@Override
	public void receiveWordString(String wordString) {
		if (this.node.isMasterNode()) {
			
			// A non-master node has returned the string to us.
			// Store the new value.
			System.out.println("Updated word string: " + wordString);
			this.wordString = wordString;

			// Mark ourselves as "not busy" so we know we can service incoming requests.
			// Then check if there are any requests in the queue.
			System.out.println("Finished servicing " + this.servicedNode);
			this.servicedNode = null;
			this.checkRequestQueue();
			
		} else {
			if (this.awaitingFinalString) {
				
				// We were waiting for the final word string from the master node.
				// Check whether all the words we have appended are found in this final string.
				this.checkFinalString(wordString);
				
			} else {
				
				// Add a random word to the string, and send it back to the
				// master node.
				System.out.println("old string: " + wordString);
				wordString = this.appendRandomWord(wordString);

				this.sendWordString(wordString, this.masterNode);
				System.out.println("new string: " + wordString);

				// Now that we have finished our read and write, we need
				// to determine the next time at which we will perform
				// another read and write operation.
				int seconds = this.getRandomWaitingTime();
				System.out.println("Waiting for " + seconds + " seconds");
				this.nextRequestTime += seconds;
			}
		}
	}

	/**
	 * Received a request from a node for the word string.
	 * 
	 * In the CME algorithm, this method is only called in the master node.
	 */
	@Override
	public void receiveWordStringRequest(int requesterId, int timeStamp) {
		
		// Get the node that corresponds to the ID.
		Node node = this.findNodeById(requesterId);
		if (node == null) {
			new Exception("Unknown address: " + requesterId).printStackTrace();
		}

		if (this.node.isMasterNode()) {
			// Even if we are not busy, we will queue the node,
			// then immediately poll it in checkRequestQueue()
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

		// Reset all our fields.
		this.nextRequestTime = -1;
		this.servicedNode = null;
		this.requestQueue.clear();
		this.doneNodes.clear();
		
		// This timer is only used by the master node.  It runs for 20 seconds,
		// and every second it will inform all non-master nodes of the new time.
		this.timer = new Thread(new Runnable() {

			@Override
			public void run() {
				int logicalTime = 0;
				for (int i = 0; i < 20; i++) {
					try {
						// Sleep for one second, then increase the logical time.
						Thread.sleep(1000);
						logicalTime += 1;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// Announce the new time to all non-master nodes.
					System.out.println("Advance time to: " + logicalTime);
					CentralizedMutualExclusion.this.sendTimeAdvance(logicalTime);
				}
			}
		});

		if (this.node.isMasterNode()) {
			// Start the 20 second clock.
			this.timer.start();
		} else {
			// Determine at what time we will perform our first read-write operation.
			// The nextRequestTime value is used in the class TimeAdvanceGrant
			this.nextRequestTime = this.getRandomWaitingTime();
			System.out.println("Waiting for " + this.nextRequestTime + " seconds");
		}
	}

	/**
	 * Called in the master node only.  This is where the master
	 * node checks whether there are any nodes that are requesting
	 * the word string.
	 */
	private void checkRequestQueue() {
		System.out.println("check request queue");
		// Only the master node should run this code.
		if (!this.node.isMasterNode()) {
			return;
		}
		
		// Make sure we're not busy before checking the queue.
		if (this.servicedNode == null) {
			Node node = this.requestQueue.poll();
			if (node != null) {
				
				// Send the word string to the first node in the queue.
				// We will not dequeue any more nodes until the first node
				// returns the word string.
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
			XmlRpcSender.execute("timeAdvance", new Object[] { logicalTime }, node);
		}
	}
}
