import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;


public class election implements Runnable {
	@Override
	public void run() {
		if(Global.node.nodes.size()>0) {//send to higher IDs
			String myIP = Global.node.OwnIp.toString();
			myIP = myIP.replaceAll("[/]","");
			String myPort = String.valueOf(Global.node.OwnPort);
			String myID = String.valueOf(Global.node.ID);
			String send="ELECTION,"+myIP+","+myPort+","+myID;
			byte[] buffer=send.getBytes();
			boolean higher = true;
			for (int i = 0; i < Global.node.nodes.size(); i++) {
				int oldID = Global.node.nodes.get(i).ID;
				if(oldID>Global.node.ID) {
					higher = false;
					InetAddress oldIP = Global.node.nodes.get(i).OwnIp;
					int oldPort = Global.node.nodes.get(i).OwnPort;
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, oldIP, oldPort);
					try {
						Global.node.sendsocket.send(packet);
					} catch (IOException e) {e.printStackTrace();}
					System.out.println("sent message: "+send);
				}
			}
			if(higher==false) {//not higher...wait for response
				long timeEnd = System.currentTimeMillis() + (20 * 1000);
				while (System.currentTimeMillis() < timeEnd) {
					try {
						Thread.sleep(5*1000);
					} catch (InterruptedException e) {e.printStackTrace();}
					System.out.println("sleeping...");
				}
				if(Global.node.responded==true) {
					System.out.println("I lost the election!");
					//this.responded=false;
				}
				else {
					System.out.println("No response. I'm the Winner!");
					Global.node.setMasterNode(Global.node);
					Global.node.advert();
				}
			}
			else{//higher...send message to network declaring yourself winner
				System.out.println("Highest ID. I'm the Winner!");
				Global.node.setMasterNode(Global.node);
				Global.node.advert();
			}
		}
		else {
			System.out.println("No nodes connected. Setting self to master node");
			Global.node.setMasterNode(Global.node);
		}
		
	}

}