/**
 * 
 */
package anzen.app.inject.client.data;

import anzen.client.messages.ClientCapability;

/**
 * @author mlcs05
 *
 */
public interface Client {

	String getSendAddress();
	
	String getClientName();
	
	long getClientId();
	
	long getLastPingTime();
	
	void setLastPingTime(long pingTime);
	
	ClientCapability[] getCapabilities();
}
