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
				StringTokenizer st=new StringTokenizer(s, " ,");
				s=st.nextToken();
				if(s.equals("join")){
					String nIP = st.nextToken();
					InetAddress IP = InetAddress.getByName(nIP);
					nIP = nIP.replaceAll("[/]","");
					int port = Integer.parseInt(new String(st.nextToken()));
					int myPort = Integer.parseInt(new String(st.nextToken()));
					Global.node.join(IP, port, myPort);
					if(!Global.node.checkInList(nIP, String.valueOf(port))) {
						Global.node.addNodeToList(IP, port);
					}
					else {
						System.out.println("Node already in network!");
					}

				} else if (s.equals("start")) {
					// TODO: Start distributed read and write
				}
			}
		}
		catch(Exception e){System.out.println("exception");}
	}
}

