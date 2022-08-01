/**
 * 
 */
package anzen.client.config.server;

/**
 * @author stebbinm
 * This is the Anzen Server - not the local server
 */
public interface ServerConfiguration {

	String getServerAddress();
	
	int[] getServerPorts();
	
}
