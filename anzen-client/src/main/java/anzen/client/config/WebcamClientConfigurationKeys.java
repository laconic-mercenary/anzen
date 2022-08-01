/**
 * 
 */
package anzen.client.config;

/**
 * @author mlcs05
 *
 */
public enum WebcamClientConfigurationKeys {

	SERVER_ADDRESS("client.server_address"),
	SERVER_PORT_1("client.server_rx_port1"),
	SERVER_PORT_2("client.server_rx_port2"),
	SERVER_PORT_3("client.server_rx_port3"),
	CLIENT_BIND_ADDR("client.bind_address"),
	CLIENT_BIND_PORT("client.bind_port"),
	CLIENT_DEVICE_NAME("client.device_name");
	
	private final String key;
	
	private WebcamClientConfigurationKeys(String key) {
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}
}
