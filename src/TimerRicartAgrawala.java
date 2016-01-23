
public class TimerRicartAgrawala implements Runnable {
	private Node node;
	private int waitTime;
	
	public TimerRicartAgrawala(Node node, int waitTime) {
		this.node = node;
		this.waitTime = waitTime;
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(this.waitTime * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		this.node.requestAllForWordString();
		
	}

}
