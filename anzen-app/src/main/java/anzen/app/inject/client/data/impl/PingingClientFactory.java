/**
 * 
 */
package anzen.app.inject.client.data.impl;

import com.frontier.lib.validation.NumberValidation;
import com.frontier.lib.validation.ObjectValidator;

import anzen.app.inject.client.data.Client;
import anzen.app.inject.client.data.ClientFactory;
import anzen.client.messages.ClientCapability;

/**
 * @author mlcs05
 *
 */
public final class PingingClientFactory implements ClientFactory {

	@Override
	public Client create(long id, String name, String senderAddress, ClientCapability[] capabilities) {
		ObjectValidator.raiseIfNull(name);
		ObjectValidator.raiseIfNull(senderAddress);
		NumberValidation.raiseIfLessThan(id, 0);
		ObjectValidator.raiseIfNull(capabilities);
		return new PingingClient(id, name, senderAddress, capabilities);
	}

}
