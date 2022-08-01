/**
 * 
 */
package anzen.client.messages;

/**
 * @author mlcs05
 *
 */
public interface PingFactory<INPUT> {
	
	Ping create(long clientId, String clientName, String senderAddress, ClientCapability[] capabilities);

	Ping fromInput(INPUT input);
	
	INPUT convertToInput(Ping ping);
	
}
