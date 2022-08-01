/**
 * 
 */
package anzen.client.messages.json;

import java.io.Serializable;

import com.frontier.lib.validation.NumberValidation;
import com.frontier.lib.validation.ObjectValidator;

import anzen.client.messages.ClientCapability;
import anzen.client.messages.Ping;

/**
 * @author mlcs05
 */
class JsonPing implements Ping, Serializable {
	
	private static final long serialVersionUID = 201602282022L;

	private final long clientId;
	
	private final String clientName;
	
	private final String senderAddress;
	
	private final ClientCapability[] capabilities;
	
	public JsonPing(long clientId, String clientName, String senderAddress, ClientCapability[] capabilities) {
		NumberValidation.raiseIfLessThanOrEqualTo(clientId, 0L);
		ObjectValidator.raiseIfNull(clientName);
		ObjectValidator.raiseIfNull(senderAddress);
		ObjectValidator.raiseIfNull(capabilities);
		this.clientId = clientId;
		this.clientName = clientName;
		this.senderAddress = senderAddress;
		this.capabilities = capabilities;
	}

	@Override
	public long getClientId() {
		return clientId;
	}

	@Override
	public String getClientName() {
		return clientName;
	}

	@Override
	public String getSendersAddress() {
		return senderAddress;
	}

	@Override
	public ClientCapability[] getCapabilities() {
		return capabilities;
	}
}