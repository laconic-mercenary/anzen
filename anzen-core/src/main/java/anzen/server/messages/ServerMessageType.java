/**
 * 
 */
package anzen.server.messages;

/**
 * @author mlcs05
 *
 */
public enum ServerMessageType {

	// messages issued strictly by the SERVER
	
	STREAM_START(0x21),
	STREAM_STOP(0x22),
	UNKNOWN(0x0);
	
	private final byte dataType;
	
	private ServerMessageType(int value) {
		this.dataType = (byte) value;
	}
	
	public byte dataType() {
		return dataType;
	}
	
	public static ServerMessageType fromByte(byte data) {
		if (STREAM_START.dataType() == data) {
			return STREAM_START;
		} else if (STREAM_STOP.dataType() == data) {
			return STREAM_STOP;
		}
		return UNKNOWN;
	}
}
