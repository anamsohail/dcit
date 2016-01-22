import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DistributedReadWrite {
	private static final int WAIT_MIN = 1;
	private static final int WAIT_MAX = 10;
	private final String WORDS[] = {"apple", "banana", "carrot", "date", "eggplant", "fig", "guava"};
	private Random rand = new Random();

	private List<String> appended = new ArrayList<String>();	
	
	/**
	 * Appends a random word to the word string.
	 * @param wordString
	 * @return the updated word string.
	 */
	public String appendRandomWord(String wordString) {
		String newWord = WORDS[new Random().nextInt(WORDS.length)];
		wordString += (wordString.length() != 0 ? " " : "") + newWord;
		this.appended.add(newWord);
		return wordString;
	}
	
	/**
	 * Checks the final string from the master node.
	 */
	public void checkFinalString(String wordString) {
		System.out.println("-------------------");
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
	 * Gets a random waiting time.
	 * @return the logical waiting time.
	 */
	public int getRandomWaitingTime() {
		return this.rand.nextInt(WAIT_MAX - WAIT_MIN) + WAIT_MIN;
	}
	
	/**
	 * Prints a list of strings in the format: [caption]: [word] [word] [word]...
	 * @param caption
	 * @param list
	 */
	public void printStringList(String caption, List<String> list) {
		System.out.print(caption);
		for (String word : list) {
			System.out.print(" " + word);
		}
		System.out.println();
	}
	
	/**
	 * Resets the distributed read/write.
	 */
	public void reset() {
		this.appended.clear();
	}
}
