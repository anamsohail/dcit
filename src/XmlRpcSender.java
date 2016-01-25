import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * Class responsible for sending messages across nodes.
 */
public class XmlRpcSender {
	
	private static final XmlRpcClient SENDER = new XmlRpcClient();
	private static final XmlRpcClientConfigImpl CONFIG = new XmlRpcClientConfigImpl();

	/**
	 * Tells the given node to execute a method with particular parameters.
	 * 
	 * @param method
	 * @param params
	 * @param node
	 */
	public static void execute(String method, Object[] params, Node node) {
		XmlRpcSender.execute(method, params, node.ip, node.port);
	}

	/**
	 * Tells the given address to execute a method with particular parameters.
	 * 
	 * @param method
	 * @param params
	 * @param node
	 */
	public static void execute(String method, Object[] params, String destIP, int destPort) {
		try {
			CONFIG.setServerURL(new URL("http://" + destIP + ":" + destPort));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		CONFIG.setEnabledForExtensions(true);
		SENDER.setConfig(CONFIG);

		try {
			SENDER.execute(String.format("receiver.%s", method), params);
		} catch (XmlRpcException e) {
			if (e.getMessage().contains("Unknown type: nil")) {
				// Ignore this exception.
				return;
			}
			
			e.printStackTrace();
		}
	}
}
