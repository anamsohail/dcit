import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Class responsible for reading the command-line input.
 */
public class InputReader implements Runnable {
	
	private Node node;

	public InputReader(Node node) {
		this.node = node;
	}
	
	@Override
	public void run(){
		try{
			while(true){
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
				String s = bufferRead.readLine();
				StringTokenizer st=new StringTokenizer(s, " ");
				s=st.nextToken();
				if(s.toUpperCase().equals("JOIN")){
					if (st.countTokens() != 2) {
						System.out.println("Usage: join <IP address> <port>");
						continue;
					}
					String IP = st.nextToken();
					int port = Integer.parseInt(new String(st.nextToken()));
					int myPort = this.node.port;
					this.node.join(IP, port, myPort);
				} 
				else if (s.toUpperCase().equals("START")) {
					if (st.countTokens() != 1) {
						System.out.println("Usage: start <algorithm (CME or RA)>");
						continue;
					}
					
					String algorithm = st.nextToken();
					if (algorithm.toUpperCase().equals("CME")) {
						this.node.sendStart(Algorithm.CENTRALIZED_MUTUAL_EXCLUSION);
					} else if (algorithm.toUpperCase().equals("RA")) {
						this.node.sendStart(Algorithm.RICART_AGRAWALA);
					} else {
						System.out.println("Usage: start <algorithm (CME or RA)>");
					}
				}
				else if (s.toUpperCase().equals("ELECTION")) {
					new Thread(new Election(this.node)).start();
				}	
				else if(s.toUpperCase().equals("SIGNOFF")) {
					for (Node node : this.node.nodes) {
						XmlRpcSender.execute("nodeSignOff", new Object[] { this.node.id }, node.ip, node.port);
					}
					System.exit(0);
				} else if (s.toUpperCase().equals("HELP")) {
					System.out.println("--- List of Commands ---");
					System.out.println("join - join a network");
					System.out.println("elect - elect a master node");
					System.out.println("start - start the distributed read-write operations");
					System.out.println("signoff - sign off from the network and exit");
					
				}
				else {
					System.out.println("Unknown command: " + s);
					System.out.println("Type 'help' for a list of commands");
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}

