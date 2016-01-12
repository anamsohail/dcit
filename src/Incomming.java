import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
					System.out.println("joining");
					System.out.println(IP);
					System.out.println(PORT);			
				}
			}
			}catch(Exception e){System.out.println("exception");}			
	}

}
