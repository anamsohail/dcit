import java.util.ArrayList;
import java.util.List;
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
	private List<String> appended = new ArrayList<String>();
	
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
				System.out.println("Sleeping for " + seconds + " seconds...");
				Thread.sleep((long)(seconds * 1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			wordString = this.node.getMasterString();
			System.out.println("old string: " + wordString);
			
			// Append some random English word to the string.
			String newWord = WORDS[rand.nextInt(WORDS.length)];
			if (wordString.length() != 0) {
				wordString += " ";
			} 

			wordString += newWord;
			this.appended.add(newWord);

			this.node.sendWordStringToMaster(wordString);
			System.out.println("new string: " + wordString);
		}
		
		System.out.println("-------------------");
		wordString = this.node.getMasterString();
		System.out.println("final string: " + wordString);
		
		// Check that all appended words are in the final string.
		String[] finalTokens = wordString.split(" ");
		this.printStringList("words appended by this node:", this.appended);
		
		List<String> missing = new ArrayList<String>();
		for (String word : this.appended) {
			boolean found = false;
			for (int i = 0; i < finalTokens.length; i++) {
				if (word.equals(finalTokens[i])) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				missing.add(word);
			}
		}
		
		System.out.println("-------------------");
		if (missing.size() == 0) {
			System.out.println("All words are included in the final string.");
		} else {
			System.out.println("Some words are missing from the final string.");
			this.printStringList("Words missing from final string:", missing);	
		}

	}
	
	/**
	 * Prints a list of strings in the format: [caption]: [word] [word] [word]...
	 * @param caption
	 * @param list
	 */
	private void printStringList(String caption, List<String> list) {
		System.out.print(caption);
		for (String word : list) {
			System.out.print(" " + word);
		}
		System.out.println();
	}
}
