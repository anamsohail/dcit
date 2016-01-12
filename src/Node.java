import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Node {
	public InetAddress OwnIp;
	public int OwnPort; 
	public DatagramSocket sendsocket;

	public void create(int port){
		try{
			OwnIp=InetAddress.getLocalHost();
			sendsocket=new DatagramSocket();
			OwnPort=port;
		}catch(Exception e){System.out.println("exception");}
	}

	public void join(InetAddress Ip,int port){
		try{
			String IpPort=Ip.getHostAddress()+","+port;
			String send="join,"+IpPort;
			byte[] buffer=send.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, Ip, port);
			sendsocket.send(packet);
			System.out.println("sent message: "+send);
		}catch(Exception e ){System.out.println("exception");}	
	}

	public void Display(){
		try{
			String IP=OwnIp.getHostAddress();
			String port=String.valueOf(OwnPort);
			String message="My IP is "+IP+" my port is "+port;
			System.out.println(message);
		}catch(Exception e){System.out.println("exception");}
	}	

	//MAIN
	public static void main(String[] argv){
		Global.node=new Node();
		Global.node.create(71);
		Incomming p=new Incomming();		
		new Thread(p).start();
		Reading q = new Reading();
		new Thread(q).start();
		Global.node.Display();	
	}
}

