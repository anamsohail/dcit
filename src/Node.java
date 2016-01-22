import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node {
	public String OwnIp;
	public int OwnPort;
	public int ID;
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
	private List<Node> doneNodes = new ArrayList<Node>();
	private Node servicedNode = null;
	private DistributedReadWrite distReadWrite = new DistributedReadWrite();
	public boolean awaitingFinalString;
	private boolean hasString;

	/**
	 * Initializes New Node
	 * @param ip
	 * @param port
	 */
	public void create(String ip,int port){
		try{
			OwnIp = ip;
			OwnPort = port;
			ID = (int) (Math.random() * (10000 - 0));
		}catch(Exception e){e.printStackTrace();}
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
			String IpPort=ip+","+port+","+OwnIp+","+myPort+","+this.ID;
			String send="join,"+IpPort;
			this.sender.execute("join", new Object[] { send }, ip, port);
			this.isJoined = true;
			System.out.println("sent message: "+send);
		}catch(Exception e ){e.printStackTrace();}	
	}

	public void sendIDtoNewNode(String ip, String port, int ID) {
		String send="newID,"+String.valueOf(ID);
		this.sender.execute("newID", new Object[] { send }, ip, Integer.parseInt(port));
		System.out.println("sent message: "+send);
	}

	public void sendToAll(String ip, String port, int newID) {
		if(nodes.size()>0) {
			System.out.println("sending new node to all other nodes!");
			for (int i = 0; i < nodes.size(); i++) {//send new node to already existing nodes
				String IpPort=ip+","+port+","+String.valueOf(newID);
				String send="new,"+IpPort;
				String oldIP = nodes.get(i).OwnIp;
				int oldPort = nodes.get(i).OwnPort;
				this.sender.execute("newNode", new Object[] { send }, oldIP, oldPort);
				System.out.println("sent message: "+send);
			}
		}
		sendListToNewNode(ip,port);
		System.out.println("adding new node to List");
		addNodeToList(ip, Integer.parseInt(port), newID);
	}

	public void sendListToNewNode(String ip, String port) {
		if(nodes.size()>0) {
			System.out.println("sending list to new node!");
			for (int i = 0; i < nodes.size(); i++) {//send nodes one by one to new node
				String oldIP = nodes.get(i).OwnIp;
				String oldPort = String.valueOf(nodes.get(i).OwnPort);
				String oldID = String.valueOf(nodes.get(i).ID);
				String IpPort=oldIP+","+oldPort+","+oldID;
				String send="new,"+IpPort;
				this.sender.execute("newNode", new Object[] { send }, ip, Integer.parseInt(port));
				System.out.println("sent message: "+send);
			}
		}
		System.out.println("Sending my info to new node...");
		String myIP = OwnIp;
		String myPort = String.valueOf(OwnPort);
		String myID = String.valueOf(ID);
		String IpPort=myIP+","+myPort+","+myID;
		String send="new,"+IpPort;
		this.sender.execute("newNode", new Object[] { send }, ip, Integer.parseInt(port));
		System.out.println("sent message: "+send);
	}

	public void sendOK(String ip, String port) {

		int PORT = Integer.parseInt(port);
		String myIP = OwnIp;
		String myPort = String.valueOf(OwnPort);
		String myID = String.valueOf(ID);
		String send="OK,"+myIP+","+myPort+","+myID;
		this.sender.execute("ok", new Object[] { send }, ip, PORT);
		System.out.println("sent message: "+send);
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
		String send = "start,"+algorithm.ordinal();
		for (Node node : nodes) {
			this.sender.execute("start", new Object[] { send }, node.OwnIp, node.OwnPort);
		}
		this.start(algorithm);
	}

	public void start(Algorithm algorithm){
		System.out.println("Starting with algorithm: " + algorithm);
		this.algorithm = algorithm;
		// Reset values used during the distributed read/write
		this.nextRequestTime = -1;
		this.wordString = "";
		this.hasString = this.isMasterNode();
		this.servicedNode = null;
		this.awaitingFinalString = false;
		this.requestQueue.clear();
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
				this.requestAllForWordString(this.nextRequestTime);
			}
		}
	}
	
	/**
	 * Requests all nodes for access to the word string.
	 * 
	 * @param timeStamp
	 */
	private void requestAllForWordString(int timeStamp) {
		for (Node node : this.nodes) {
			this.requestWordString(node, timeStamp);
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
			System.out.println("receive REQUEST from " + port + " at " + timeStamp);
			if ((!this.isMasterNode() && (this.hasString || timeStamp >= this.nextRequestTime))) {
				System.out.println("queueing request from " + port);
				this.requestQueue.add(node);
			} else {
				this.sendWordStringOK(node, timeStamp);
			}
		}
	}
	
	/**
	 * Tell the requesting node that this node does not need the word string.
	 * 
	 * @param node
	 * @param timeStamp
	 */
	private void sendWordStringOK(Node node, int timeStamp) {
		System.out.println("Send OK to " + node.OwnPort);
		String send = String.format("str_request_ok,%s,%s,%d", this.OwnIp, this.OwnPort, timeStamp);
		this.sender.execute("strRequestOk", new Object[] { send }, node.OwnIp, node.OwnPort);
	}
	
	/**
	 * Receive "OK" message from a node.
	 * 
	 * @param ip
	 * @param port
	 * @param timeStamp
	 */
	public void receiveWordStringOK(String ip, int port, int timeStamp) {
		System.out.println("Receive OK from " + port + " at " + timeStamp);
		Node node = this.findNode(ip, port);
		if (node == null) {
			new Exception("Unknown address: " + ip + ":" + port).printStackTrace();
		}
		
		if (this.requestQueue.contains(node)) {
			new Exception("Duplicate node " + node).printStackTrace();
		}
		
		this.requestQueue.add(node);
		
		// We have access to the word string if all nodes respond with "OK"
		if (this.requestQueue.size() == this.nodes.size()) {
			this.requestQueue.clear();
			this.hasString = true;
			System.out.println("In CS");
			
			this.sender.execute("strRequestMaster", new Object[] { this.OwnIp + "," + this.OwnPort+ "," + timeStamp}, node.OwnIp, node.OwnPort);
		}
	}
	
	
	
	/**
	 * Requests for the master node's word string.
	 * 
	 * @return the master node's word string.
	 */
	public void requestWordString(Node node, int timeStamp) {
		if (this.algorithm == Algorithm.RICART_AGRAWALA)  {
			System.out.println("REQUEST to " + node.OwnPort + " for " + timeStamp);
		}
		
		String send="str_request,"+this.OwnIp+","+this.OwnPort+","+timeStamp;
		this.sender.execute("strRequest", new Object[] { send }, node.OwnIp, node.OwnPort);
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
				
				if (this.algorithm == Algorithm.RICART_AGRAWALA) {
					
					// Give up ownership of the word string.
					this.hasString = false;
					
					if (this.nextRequestTime < 20) {
						// Make the next request.
						this.requestAllForWordString(this.nextRequestTime);
					} else {
						System.out.println("Done");
					}
				}
			}
		}
	}

	public void requestFinalString(){
		System.out.println("Requesting final string");
		String send = "str_request_final,"+this.OwnIp+","+this.OwnPort;
		this.sender.execute("strRequestFinal", new Object[] { send }, this.masterNode.OwnIp, this.masterNode.OwnPort);
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
		newNode.OwnIp = ip;
		newNode.OwnPort = port;
		newNode.ID = ID;
		nodes.add(newNode);
		System.out.println("new node added. printing list...");
		printList();
	}

	/**
	 * Prints a list of all other nodes connected to the network.
	 */
	public void printList() {
		for (int i = 0; i < nodes.size(); i++) {
			System.out.println(i+1+" IP: "+nodes.get(i).OwnIp+" Port: "+nodes.get(i).OwnPort+" ID: "+nodes.get(i).ID);
		}
	}

	public void Display(){
		try{
			String IP=OwnIp;
			String port=String.valueOf(OwnPort);
			String message="My IP: "+IP+" Port: "+port+" ID: "+ID;
			System.out.println(message);
		}catch(Exception e){e.printStackTrace();}
	}	

	/**
	 * Checks whether a node contains the given network information.
	 * 
	 * @param ip
	 * @param port
	 * @return true if the node is in the network, otherwise false.
	 */
	public boolean checkInList(String ip, String port) {
		String myIP = OwnIp;
		String myPort = String.valueOf(OwnPort);
		if(myIP.equals(ip) & myPort.equals(port)) return true;
		if(nodes.size()>0) {
			System.out.println("checking if node already exists...");
			for (int i = 0; i < nodes.size(); i++) {
				String oldIP = nodes.get(i).OwnIp;
				String oldPort = String.valueOf(nodes.get(i).OwnPort);
				if(oldIP.equals(ip) & oldPort.equals(port)) return true;
			}
		}
		return false;
	}

	public int checkID(int ID) {
		if(ID==this.ID) {
			System.out.println("ID is the same as my ID...changing ID...");
			ID = (int) (Math.random() * (10000 - 0));
			ID = checkID(ID);
		}
		if(nodes.size()>0) {
			System.out.println("checking if ID is unique...");
			for (int i = 0; i < nodes.size(); i++) {
				int oldID = nodes.get(i).ID;
				if(ID==oldID) {
					System.out.println("ID isn't unique...changing ID...");
					ID = (int) (Math.random() * (10000 - 0));
					ID = checkID(ID);
				}
			}
		}
		return ID;
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
			if (node.OwnIp.equals(ip) && node.OwnPort == port) {
				// Node found.
				return node;
			}
		}
		// Check if requested node is this instance.
		if (this.OwnIp.equals(ip) && port == this.OwnPort) {
			return this;
		}
		System.out.println(this.OwnIp);
		// Node not found.
		return null;
	}

	public int findNodeIndex(int ID) {
		if(nodes.isEmpty()) {
			System.out.println("List is empty");
			return -1;
		}
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).ID == ID) {
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
		return this.isMasterNode();
	}

	/**
	 * Send a word string to the provided node.
	 */
	private void sendWordString(String value, Node destination) {
		String send="str_update,"+value;
		this.sender.execute("strUpdate", new Object[] { send }, destination);
	}

	public void setMasterNode(Node Winner) {
		if(Winner==null) {
			System.out.println("No master node for the time being!");
			return;
		}
		System.out.println("====Master node elected====");
		Winner.Display();
		System.out.println("===========================");
		this.masterNode = Winner;
	}

	public void election(){
		election elect = new election();
		new Thread(elect).start();
	}

	public void advert() {
		for (int i = 0; i < nodes.size(); i++) {
			String myIP = this.OwnIp;
			String myPort = String.valueOf(this.OwnPort);
			String myID = String.valueOf(this.ID);
			String send="MASTER,"+myIP+","+myPort+","+myID;
			String oldIP = Global.node.nodes.get(i).OwnIp;
			int oldPort = Global.node.nodes.get(i).OwnPort;
			this.sender.execute("master", new Object[] { send }, oldIP, oldPort);
			System.out.println("sent message: "+send);
		}
	}

	public void signOff() {
		for (int i = 0; i < nodes.size(); i++) {
			String myID = String.valueOf(this.ID);
			String send="signOff,"+myID;
			String oldIP = Global.node.nodes.get(i).OwnIp;
			int oldPort = Global.node.nodes.get(i).OwnPort;
			this.sender.execute("signOff", new Object[] { send }, oldIP, oldPort);
			System.out.println("sent message: "+send);
		}
	}

	/**
	 * Tells all nodes to move their logical clocks one step forward.
	 */
	public void sendTimeAdvance(int logicalTime) {
		if (!this.isMasterNode()) {
			return;
		}
		
		String send = "time_advance,"+logicalTime;
		for (Node node : nodes) {
			this.sender.execute("timeAdvance", new Object[] { send }, node);
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
		return this.OwnIp.equals(node.OwnIp) && this.OwnPort == node.OwnPort;
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
		} else {
			Node node = this.requestQueue.poll();
			if (node != null) {
				System.out.println("Send OK to " + node.OwnPort);
				this.servicedNode = node;
				this.sendWordStringOK(node, -1);
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
				Global.node = new Node();
				Global.node.create(matcher.group(1), port);
				Incomming p=new Incomming();
				new Thread(p).start();
				Reading q = new Reading();
				new Thread(q).start();
				Global.node.Display();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}	
	}
}