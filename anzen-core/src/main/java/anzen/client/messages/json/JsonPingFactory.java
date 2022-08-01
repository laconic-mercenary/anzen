/**
 * 
 */
package anzen.client.messages.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import anzen.client.messages.ClientCapability;
import anzen.client.messages.Ping;
import anzen.client.messages.PingFactory;

/**
 * @author mlcs05
 *
 */
public class JsonPingFactory implements PingFactory<String> {
	
	private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

	@Override
	public Ping fromInput(String input) {
		Gson gson = GSON_BUILDER.create();
		return gson.fromJson(input, JsonPing.class);
	}

	@Override
	public String convertToInput(Ping ping) {
		Gson gson = GSON_BUILDER.create();
		return gson.toJson(ping);
	}

	@Override
	public Ping create(long clientId, String clientName, String senderAddress, ClientCapability[] capabilities) {
		return new JsonPing(clientId, clientName, senderAddress, capabilities);
	}

}
