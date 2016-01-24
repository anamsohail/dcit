import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node {
	public String ip = "";
	public int port = -1;
	public int id = new Random().nextInt(10000);
	public ArrayList<Node> nodes = new ArrayList<Node>();
	public boolean responded = false;
	private Thread timer;
	public int nextRequestTime;
	public Algorithm algorithm;
	public Sender sender = new Sender();
	private static String USAGE = "Usage: Node.java <port>";
	public boolean isJoined = false;
	private Node masterNode;
	private String wordString;
	private Queue<Node> requestQueue = new LinkedList<Node>();
	private Queue<Request> requestQueueRA = new LinkedList<Request>();
	private List<Node> doneNodes = new ArrayList<Node>();
	private Node servicedNode = null;
	private DistributedReadWrite distReadWrite = new DistributedReadWrite();
	public boolean awaitingFinalString;
	private boolean hasString;
	private int logicalTime;
	private Thread timerRA;
	
	public Node(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	public Node() {
		// Do nothing
	}

	/**
	 * Joins a network by connecting to a node that
	 * is already in the network. 
	 * @param Ip
	 * @param port
	 * @param myPort
	 */
	public void join(String ip,int port, int myPort){
		try{
			this.sender.execute("join", new Object[] { ip, port, this.ip, String.valueOf(this.port), this.id }, ip, port);
			this.isJoined = true;
		}catch(Exception e ){e.printStackTrace();}	
	}

	public void sendIDtoNewNode(String ip, String port, int ID) {
		this.sender.execute("newID", new Object[] { ID }, ip, Integer.parseInt(port));
	}

	public void sendToAll(String ip, String port, int newID) {
		if(nodes.size()>0) {
			System.out.println("sending new node to all other nodes!");
			for (Node node : this.nodes) {
				this.sender.execute("newNode", new Object[] { ip, Integer.parseInt(port), newID }, node.ip, node.port);
			}
		}
		sendListToNewNode(ip,port);
		System.out.println("adding new node to List");
		addNodeToList(ip, Integer.parseInt(port), newID);
	}

	public void sendListToNewNode(String ip, String port) {
		if(nodes.size()>0) {
			System.out.println("sending list to new node!");
			for (Node node : this.nodes) {
				this.sender.execute("newNode", new Object[] { node.ip, node.port, node.id }, ip, Integer.parseInt(port));
			}
		}
		System.out.println("Sending my info to new node...");
		this.sender.execute("newNode", new Object[] { this.ip, this.port, this.id }, ip, Integer.parseInt(port));
	}

	public void sendOK(String ip, int port) {
		this.sender.execute("ok", new Object[] { this.ip, this.port, this.id }, ip, port);
	}

	/**
	 * Send "start" command to all connected nodes.
	 */
	public void sendStart(Algorithm algorithm) {
		if (!this.isJoined) {
			System.out.println("You must join a network before you can start.");
			return;
		}
		
		if (this.masterNode == null) {
			System.out.println("You need to elect a master node first.");
			return;
		}

		for (Node node : nodes) {
			this.sender.execute("start", new Object[] { algorithm.ordinal() }, node.ip, node.port);
		}
		
		this.start(algorithm);
	}

	public void start(Algorithm algorithm){
		System.out.println("Starting with algorithm: " + algorithm);
		System.out.println(this);
		System.out.println("------------");
		this.algorithm = algorithm;
		// Reset values used during the distributed read/write
		this.nextRequestTime = -1;
		this.wordString = "";
		this.hasString = this.isMasterNode();
		this.servicedNode = null;
		this.awaitingFinalString = false;
		this.requestQueue.clear();
		this.requestQueueRA.clear();
		this.doneNodes.clear();
		this.distReadWrite.reset();
		this.timer = new Thread(new Timer(this));
		
		if (this.isMasterNode()) {
			if (this.algorithm == Algorithm.CENTRALIZED_MUTUAL_EXCLUSION) {
				this.timer.start();
			}
		} else {
			this.nextRequestTime = this.distReadWrite.getRandomWaitingTime();
			System.out.println("Waiting for " + this.nextRequestTime + " seconds");
			
			if (this.algorithm == Algorithm.RICART_AGRAWALA) {
				this.logicalTime = this.nextRequestTime;
				this.timerRA = new Thread(new TimerRicartAgrawala(this, this.nextRequestTime));
				this.timerRA.start();
				this.timer = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(20 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						Node.this.awaitingFinalString = true;
						Node.this.requestFinalString();
					}
					
				});
				
				this.timer.start();
			}
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
	 * Received a request from a node for the word string.
	 * 
	 * @param ip
	 * @param port
	 * @param timeStamp
	 */
	public void receiveWordStringRequest(String ip, int port, int timeStamp) {
		Node node = this.findNode(ip, port);
		if (node == null) {
			new Exception("Unknown address: " + ip + ":" + port).printStackTrace();
		}
		
		if (this.algorithm == Algorithm.CENTRALIZED_MUTUAL_EXCLUSION) {
			if (this.isMasterNode()) {
				System.out.println("Receive request from " + node);
				this.requestQueue.add(node);
				this.checkRequestQueue();
			}
		} else if (this.algorithm == Algorithm.RICART_AGRAWALA) {
			
			System.out.println("REQUEST from " + node + " at " + timeStamp);
			if (this.isMasterNode()) {
				this.sendWordStringOK(node, timeStamp);
			} else {
				if (this.hasString) {
					System.out.println("I have the string. Queue Request from " + node);
					this.requestQueueRA.add(new Request(node, timeStamp));
				} else {
					Request request = new Request(node, timeStamp);
					if (this.hasPriority(request)) {
						System.out.println("I have higher priority over " + node + ". Queueing.");
						this.requestQueueRA.add(request);
					} else {
						this.sendWordStringOK(node, timeStamp);
					}
				}
			}
			
			this.logicalTime = Math.max(this.logicalTime, timeStamp) + 1;
		}
	}
	
	/**
	 * Tell the requesting node that this node does not need the word string.
	 * 
	 * @param node
	 * @param timeStamp
	 */
	private void sendWordStringOK(Node node, int timeStamp) {
		System.out.println("OK to " + node + " for " + timeStamp);
		this.sender.execute("strRequestOk", new Object[] { this.ip, this.port, this.logicalTime}, node.ip, node.port);
		this.logicalTime++;
	}
	
	/**
	 * Receive "OK" message from a node.
	 * 
	 * @param ip
	 * @param port
	 * @param timeStamp
	 */
	public void receiveWordStringOK(String ip, int port, int timeStamp) {
		this.logicalTime++;
		Node node = this.findNode(ip, port);
		if (node == null) {
			new Exception("Unknown address: " + ip + ":" + port).printStackTrace();
		}
		
		if (this.requestQueue.contains(node)) {
			new Exception("Duplicate node " + node).printStackTrace();
		}
		
		System.out.println("OK from " + node);
		
		this.requestQueue.add(node);
		
		// We have access to the word string if all nodes respond with "OK"
		if (this.requestQueue.size() == this.nodes.size()) {
			this.hasString = true;
			System.out.println("/// Entering Critical Section \\\\\\");
			
			this.getWordStringFromMaster();
		}
	}
	
	public void getWordStringFromMaster() {
		if (this.algorithm == Algorithm.RICART_AGRAWALA) {
			if (this.requestQueue.size() != this.nodes.size()) {
				System.out.println("ERROR: Did not receive OK from all nodes!");
			}
			
			this.requestQueue.clear();
		}
		
		this.sender.execute("strRequestMaster", new Object[] { this.ip, this.port}, this.masterNode);
	}

	/**
	 * Requests for the master node's word string.
	 * 
	 * @return the master node's word string.
	 */
	public void requestWordString(Node node, int timeStamp) {
		if (this.algorithm == Algorithm.RICART_AGRAWALA)  {
			System.out.println("REQUEST to " + node + " for " + timeStamp);
		}
		
		this.sender.execute("strRequest", new Object[] { this.ip, this.port, timeStamp }, node.ip, node.port);
	}

	public void sendFinalString(String ip, int port) {
		Node node = this.findNode(ip, port);
		if (node != null) {
			this.doneNodes.add(node);
			if (this.doneNodes.size() == this.nodes.size()) {
				for (Node n: this.nodes) {
					this.sendWordString(this.wordString, n);
				}
			}
		} else {
			Exception e = new Exception("Node not found: " + ip + ":" + port);
			e.printStackTrace();
		}
	}

	/**
	 * Called when a node receives the word string from another node.
	 * 
	 * @param wordString
	 */
	public void receiveWordString(String wordString) {
		if (this.isMasterNode()) {
			System.out.println("Updated word string: " + wordString);
			this.wordString = wordString;
			this.hasString = true;
			
			if (this.algorithm == Algorithm.CENTRALIZED_MUTUAL_EXCLUSION) {
				System.out.println("Finished servicing " + this.servicedNode);
				this.servicedNode = null;
				this.checkRequestQueue();
			}
		} else {
			if (this.awaitingFinalString) {
				this.distReadWrite.checkFinalString(wordString);
			} else {
				// Add a random word to the string, and send it back to the master node.
				System.out.println("old string: " + wordString);
				wordString = this.distReadWrite.appendRandomWord(wordString);
	
				this.sendWordString(wordString, this.masterNode);
				System.out.println("new string: " + wordString);
				
				int seconds = this.distReadWrite.getRandomWaitingTime();
				System.out.println("Waiting for " + seconds + " seconds");
				this.nextRequestTime += seconds;
				this.logicalTime += seconds;
				
				if (this.algorithm == Algorithm.RICART_AGRAWALA) {
					
					// Give up ownership of the word string.
					this.hasString = false;
					
					// Make the next request.
					System.out.println("\\\\\\ Exiting Critical Section ///");
					while (!this.requestQueueRA.isEmpty()) {
						Request request = this.requestQueueRA.poll();
						this.sendWordStringOK(request.node, request.timeStamp);
					}
					
					System.out.println("REQUEST " + this.logicalTime);
					this.timerRA = new Thread(new TimerRicartAgrawala(this, seconds));
					this.timerRA.start();
				}
			}
		}
	}
	
	private boolean hasPriority(Request request) {
		return ((this.logicalTime < request.timeStamp) || (this.logicalTime == request.timeStamp && this.id < request.node.id));
	}

	public void requestFinalString(){
		if (this.isMasterNode()) {
			return;
		}
		
		System.out.println("Requesting final string");
		this.sender.execute("strRequestFinal", new Object[] { this.ip, this.port }, this.masterNode.ip, this.masterNode.port);
	}

	/**
	 * Stores a new node's information in a list
	 * of all nodes in the network.
	 * 
	 * @param ip
	 * @param port
	 */
	public void addNodeToList(String ip, int port, int ID) {
		//add new node to the list
		Node newNode = new Node();
		newNode.ip = ip;
		newNode.port = port;
		newNode.id = ID;
		nodes.add(newNode);
		System.out.println("new node added. printing list...");
		for (Node node : this.nodes) {
			System.out.println(this.nodes.indexOf(node) + 1 + ": " + node);
		}
	}

	/**
	 * Checks whether a node contains the given network information.
	 * 
	 * @param ip
	 * @param port
	 * @return true if the node is in the network, otherwise false.
	 */
	public boolean checkInList(String ip, String port) {
		if(this.ip.equals(ip) & this.port == Integer.parseInt(port))
			return true;
		
		if(nodes.size()>0) {
			System.out.println("checking if node already exists...");
			
			for (Node node : this.nodes) {
				return node.ip.equals(ip) && node.port == Integer.parseInt(port);
			}
		}
		return false;
	}

	public int checkID(int ID) {
		if(ID==this.id) {
			System.out.println("ID is the same as my ID...changing ID...");
			ID = (int) (Math.random() * (10000 - 0));
			ID = checkID(ID);
		}
		if(nodes.size()>0) {
			System.out.println("checking if ID is unique...");
			for (int i = 0; i < nodes.size(); i++) {
				int oldID = nodes.get(i).id;
				if(ID==oldID) {
					System.out.println("ID isn't unique...changing ID...");
					ID = (int) (Math.random() * (10000 - 0));
					ID = checkID(ID);
				}
			}
		}
		return ID;
	}
	
	public String toString() {
		return String.format("IP: %s Port: %d ID: %d", this.ip, this.port, this.id);
	}

	/**
	 * Find the locally stored node from an IP address and port.
	 * 
	 * @param ip
	 * @param port
	 * @return The node if found, otherwise null.
	 */
	public Node findNode(String ip, int port) {
		for (Node node: this.nodes) {
			if (node.ip.equals(ip) && node.port == port) {
				// Node found.
				return node;
			}
		}
		// Check if requested node is this instance.
		if (this.ip.equals(ip) && port == this.port) {
			return this;
		}
		System.out.println(this.ip);
		// Node not found.
		return null;
	}

	public int findNodeIndex(int ID) {
		if(nodes.isEmpty()) {
			System.out.println("List is empty");
			return -1;
		}
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).id == ID) {
				System.out.println("Node found!");
				return i;
			}
		}
		System.out.println("Node not in list!");
		return -1;
	}
	
	public void sendWordString(String ip, int port) {
		this.sendWordString(this.wordString, this.findNode(ip, port));
	}
	
	public boolean isMasterNode() {
		return this.masterNode.equals(this);
	}

	/**
	 * Send a word string to the provided node.
	 */
	private void sendWordString(String value, Node destination) {
		this.sender.execute("strUpdate", new Object[] { value }, destination);
	}

	public void setMasterNode(Node Winner) {
		if(Winner==null) {
			System.out.println("No master node for the time being!");
			return;
		}
		System.out.println("====Master node elected====");
		System.out.println(Winner);
		System.out.println("===========================");
		this.masterNode = Winner;
	}

	/**
	 * Tells all nodes to move their logical clocks one step forward.
	 */
	public void sendTimeAdvance(int logicalTime) {
		if (!this.isMasterNode()) {
			return;
		}
		
		for (Node node : nodes) {
			this.sender.execute("timeAdvance", new Object[] { logicalTime }, node);
		}
	}

	public Node getMasterNode() {
		return this.masterNode;
	}

	@Override
	public boolean equals(Object object) {
		return this.equals((Node)object);
	}

	public boolean equals(Node node) {
		return this.ip.equals(node.ip) && this.port == node.port;
	}

	private void checkRequestQueue() {
		if (this.algorithm == Algorithm.CENTRALIZED_MUTUAL_EXCLUSION) {
			// Only the master node should run this code.
			if (!this.isMasterNode()) {
				return;
			}
			if (this.servicedNode == null) {
				Node node = this.requestQueue.poll();
				if (node != null) {
					System.out.println("Now servicing " + node);
					this.servicedNode = node;
					synchronized(this.wordString) {
						this.sendWordString(this.wordString, node);
					}
				}
			} else {
				System.out.println("Serviced node is not null: " + this.servicedNode);
			}
		}
	}

	//MAIN
	//Usage: join <IP address> <port>
	public static void main(String[] argv){
		int port;
		if (argv.length != 1) {
			System.out.println(USAGE);
			return;
		}
		try {
			port = Integer.valueOf(argv[0]);
		} catch (NumberFormatException ex) {
			System.out.println(USAGE);
			return;
		}
		try {
			// Get local IP address from format: <hostname>/<IP address>
			Matcher matcher = Pattern.compile(".*/(.*)").matcher(InetAddress.getLocalHost().toString());
			if (matcher.find()) {
				Node node = new Node(matcher.group(1), port);
				Incoming.NODE = node;
				new Thread(new Incoming()).start();
				new Thread(new Reading(node)).start();
				System.out.println(node);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}	
	}
}