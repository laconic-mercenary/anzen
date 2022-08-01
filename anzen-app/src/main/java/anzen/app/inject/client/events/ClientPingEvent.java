/**
 * 
 */
package anzen.app.inject.client.events;

import java.io.Serializable;

import anzen.client.messages.Ping;

import com.frontier.lib.validation.ObjectValidator;

/**
 * @author mlcs05
 *
 */
public final class ClientPingEvent implements Serializable {

	private static final long serialVersionUID = 201602282021L;
	
	private final Ping ping;
	
	private final String senderAddress;
	
	public ClientPingEvent(Ping ping, String senderAddress) {
		ObjectValidator.raiseIfNull(ping);
		this.ping = ping;
		this.senderAddress = senderAddress;
	}
	
	public Ping getPing() {
		return ping;
	}
	
	public String getSenderAddress() {
		return senderAddress;
	}
}
