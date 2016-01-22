
public class election implements Runnable {
	@Override
	public void run() {
		if(Global.node.nodes.size()>0) {//send to higher IDs
			String myIP = Global.node.OwnIp;
			String myPort = String.valueOf(Global.node.OwnPort);
			String myID = String.valueOf(Global.node.ID);
			String send="ELECTION,"+myIP+","+myPort+","+myID;
			boolean higher = true;
			for (int i = 0; i < Global.node.nodes.size(); i++) {
				int oldID = Global.node.nodes.get(i).ID;
				if(oldID>Global.node.ID) {
					higher = false;
					String oldIP = Global.node.nodes.get(i).OwnIp;
					int oldPort = Global.node.nodes.get(i).OwnPort;
					Global.node.sender.execute("election", new Object[] { send }, oldIP, oldPort);
					System.out.println("sent message: "+send);
				}
			}
			if(higher==false) {//not higher...wait for response
				long timeEnd = System.currentTimeMillis() + (5 * 1000);
				while (System.currentTimeMillis() < timeEnd) {
					try {
						Thread.sleep(1*1000);
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
