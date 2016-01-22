import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * Class responsible for sending messages across nodes.
 */
public class Sender {

	private XmlRpcClient sender = new XmlRpcClient();
	private XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

	/**
	 * Tells the given node to execute a method with particular parameters.
	 * 
	 * @param method
	 * @param params
	 * @param node
	 */
	public void execute(String method, Object[] params, Node node) {
		this.execute(method, params, node.OwnIp, node.OwnPort);
	}

	/**
	 * Tells the given address to execute a method with particular parameters.
	 * 
	 * @param method
	 * @param params
	 * @param node
	 */
	public void execute(String method, Object[] params, String destIP, int destPort) {
		try {
			this.config.setServerURL(new URL("http://" + destIP + ":" + destPort));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		this.config.setEnabledForExtensions(true);
		this.sender.setConfig(config);

		try {
			this.sender.execute(String.format("receiver.%s", method), params);
		} catch (XmlRpcException e) {
			e.printStackTrace();
		}
	}
}
