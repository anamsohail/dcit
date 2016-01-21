import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;


public class Reading implements Runnable {
	@Override
	public void run(){
		try{
			while(true){
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
				String s = bufferRead.readLine();
				StringTokenizer st=new StringTokenizer(s, " ");
				s=st.nextToken();
				if(s.equals("join")){
					if (st.countTokens() != 2) {
						System.out.println("Usage: join <IP address> <port>");
						continue;
					}
					String IP = st.nextToken();
					int port = Integer.parseInt(new String(st.nextToken()));
					int myPort = Global.node.OwnPort;
					Global.node.join(IP, port, myPort);
				} 
				else if (s.equals("start")) {
					if (st.countTokens() != 1) {
						System.out.println("Usage: start <algorithm (CME or RA)>");
						continue;
					}
					
					String algorithm = st.nextToken();
					if (algorithm.toUpperCase().equals("CME")) {
						Global.node.sendStart(Algorithm.CENTRALIZED_MUTUAL_EXCLUSION);
					} else if (algorithm.toUpperCase().equals("RA")) {
						Global.node.sendStart(Algorithm.RICART_AGRAWALA);
					} else {
						System.out.println("Usage: start <algorithm (CME or RA)>");
					}
				}
				else if (s.equals("ELECTION")) {
					Global.node.election();
				}	
				else if(s.equals("sign")) {
					Global.node.signOff();
					System.exit(0);
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}

