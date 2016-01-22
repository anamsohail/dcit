	public class Timer implements Runnable {
		private Node node;
		
		public Timer(Node node) {
			this.node = node;
		}
		@Override
		public void run() {
			long timeEnd = System.currentTimeMillis() + (20 * 1000);
			int logicalTime = 0;
			while (System.currentTimeMillis() < timeEnd) {
				try {
					Thread.sleep(1000);
					logicalTime += 1;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Advance time to: " + logicalTime);
				this.node.sendTimeAdvance(logicalTime);
			}
		}
	}