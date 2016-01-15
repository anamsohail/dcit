import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
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
					String nIP = st.nextToken();
					InetAddress IP = InetAddress.getByName(nIP);
					nIP = nIP.replaceAll("[/]","");
					int port = Integer.parseInt(new String(st.nextToken()));
					int myPort = Global.node.OwnPort;
					Global.node.join(IP, port, myPort);
				} 
				else if (s.equals("start")) {
					Global.node.start();
				}
				else if (s.equals("ELECTION")) {
					Global.node.election();
				}	
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}

