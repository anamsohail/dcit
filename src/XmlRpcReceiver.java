import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

/**
 * Class responsible for receiving XML-RPC messages.
 */
public class XmlRpcReceiver implements Runnable {
	public static Node NODE;
	
	/**
	 * Called when a node wants to join the network.
	 * 
	 * Called to a node that is either already part of a network or
	 * when no network has been yet established.
	 * 
	 * @param ip
	 * @param port
	 * @param id
	 */
	public void joinRequest(String ip, int port, int id) {
		NODE.acceptJoinRequest(ip, port, id);
	}
	
	/**
	 * Called when a node needs to change its ID.
	 * 
	 * @param ID
	 */
	public void idUpdate(int ID) {
		System.out.println("changing my ID to: "+ID);
		NODE.id = ID;
		System.out.println(NODE);
	}
	
	/**
	 * Informs this node that a new node has joined the network.
	 * 
	 * Information was sent by a node that was already in the network.
	 * 
	 * @param ip
	 * @param port
	 * @param id
	 */
	public void nodeJoined(String ip, int port, int id) {
		System.out.println("New node joined.");
		NODE.nodes.add(new Node(ip, port, id));
		NODE.printNodeList();
	}
	
	/***
	 * Announces the master node.
	 * 
	 * @param masterID
	 */
	public void masterNodeAnnouncement(int masterID) {
		Node master = null;
		for (Node node : NODE.nodes) {
			if (node.id == masterID) {
				master = node;
			}
		}
		
		NODE.setMasterNode(master);
	}
	
	/**
	 * Part of the bully algorithm.  Called when a node has a higher ID.
	 * 
	 * @param ip
	 * @param port
	 * @param id
	 */
	public void electionResponse(String ip, int port, int id) {
		System.out.println("msg received: OK from "+ip+","+port+","+id);
		NODE.responded = true;
	}
	
	/**
	 * Start an election as part of the bully algorithm.
	 * 
	 * @param ip
	 * @param port
	 * @param id
	 */
	public void startElection(String ip, int port, int id) {
		System.out.println("msg received: ELECTION from "+ip+","+port+","+id);
		if(id < NODE.id) {
			NODE.sendOK(ip, port);
			System.out.println("My ID is higher so I'll start my own election!");
			new Thread(new Election(NODE)).start();
		}
	}
	
	/**
	 * Called when a node leaves the network.
	 * 
	 * @param senderID
	 */
	public void nodeSignOff(int senderID) {				
		int index = NODE.findNodeIndex(senderID);
		if(index==-1) {
			System.out.println("Node not in list. So no sign off!");
		}
		else {//remove node from list
			NODE.nodes.remove(index);
			System.out.println("Node ID: "+senderID+" removed!");
		}
	}
	
	/**
	 * Called when a node has started the distributed read-write operations.
	 * 
	 * @param algorithmOrdinal
	 */
	public void startDistributedReadWrite(int algorithmOrdinal) {
		NODE.start(Algorithm.values()[Integer.valueOf(algorithmOrdinal)]);
	}
	
	/**
	 * Called when a node is requesting the word string.
	 * 
	 * @param requesterId
	 * @param timeStamp
	 */
	public void wordStringRequest(int requesterId, int timeStamp) {
		NODE.distReadWrite.receiveWordStringRequest(requesterId, timeStamp);
	}
	
	/**
	 * Called when a node is directly asking the master node for the word string.
	 * 
	 * @param requesterId
	 */
	public void wordStringRequestToMasterNode(int requesterId) {
		NODE.distReadWrite.sendWordString(requesterId);
	}
	
	/**
	 * Called when a node has responded "OK" to a resource request.
	 * 
	 * Part of the Ricart-Agrawala algorithm.
	 * 
	 * @param senderId
	 * @param timeStamp
	 */
	public void wordStringResponse(int senderId, int timeStamp) {
		((RicartAgrawala)NODE.distReadWrite).receiveWordStringOK(senderId, timeStamp);
	}
	
	/**
	 * Called when a timer has sent a new clock time.
	 * 
	 * @param time
	 */
	public void timeAdvance(int time) {
		NODE.distReadWrite.receiveTimeAdvanceGrant(time);
	}
	
	/**
	 * Called when a node wants the final word string from the master node.
	 * 
	 * @param requesterId
	 */
	public void finalWordStringRequest(int requesterId) {
		NODE.distReadWrite.sendFinalString(requesterId);
	}
	
	/**
	 * Called when a node has sent the updated word string.
	 * 
	 * @param value
	 */
	public void wordStringUpdate(String value) {
		NODE.distReadWrite.receiveWordString(value);
	}
	
	@Override
	public void run() {
		try{
			WebServer webServer = new WebServer(NODE.port);
			XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
			PropertyHandlerMapping phm = new PropertyHandlerMapping();
			phm.setVoidMethodEnabled(true);
			phm.addHandler("receiver", XmlRpcReceiver.class);
			xmlRpcServer.setHandlerMapping(phm);
			XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl)
					xmlRpcServer.getConfig();
			serverConfig.setEnabledForExtensions(true);
			serverConfig.setContentLengthOptional(false);
			webServer.start();
		} catch(Exception e){
			e.printStackTrace();}
	}
}
