public class TimeAdvanceGrant implements Runnable {
		private int logicalTime;
		private Node node;
		
		public TimeAdvanceGrant(Node node, int logicalTime) {
			this.logicalTime = logicalTime;
			this.node = node;
		}
		
		@Override
		public void run() {
			System.out.println("Time: " + this.logicalTime);
			
			if (this.logicalTime == 20) {
				this.node.awaitingFinalString = true;
				this.node.requestFinalString();
				return;
			}
			
			if (this.node.nextRequestTime == this.logicalTime) {
				this.node.getWordStringFromMaster();
			}
		}
	}