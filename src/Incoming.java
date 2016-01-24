import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Incoming implements Runnable {
	public static Node NODE;
	
	public void join(String ip, int port, String senderIP, String senderPORT, int sID) {
		System.out.println(ip + " " + port + " " + senderIP + " " + senderPORT + " " + sID);
		if(!NODE.checkInList(senderIP, senderPORT)) {
			int newID = NODE.checkID(sID);
			if(sID==newID) {
				System.out.println("ID unique...joining node!");
			}
			else {
				System.out.println("ID wasn't unique. Sending new ID to new node!");
				NODE.sendIDtoNewNode(senderIP, senderPORT, newID);
			}
			System.out.println("joined: "+senderIP+","+senderPORT);
			NODE.sendToAll(senderIP,senderPORT,newID);
		}
		else {
			System.out.println("Node already in network!");
		}
		
		NODE.isJoined = true;
	}
	
	public void newID(int ID) {
		System.out.println("changing my ID to: "+ID);
		NODE.id = ID;
		System.out.println(NODE);
	}
	
	public void newNode(String IP, int PORT, int sID) {
		System.out.println("adding new node to List");
		NODE.addNodeToList(IP, PORT, sID);
	}
	
	public void master (String masterIp, int masterPort, int masterID) {
		Node master = new Node();
		master.ip = masterIp;
		master.port = masterPort;
		master.id = masterID;
		NODE.setMasterNode(master);
	}
	
	public void ok(String ip, int port, int id) {
		System.out.println("msg received: OK from "+ip+","+port+","+id);
		NODE.responded = true;
	}
	
	public void election(String ip, int port, int id) {
		System.out.println("msg received: ELECTION from "+ip+","+port+","+id);
		if(id < NODE.id) {
			NODE.sendOK(ip, port);
			System.out.println("My ID is higher so I'll start my own election!");
			new Thread(new Election(NODE)).start();
		}
	}
	
	public void signOff(int senderID) {				
		int index = NODE.findNodeIndex(senderID);
		if(index==-1) {
			System.out.println("Node not in list. So no sign off!");
		}
		else {//remove node from list
			NODE.nodes.remove(index);
			System.out.println("Node ID: "+senderID+" removed!");
		}
	}
	
	public void start(int algorithmOrdinal) {
		NODE.start(Algorithm.values()[Integer.valueOf(algorithmOrdinal)]);
	}
	
	public void strRequest(int requesterId, int timeStamp) {
		NODE.distReadWrite.receiveWordStringRequest(requesterId, timeStamp);
	}
	
	public void strRequestMaster(int requesterId) {
		NODE.distReadWrite.sendWordString(requesterId);
	}
	
	public void strRequestOk(int senderId, int timeStamp) {
		((RicartAgrawala)NODE.distReadWrite).receiveWordStringOK(senderId, timeStamp);
	}
	
	public void timeAdvance(int time) {
		NODE.distReadWrite.receiveTimeAdvanceGrant(time);
	}
	
	public void strRequestFinal(int requesterId) {
		NODE.distReadWrite.sendFinalString(requesterId);
	}
	
	public void strUpdate(String value) {
		NODE.distReadWrite.receiveWordString(value);
	}
	
	@Override
	public void run() {
		try{
			WebServer webServer = new WebServer(NODE.port);
			XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
			PropertyHandlerMapping phm = new PropertyHandlerMapping();
			phm.setVoidMethodEnabled(true);
			phm.addHandler("receiver", Incoming.class);
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
