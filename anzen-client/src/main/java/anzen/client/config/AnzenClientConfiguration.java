/**
 * 
 */
package anzen.client.config;

import anzen.client.config.client.ClientConfiguration;
import anzen.client.config.server.ServerConfiguration;

/**
 * @author stebbinm
 */
public interface AnzenClientConfiguration {
	
	ServerConfiguration getServerConfiguration();

	ClientConfiguration getClientConfiguration();
	
}
