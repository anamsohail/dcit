import java.io.IOException;
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
			while(true){
				byte[] b=new byte [50];
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
					System.out.println("msg received: "+IP+","+PORT+","+senderPORT);
					String newIP = packet.getAddress().toString();
					newIP = newIP.replaceAll("[/]","");
					System.out.println("joined: "+newIP+","+senderPORT);
					sendToAll(newIP,senderPORT);
				}

				if(function.equals("new")){
					String IP=token.nextToken();
					String PORT=token.nextToken();
					System.out.println("new node: "+IP+","+PORT);
					System.out.println("adding new node to List");
					addNodeToList(IP,PORT);
				}

			}
		}catch(Exception e){System.out.println("exception");}			
	}
	public void sendToAll(String ip, String port) {
		ip = ip.replaceAll("[/]","");
		if(Global.node.nodes.size()>0) {
			System.out.println("sending new node to all other nodes!");
			for (int i = 0; i < Global.node.nodes.size(); i++) {//send new node to already existing nodes
				String IpPort=ip+","+port;
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
			sendListToNewNode(ip,port);
		}
		System.out.println("adding new node to List");
		addNodeToList(ip,port);
	}
	public void addNodeToList(String ip, String port) {
		//add new node to the list
		Node newNode = new Node();
		ip = ip.replaceAll("[/]","");
		try {
			newNode.OwnIp = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {e.printStackTrace();}
		newNode.OwnPort = Integer.parseInt(port);
		Global.node.nodes.add(newNode);
		System.out.println("new node added. printing list...");
		Global.node.printList();
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
					String IpPort=oldIP+","+oldPort;
					String send="new,"+IpPort;
					byte[] buffer=send.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IP, PORT);
					try {
						Global.node.sendsocket.send(packet);} 
					catch (IOException e) {e.printStackTrace();}
					System.out.println("sent message: "+send);
				}
			}
		} catch (UnknownHostException e1) {e1.printStackTrace();}
	}
}
