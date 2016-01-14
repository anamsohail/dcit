import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
//how to join: join,ip,port,myport
public class Node {
	public InetAddress OwnIp;
	public int OwnPort; 
	public DatagramSocket sendsocket;
	public ArrayList<Node> nodes = new ArrayList<Node>();

	public void create(String ip,int port){
		try{
			OwnIp=InetAddress.getByName(ip);
			sendsocket=new DatagramSocket();
			OwnPort=port;
		}catch(Exception e){System.out.println("Exception");}
	}

	public void join(InetAddress Ip,int port, int myPort){
		try{
			String IpPort=Ip.getHostAddress()+","+port+","+myPort;
			String send="join,"+IpPort;
			byte[] buffer=send.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, Ip, port);
			sendsocket.send(packet);
			System.out.println("sent message: "+send);
		}catch(Exception e ){System.out.println("exception");}	
	}

	public void addNodeToList(InetAddress ip, int port) {
		//add new node to the list
		Node newNode = new Node();
		newNode.OwnIp = ip;
		newNode.OwnPort = port;
		nodes.add(newNode);
		System.out.println("new node added. printing list...");
		printList();
	}

	public void printList() {
		for (int i = 0; i < nodes.size(); i++) {
			System.out.println(i+" IP: "+nodes.get(i).OwnIp+" Port: "+nodes.get(i).OwnPort);
		}
	}

	public void Display(){
		try{
			String IP=OwnIp.getHostAddress();
			String port=String.valueOf(OwnPort);
			String message="My IP is "+IP+" my port is "+port;
			System.out.println(message);
		}catch(Exception e){System.out.println("exception");}
	}	
	
	public boolean checkInList(String ip, String port) {
		if(nodes.size()>0) {
			System.out.println("checking if node already exists...");
			for (int i = 0; i < nodes.size(); i++) {//send nodes one by one to new node
				String oldIP = nodes.get(i).OwnIp.toString();
				oldIP = oldIP.replaceAll("[/]","");
				String oldPort = String.valueOf(nodes.get(i).OwnPort);
				if(oldIP.equals(ip) & oldPort.equals(port)) return true;
			}
		}
		return false;
}

	//MAIN
	public static void main(String[] argv){
		Global.node=new Node();
		Global.node.create("172.16.1.100",71);
		Incomming p=new Incomming();		
		new Thread(p).start();
		Reading q = new Reading();
		new Thread(q).start();
		Global.node.Display();	
	}
}

