
public class election implements Runnable {
	
	private Node node;

	public election(Node node) {
		this.node = node;
	}
	
	@Override
	public void run() {
		if(this.node.nodes.size()>0) {//send to higher IDs
			boolean higher = true;
			for (Node node : this.node.nodes) {
				if (node.ID > this.node.ID) {
					higher = false;
					this.node.sender.execute("election", new Object[] { this.node.OwnIp, this.node.OwnPort, this.node.ID }, node.OwnIp, node.OwnPort);
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
				if(this.node.responded==true) {
					System.out.println("I lost the election!");
					//this.responded=false;
				}
				else {
					System.out.println("No response. I'm the Winner!");
					this.node.setMasterNode(this.node);
					this.node.advert();
				}
			}
			else{//higher...send message to network declaring yourself winner
				System.out.println("Highest ID. I'm the Winner!");
				this.node.setMasterNode(this.node);
				this.node.advert();
			}
		}
		else {
			System.out.println("No nodes connected. Setting self to master node");
			this.node.setMasterNode(this.node);
		}		
	}
}
