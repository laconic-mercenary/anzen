/**
 * 
 */
package anzen.device;

/**
 * @author stebbinm
 *
 */
public interface Device extends AutoCloseable {

	void open() throws DeviceException;	
	
	boolean isOpen();
	
	void close() throws DeviceException;
	
	String getId();
	
	byte[] getCurrentData() throws DeviceException;
	
}
