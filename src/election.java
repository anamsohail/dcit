
public class election implements Runnable {
	@Override
	public void run() {
		if(Global.node.nodes.size()>0) {//send to higher IDs
			boolean higher = true;
			for (Node node : Global.node.nodes) {
				if (node.ID > Global.node.ID) {
					higher = false;
					Global.node.sender.execute("election", new Object[] { Global.node.OwnIp, Global.node.OwnPort, Global.node.ID }, node.OwnIp, node.OwnPort);
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
