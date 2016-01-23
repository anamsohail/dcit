import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Incomming implements Runnable {
	private Thread timeAdvanceGrant;
	
	public void join(String ip, int port, String senderIP, String senderPORT, int sID) {
		if(!Global.node.checkInList(senderIP, String.valueOf(senderPORT))) {
			int newID = Global.node.checkID(sID);
			if(sID==newID) {
				System.out.println("ID unique...joining node!");
			}
			else {
				System.out.println("ID wasn't unique. Sending new ID to new node!");
				Global.node.sendIDtoNewNode(senderIP, senderPORT, newID);
			}
			System.out.println("joined: "+senderIP+","+senderPORT);
			Global.node.sendToAll(senderIP,senderPORT,newID);
		}
		else {
			System.out.println("Node already in network!");
		}
		
		Global.node.isJoined = true;
	}
	
	public void newID(int ID) {
		System.out.println("changing my ID to: "+ID);
		Global.node.ID = ID;
		Global.node.Display();
	}
	
	public void newNode(String IP, int PORT, int sID) {
		System.out.println("adding new node to List");
		Global.node.addNodeToList(IP, PORT, sID);
	}
	
	public void master (String masterIp, int masterPort, int masterID) {
		Node master = new Node();
		master.OwnIp = masterIp;
		master.OwnPort = masterPort;
		master.ID = masterID;
		Global.node.setMasterNode(master);
	}
	
	public void ok(String ip, int port, int id) {
		System.out.println("msg received: OK from "+ip+","+port+","+id);
		Global.node.responded = true;
	}
	
	public void election(String ip, int port, int id) {
		System.out.println("msg received: ELECTION from "+ip+","+port+","+id);
		if(id < Global.node.ID) {
			Global.node.sendOK(ip, port);
			System.out.println("My ID is higher so I'll start my own election!");
			Global.node.election();
		}
	}
	
	public void signOff(int senderID) {				
		int index = Global.node.findNodeIndex(senderID);
		if(index==-1) {
			System.out.println("Node not in list. So no sign off!");
		}
		else {//remove node from list
			Global.node.nodes.remove(index);
			System.out.println("Node ID: "+senderID+" removed!");
		}
	}
	
	public void start(int algorithmOrdinal) {
		Global.node.start(Algorithm.values()[Integer.valueOf(algorithmOrdinal)]);
	}
	
	public void strRequest(String ip, int port, int timeStamp) {
		Global.node.receiveWordStringRequest(ip, port, timeStamp);
	}
	
	public void strRequestMaster(String ip, int port) {
		Global.node.sendWordString(ip, port);
	}
	
	public void strRequestOk(String ip, int port, int timeStamp) {
		Global.node.receiveWordStringOK(ip, port, timeStamp);
	}
	
	public void timeAdvance(int time) {
		this.timeAdvanceGrant = new Thread(new TimeAdvanceGrant(Global.node, time));
		this.timeAdvanceGrant.start();
	}
	
	public void strRequestFinal(String ip, int port) {
		Global.node.sendFinalString(ip, port);
	}
	
	public void strUpdate(String value) {
		Global.node.receiveWordString(value);
	}
	
	@Override
	public void run() {
		try{
			WebServer webServer = new WebServer(Global.node.OwnPort);
			XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
			PropertyHandlerMapping phm = new PropertyHandlerMapping();
			phm.setVoidMethodEnabled(true);
			phm.addHandler("receiver", Incomming.class);
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
