import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;


public class Reading implements Runnable {
	
	private Node node;

	public Reading(Node node) {
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
					this.node.election();
				}	
				else if(s.toUpperCase().equals("SIGN")) {
					this.node.signOff();
					System.exit(0);
				}
				else {
					System.out.println("Unknown command: " + s);
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}

