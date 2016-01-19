import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TimeAdvanceGrant implements Runnable {
		private static final int WAIT_MIN = 1;
		private static final int WAIT_MAX = 5;
		private final String WORDS[] = {"apple", "banana", "carrot", "date", "eggplant", "fig", "guava"};
		private Random rand = new Random();
		private int logicalTime;
		private Node node;
		
		public TimeAdvanceGrant(Node node, int logicalTime) {
			this.logicalTime = logicalTime;
			this.node = node;
		}
		
		@Override
		public void run() {
			System.out.println("Time: " + this.logicalTime);
			
			if (this.node.nextRequestTime == -1 ) {
				this.node.nextRequestTime = this.getRandomWaitingTime();
				System.out.println("Waiting for " + this.node.nextRequestTime + " seconds");
				return;
			}
			
			if (this.logicalTime == 20) {
				this.checkFinalString();
				return;
			}
			
			if (this.node.nextRequestTime == this.logicalTime) {
				String wordString = this.node.getMasterString();
				System.out.println("old string: " + wordString);
				
				// Append some random English word to the string.
				String newWord = WORDS[rand.nextInt(WORDS.length)];
				if (wordString.length() != 0) {
					wordString += " ";
				}
				
				wordString += newWord;
				this.node.appended.add(newWord);

				this.node.sendWordStringToMaster(wordString);
				System.out.println("new string: " + wordString);
				
				int seconds = this.getRandomWaitingTime();
				System.out.println("Waiting for " + seconds + " seconds");
				this.node.nextRequestTime = this.logicalTime + seconds;
			}
		}
		
		/**
		 * Gets a random waiting time.
		 * @return the logical waiting time.
		 */
		private int getRandomWaitingTime() {
			return new Random().nextInt(WAIT_MAX - WAIT_MIN) + WAIT_MIN;
		}
		
		/**
		 * Checks the final string from the master node.
		 */
		private void checkFinalString() {
			System.out.println("-------------------");
			String wordString = this.node.requestFinalString();
			System.out.println("final string: " + wordString);
			
			// Check that all appended words are in the final string.
			String[] finalTokens = wordString.split(" ");
			this.printStringList("words appended by this node:", this.node.appended);
			
			List<String> missing = new ArrayList<String>();
			for (String word : this.node.appended) {
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