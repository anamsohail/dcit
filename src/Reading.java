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
					InetAddress IP = InetAddress.getByName(new String(st.nextToken()));
					int port = Integer.parseInt(new String(st.nextToken()));
					Global.node.join(IP, port);
				}
			}
		}
		catch(Exception e){System.out.println("exception");}
	}
}

