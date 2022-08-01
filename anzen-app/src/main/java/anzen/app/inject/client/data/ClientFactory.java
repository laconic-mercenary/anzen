/**
 * 
 */
package anzen.app.inject.client.data;

import anzen.client.messages.ClientCapability;

/**
 * @author mlcs05
 *
 */
public interface ClientFactory {

	Client create(long id, String name, String senderAddress, ClientCapability[] capabilities);
	
}
