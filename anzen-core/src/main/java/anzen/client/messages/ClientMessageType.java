/**
 * 
 */
package anzen.client.messages;

/**
 * @author mlcs05
 */
public enum ClientMessageType {

	// messages coming from the CLIENT
	
	STREAM_IMAGE(0x11),
	CLIENT_MESSAGE(0x10),
	PING(0x12),
	UNKNOWN(0x99);
	
	private final byte dataType;
	
	private ClientMessageType(int value) {
		this.dataType = (byte) value;
	}
	
	public byte dataType() {
		return dataType;
	}
	
	public static ClientMessageType fromByte(byte val) {
		if (val == STREAM_IMAGE.dataType()) {
			return STREAM_IMAGE;
		} else if (val == CLIENT_MESSAGE.dataType()) {
			return CLIENT_MESSAGE;
		} else if (val == PING.dataType()) {
			return PING;
		}
		return UNKNOWN;
	}
}
