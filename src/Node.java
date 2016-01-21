import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node {
	public boolean responded = false;
	public ArrayList<Node> nodes = new ArrayList<Node>();
	public List<String> appended = new ArrayList<String>();
	public boolean isJoined = false;

	public DatagramSocket sendsocket;
	public Algorithm algorithm;
	public InetAddress OwnIp;
	public int nextRequestTime;
	public int OwnPort;
	public int ID;

	private static String USAGE = "Usage: Node.java <port>";
	private Queue<Node> requestQueue = new LinkedList<Node>();
	private List<Node> doneNodes = new ArrayList<Node>();
	
	private Node servicedNode;
	private String wordString;
	private Node masterNode;
	private Thread timer;

	public void create(String ip,int port){
		try{
			OwnIp = InetAddress.getByName(ip);
			sendsocket = new DatagramSocket();
			OwnPort = port;
			ID = (int) (Math.random() * (10000 - 0));
		}catch(Exception e){e.printStackTrace();}
	}

	/**
	 * Joins a network by connecting to a node that
	 * is already in the network.
	 * 
	 * @param Ip
	 * @param port
	 * @param myPort
	 */
	public void join(InetAddress Ip,int port, int myPort){
		try{
			String IpPort=Ip.getHostAddress()+","+port+","+myPort+","+this.ID;
			String send="join,"+IpPort;
			byte[] buffer=send.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, Ip, port);
			sendsocket.send(packet);
			this.isJoined = true;
			System.out.println("sent message: "+send);
		}catch(Exception e ){e.printStackTrace();}	
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

		byte[] buffer = ("start," + algorithm.ordinal()).getBytes();
		for (Node node : nodes) {
			try {
				this.sendsocket.send(new DatagramPacket(buffer, buffer.length, node.OwnIp, node.OwnPort));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.start(algorithm);
	}

	public void start(Algorithm algorithm){
		System.out.println("Starting with algorithm: " + algorithm);
		this.algorithm = algorithm;
		
		// Reset values used during the distributed read/write
		this.nextRequestTime = -1;
		this.wordString = "";
		this.servicedNode = null;
		this.appended.clear();
		this.requestQueue.clear();
		this.doneNodes.clear();
		this.timer = new Thread(new Timer());
		
		if (this.masterNode.equals(this)) {
			this.timer.start();
		} else {
			new TimeAdvanceGrant(this, 0).run();
		}
	}
	/**
	 * Gets the master node's word string.
	 * 
	 * @return the master node's word string.
	 */
	public String getMasterString() {
		byte[] buffer = String.format("str_request,%s,%s", this.getIpString(), this.OwnPort).getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.masterNode.OwnIp, this.masterNode.OwnPort);

		try {
			this.sendsocket.send(packet);

			// Wait for updated value (see Node::unlockWordString).
			synchronized(this.wordString) {
				this.wordString.wait();
			}

			return this.wordString;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Called by the node that has finished adding a new word
	 * to the word string.
	 * 
	 * @param value
	 */
	public void sendWordStringToMaster(String value){
		this.sendWordString(value, this.masterNode);
	}

	/**
	 * Send this node's word string to the specified address.
	 * 
	 * @param ip
	 * @param port
	 */
	public void sendWordString(String ip, int port) {
		Node node = this.findNode(ip, port);
		if (node != null) {
			if (this.algorithm == Algorithm.CENTRALIZED_MUTUAL_EXCLUSION && this.masterNode.equals(this)) {
				System.out.println("Receive request from " + node);
				this.requestQueue.add(node);
				this.checkRequestQueue();
			} else {
				this.sendWordString(this.wordString, node);
			}
		} else {
			Exception e = new Exception("Node not found: " + ip + ":" + port);
			e.printStackTrace();
		}
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
	 * Called when a node receives the word string
	 * from the master node.
	 * 
	 * Allows code in Node::getMasterString to proceed.
	 * 
	 * @param value
	 */
	public void unlockWordString(String value) {
		synchronized(this.wordString) {
			this.wordString.notify();
			this.wordString = value;
		}
		
		if (this.algorithm == Algorithm.CENTRALIZED_MUTUAL_EXCLUSION) {
			if (this.masterNode.equals(this)) {
				System.out.println("Finished servicing " + this.servicedNode);
				synchronized (this.servicedNode) {
					this.servicedNode = null;
				}
				
				this.checkRequestQueue();
			}
		}
	}
	
	public String requestFinalString(){
		byte[] buffer = String.format("str_request_final,%s,%s", this.getIpString(), this.OwnPort).getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.masterNode.OwnIp, this.masterNode.OwnPort);

		try {
			this.sendsocket.send(packet);

			// Wait for updated value (see Node::unlockWordString).
			synchronized(this.wordString) {
				this.wordString.wait();
			}

			return this.wordString;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Stores a new node's information in a list
	 * of all nodes in the network.
	 * 
	 * @param ip
	 * @param port
	 */
	public void addNodeToList(InetAddress ip, int port, int ID) {
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
			String IP=OwnIp.getHostAddress();
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
		String myIP = OwnIp.toString();
		myIP = myIP.replaceAll("[/]","");
		String myPort = String.valueOf(OwnPort);
		if(myIP.equals(ip) & myPort.equals(port)) return true;
		if(nodes.size()>0) {
			System.out.println("checking if node already exists...");
			for (int i = 0; i < nodes.size(); i++) {
				String oldIP = nodes.get(i).OwnIp.toString();
				oldIP = oldIP.replaceAll("[/]","");
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
			if (node.getIpString().equals(ip) && node.OwnPort == port) {
				// Node found.
				return node;
			}
		}

		// Check if requested node is this instance.
		if (this.getIpString().equals(ip) && port == this.OwnPort) {
			return this;
		}

		System.out.println(this.getIpString());
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

	/**
	 * Extract the IP address from the format [hostname]/[IP address]
	 */
	public String getIpString() {
		Matcher matcher = Pattern.compile(".*/(.*)").matcher(this.OwnIp.toString());
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return this.OwnIp.toString();
		}
	}

	/**
	 * Send a word string to the provided node.
	 */
	private void sendWordString(String value, Node destination) {

		byte[] buffer = String.format("str_update,%s", value).getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destination.OwnIp, destination.OwnPort);

		try {
			this.sendsocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			String myIP = this.OwnIp.toString();
			myIP = myIP.replaceAll("[/]","");
			String myPort = String.valueOf(this.OwnPort);
			String myID = String.valueOf(this.ID);
			String send="MASTER,"+myIP+","+myPort+","+myID;
			byte[] buffer=send.getBytes();
			InetAddress oldIP = Global.node.nodes.get(i).OwnIp;
			int oldPort = Global.node.nodes.get(i).OwnPort;
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, oldIP, oldPort);
			try {
				Global.node.sendsocket.send(packet);
			} catch (IOException e) {e.printStackTrace();}
			System.out.println("sent message: "+send);
		}
	}

	public void signOff() {
		for (int i = 0; i < nodes.size(); i++) {
			String myID = String.valueOf(this.ID);
			String send="signOff,"+myID;
			byte[] buffer=send.getBytes();
			InetAddress oldIP = Global.node.nodes.get(i).OwnIp;
			int oldPort = Global.node.nodes.get(i).OwnPort;
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, oldIP, oldPort);
			try {
				Global.node.sendsocket.send(packet);
			} catch (IOException e) {e.printStackTrace();}
			System.out.println("sent message: "+send);
		}
	}
	
	/**
	 * Tells all nodes to move their logical clocks one step forward.
	 */
	public void sendTimeAdvance(int logicalTime) {
		if (!this.masterNode.equals(this)) {
			return;
		}
		
		byte[] buffer = ("time_advance," + logicalTime).getBytes();
		for (Node node : nodes) {
			try {
				this.sendsocket.send(new DatagramPacket(buffer, buffer.length, node.OwnIp, node.OwnPort));
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		return this.getIpString().equals(node.getIpString()) && this.OwnPort == node.OwnPort;
	}
	
	@Override
	public String toString() {
		return "[" + this.getIpString() + ":" + this.OwnPort + "]";
	}
	
	private void checkRequestQueue() {
		// Only the master node should run this code.
		if (!this.masterNode.equals(this)) {
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
	
	public class Timer implements Runnable {

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
				Node.this.sendTimeAdvance(logicalTime);
				
			}
			
		}
	}
}