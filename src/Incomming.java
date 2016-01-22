import java.util.StringTokenizer;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Incomming implements Runnable {
	private Thread timeAdvanceGrant;
	
	public void join(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if(function.equals("join")){
			String IP=token.nextToken();
			String PORT=token.nextToken();
			String senderIP=token.nextToken();
			String senderPORT=token.nextToken();
			String senderID = token.nextToken();
			System.out.println("msg received: "+IP+","+PORT+","+senderIP+","+senderPORT+","+senderID);
			int sID = Integer.parseInt(senderID);
			if(!Global.node.checkInList(senderIP, senderPORT)) {
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
	}
	
	public void newID(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if(function.equals("newID")) {
			String newID = token.nextToken();
			int ID = Integer.parseInt(newID);
			System.out.println("changing my ID to: "+newID);
			Global.node.ID = ID;
			Global.node.Display();
		}
	}
	
	public void newNode(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if(function.equals("new")){
			String IP=token.nextToken();
			String PORT=token.nextToken();
			String senderID = token.nextToken();
			int sID = Integer.parseInt(senderID);
			System.out.println("new node: "+IP+","+PORT+","+senderID);
			System.out.println("adding new node to List");
				Global.node.addNodeToList(IP, Integer.parseInt(PORT), sID);
		}
	}
	
	public void master (String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if(function.equals("MASTER")) {
			String masterIp = token.nextToken();
			String masterPort = token.nextToken();
			String masterID = token.nextToken();
			Node master = new Node();
			master.OwnIp = masterIp;
			master.OwnPort = Integer.parseInt(masterPort);
			master.ID = Integer.parseInt(masterID);
			Global.node.setMasterNode(master);
		}
	}
	
	public void ok(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if(function.equals("OK")) {
			String IP=token.nextToken();
			String PORT=token.nextToken();
			String senderID = token.nextToken();
			System.out.println("msg received: OK from "+IP+","+PORT+","+senderID);
			Global.node.responded = true;
		}
	}
	
	public void election(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if(function.equals("ELECTION")) {
			String IP=token.nextToken();
			String PORT=token.nextToken();
			String senderID = token.nextToken();
			System.out.println("msg received: ELECTION from "+IP+","+PORT+","+senderID);
			if(Integer.parseInt(senderID)<Global.node.ID) {
				Global.node.sendOK(IP,PORT);
				System.out.println("My ID is higher so I'll start my own election!");
				Global.node.election();
			}
		}
	}
	
	public void signOff(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if(function.equals("signOff")) {
			int senderID = Integer.parseInt(token.nextToken());					
			int index = Global.node.findNodeIndex(senderID);
			if(index==-1) {
				System.out.println("Node not in list. So no sign off!");
			}
			else {//remove node from list
				Global.node.nodes.remove(index);
				System.out.println("Node ID: "+senderID+" removed!");
			}
		}
	}
	
	public void start(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if(function.equals("start")) {
			Global.node.start(Algorithm.values()[Integer.valueOf(token.nextToken())]);
		}
	}
	
	public void strRequest(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if (function.equals("str_request")) {
			String ip = token.nextToken();
			int port = Integer.valueOf(token.nextToken());
			int timeStamp = Integer.valueOf(token.nextToken());
			Global.node.receiveWordStringRequest(ip, port, timeStamp);
		}
	}
	
	public void strRequestMaster(String msg) {
		StringTokenizer token = new StringTokenizer(msg,",");
		String ip = token.nextToken();
		int port = Integer.valueOf(token.nextToken());
		Global.node.sendWordString(ip, port);
	}
	
	public void strRequestOk(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if (function.equals("str_request_ok")) {
			String ip = token.nextToken();
			int port = Integer.valueOf(token.nextToken());
			Global.node.receiveWordStringOK(ip, port);
		}
	}
	
	public void timeAdvance(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if (function.equals("time_advance")) {
			int time = Integer.valueOf(token.nextToken());
			this.timeAdvanceGrant = new Thread(new TimeAdvanceGrant(Global.node, time));
			this.timeAdvanceGrant.start();
		}
	}
	
	public void strRequestFinal(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if (function.equals("str_request_final")) {
			String ip = token.nextToken();
			String port = token.nextToken();

			Global.node.sendFinalString(ip, Integer.valueOf(port));
		}
	}
	
	public void strUpdate(String msg) {
		StringTokenizer token=new StringTokenizer(msg,",");
		String function= token.nextToken();
		if (function.equals("str_update")) {
			String wordString = token.hasMoreTokens() ? token.nextToken() : "";
			Global.node.receiveWordString(wordString);
		}
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
