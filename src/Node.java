import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node {
	public String ip = "";
	public int port = -1;
	public int id = new Random().nextInt(10000);
	public ArrayList<Node> nodes = new ArrayList<Node>();
	public boolean responded = false;
	public Sender sender = new Sender();
	public boolean isJoined = false;
	private Node masterNode;
	public DistributedReadWrite distReadWrite;
	private static String USAGE = "Usage: Node.java <port>";
	
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
			this.sender.execute("joinRequest", new Object[] { this.ip, this.port, this.id }, ip, port);
			this.isJoined = true;
		}catch(Exception e ){e.printStackTrace();}	
	}

	public void sendIdToNewNode(String ip, int port, int ID) {
		this.sender.execute("idUpdate", new Object[] { ID }, ip, port);
	}

	public void sendToAll(String ip, int port, int newID) {
		if(nodes.size()>0) {
			System.out.println("sending new node to all other nodes!");
			for (Node node : this.nodes) {
				this.sender.execute("nodeJoined", new Object[] { ip, port, newID }, node.ip, node.port);
			}
		}
		this.sendListToNewNode(ip,port);
		System.out.println("adding new node to List");
		addNodeToList(ip, port, newID);
	}

	public void sendListToNewNode(String ip, int port) {
		if(nodes.size()>0) {
			System.out.println("sending list to new node!");
			for (Node node : this.nodes) {
				this.sender.execute("nodeJoined", new Object[] { node.ip, node.port, node.id }, ip, port);
			}
		}
		System.out.println("Sending my info to new node...");
		this.sender.execute("nodeJoined", new Object[] { this.ip, this.port, this.id }, ip, port);
	}

	public void sendOK(String ip, int port) {
		this.sender.execute("electionResponse", new Object[] { this.ip, this.port, this.id }, ip, port);
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
			this.sender.execute("startDistributedReadWrite", new Object[] { algorithm.ordinal() }, node.ip, node.port);
		}
		
		this.start(algorithm);
	}

	public void start(Algorithm algorithm){
		this.distReadWrite = DistributedReadWrite.create(this, algorithm);
		this.distReadWrite.start();
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
	public boolean checkInList(String ip, int port) {
		if(this.ip.equals(ip) & this.port == port)
			return true;
		
		if(nodes.size()>0) {
			System.out.println("checking if node already exists...");
			
			for (Node node : this.nodes) {
				return node.ip.equals(ip) && node.port == port;
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
	
	public boolean isMasterNode() {
		return this.masterNode.equals(this);
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