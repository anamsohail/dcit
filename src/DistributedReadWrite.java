import java.util.Random;

/**
 * Runs the distributed read and write operation.
 * 
 * @author Jeanine Bonot
 *
 */
public class DistributedReadWrite implements Runnable{
	private static int WAIT_MIN = 1;
	private static int WAIT_MAX = 5;
	private static String WORDS[] = {"apple", "banana", "carrot", "date", "eggplant", "fig", "guava"};
	private Node node;
	
	public DistributedReadWrite(Node node) {
		this.node = node;
	}
	
	/**
	 * Run a loop for 20 seconds and do the following:
	 * - Wait a random amount of time
	 * - Read the string variable from the master node
	 * - Append some random english word to this string
	 * - Write the updated string to the master node 
	 */
	public void run() {
		Random rand = new Random();
		String wordString;
		
		// TODO: Synchronize the 20 second duration with other nodes.
		long timeEnd = System.currentTimeMillis() + (20 * 1000);
		while (System.currentTimeMillis() < timeEnd) {
			
			// Sleep for a random amount of time
			try {
				int seconds = rand.nextInt(WAIT_MAX - WAIT_MIN) + WAIT_MIN;
				Thread.sleep((long)(seconds * 1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			wordString = this.node.getMasterString();
			
			// Append some random English word to the string.
			if (wordString.contains(" ")) {
				wordString += " " + WORDS[rand.nextInt(WORDS.length)];
			} else {
				wordString += WORDS[rand.nextInt(WORDS.length)];
			}

			this.node.sendWordStringToMaster(wordString);
		}
		
		System.out.println(this.node.getMasterString());
	}
}
