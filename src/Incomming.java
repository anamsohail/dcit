import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class Incomming implements Runnable {

	@Override
	public void run() {
		try{
			DatagramSocket receivingsocket=new DatagramSocket(Global.node.OwnPort);
			Thread distributiveReadWrite = new Thread(new DistributedReadWrite(Global.node));
			while(true){
				byte[] b=new byte [250];
				DatagramPacket packet= new DatagramPacket(b,b.length);
				receivingsocket.receive(packet);
				byte[] buffer = packet.getData();
				String s=new String(buffer);
				s=s.trim();
				StringTokenizer token=new StringTokenizer(s,",");
				String function= token.nextToken();

				if(function.equals("join")){
					String IP=token.nextToken();
					String PORT=token.nextToken();
					String senderPORT=token.nextToken();
					String senderID = token.nextToken();
					System.out.println("msg received: "+IP+","+PORT+","+senderPORT+","+senderID);
					int sID = Integer.parseInt(senderID);
					String newIP = packet.getAddress().toString();
					newIP = newIP.replaceAll("[/]","");
					if(!Global.node.checkInList(newIP, senderPORT)) {
						int newID = Global.node.checkID(sID);
						if(sID==newID) {
							System.out.println("ID unique...joining node!");
						}
						else {
							System.out.println("ID wasn't unique. Sending new ID to new node!");
							sendIDtoNewNode(newIP, senderPORT, newID);
						}
						System.out.println("joined: "+newIP+","+senderPORT);
						sendToAll(newIP,senderPORT,newID);
					}
					else {
						System.out.println("Node already in network!");
					}
				}
				
				if(function.equals("newID")) {
					String newID = token.nextToken();
					int ID = Integer.parseInt(newID);
					System.out.println("changing my ID to: "+newID);
					Global.node.ID = ID;
					Global.node.Display();
				}

				if(function.equals("new")){
					String IP=token.nextToken();
					String PORT=token.nextToken();
					String senderID = token.nextToken();
					int sID = Integer.parseInt(senderID);
					System.out.println("new node: "+IP+","+PORT+","+senderID);
					System.out.println("adding new node to List");
					try {
						Global.node.addNodeToList(InetAddress.getByName(IP), Integer.parseInt(PORT), sID);
					} catch (NumberFormatException | UnknownHostException e) {e.printStackTrace();}
				}

				if(function.equals("start")) {
					System.out.println("Starting...");
					distributiveReadWrite.start();
				}

				if (function.equals("str_request")) {
					String ip = token.nextToken();
					String port = token.nextToken();
					Global.node.sendWordString(ip, Integer.valueOf(port));
				}

				if (function.equals("str_update")) {
					String wordString = token.hasMoreTokens() ? token.nextToken() : "";
					Global.node.unlockWordString(wordString);
				}

			}
		} catch (BindException be) {
			// Do nothing.
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public void sendToAll(String ip, String port, int newID) {
		ip = ip.replaceAll("[/]","");
		if(Global.node.nodes.size()>0) {
			System.out.println("sending new node to all other nodes!");
			for (int i = 0; i < Global.node.nodes.size(); i++) {//send new node to already existing nodes
				String IpPort=ip+","+port+","+String.valueOf(newID);
				String send="new,"+IpPort;
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
		sendListToNewNode(ip,port);
		System.out.println("adding new node to List");
		try {
			Global.node.addNodeToList(InetAddress.getByName(ip), Integer.parseInt(port), newID);
		} catch (NumberFormatException | UnknownHostException e) {e.printStackTrace();}
	}

	public void sendListToNewNode(String ip, String port) {
		InetAddress IP;
		try {
			IP = InetAddress.getByName(ip);
			int PORT = Integer.parseInt(port);
			
			if(Global.node.nodes.size()>0) {
				System.out.println("sending list to new node!");
				for (int i = 0; i < Global.node.nodes.size(); i++) {//send nodes one by one to new node
					String oldIP = Global.node.nodes.get(i).OwnIp.toString();
					oldIP = oldIP.replaceAll("[/]","");
					String oldPort = String.valueOf(Global.node.nodes.get(i).OwnPort);
					String oldID = String.valueOf(Global.node.nodes.get(i).ID);
					String IpPort=oldIP+","+oldPort+","+oldID;
					String send="new,"+IpPort;
					byte[] buffer=send.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IP, PORT);
					try {
						Global.node.sendsocket.send(packet);} 
					catch (IOException e) {e.printStackTrace();}
					System.out.println("sent message: "+send);
				}
			}
			
			System.out.println("Sending my info to new node...");
			String myIP = Global.node.OwnIp.toString();
			myIP = myIP.replaceAll("[/]","");
			String myPort = String.valueOf(Global.node.OwnPort);
			String myID = String.valueOf(Global.node.ID);
			String IpPort=myIP+","+myPort+","+myID;
			String send="new,"+IpPort;
			byte[] buffer=send.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IP, PORT);
			try {
				Global.node.sendsocket.send(packet);} 
			catch (IOException e) {e.printStackTrace();}
			System.out.println("sent message: "+send);
		} catch (UnknownHostException e1) {e1.printStackTrace();}
	}

	public void sendIDtoNewNode(String ip, String port, int ID) {
		InetAddress IP;
		try {
			IP = InetAddress.getByName(ip);
			int PORT = Integer.parseInt(port);
			String send="newID,"+String.valueOf(ID);
			byte[] buffer=send.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IP, PORT);
			try {
				Global.node.sendsocket.send(packet);} 
			catch (IOException e) {e.printStackTrace();}
			System.out.println("sent message: "+send);
		} catch (UnknownHostException e1) {e1.printStackTrace();}
	}

}
