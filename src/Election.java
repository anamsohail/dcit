
public class Election implements Runnable {
	
	private Node node;

	public Election(Node node) {
		this.node = node;
	}
	
	@Override
	public void run() {
		if(this.node.nodes.size() > 0) {
			//send to higher IDs
			boolean higher = true;
			for (Node node : this.node.nodes) {
				if (node.id > this.node.id) {
					higher = false;
					this.node.sender.execute("startElection", new Object[] { this.node.ip, this.node.port, this.node.id }, node.ip, node.port);
				}
			}
			
			if (higher) {
				//higher...send message to network declaring yourself winner
				System.out.println("Highest ID. I'm the Winner!");
				this.node.setMasterNode(this.node);
				this.advert();
			} else {
				//not higher...wait for response
				System.out.println("waiting for response...");
				for (int i = 0; i < 5; i++) {
					try {
						Thread.sleep(1000);
						if(this.node.responded) {
							System.out.println("I lost the election!");
							return;
						}
					} catch (InterruptedException e) {e.printStackTrace();}
				}
				
				System.out.println("No response. I'm the Winner!");
				this.node.setMasterNode(this.node);
				this.advert();
			}
		}
		else {
			System.out.println("No nodes connected. Setting self to master node");
			this.node.setMasterNode(this.node);
		}		
	}
	
	private void advert() {
		for (Node node : this.node.nodes) {
			this.node.sender.execute("masterNodeAnnouncement", new Object[] { this.node.id }, node);
		}
	}
}
