/**
 * 
 */
package anzen.device;

/**
 * @author stebbinm
 *
 */
public final class DeviceException extends Exception {

	private static final long serialVersionUID = 5045771553184824430L;
	
	public DeviceException(String msg) {
		super(msg);
	}
	
	public DeviceException(Throwable t) {
		super(t);
	}

	public DeviceException(String msg, Throwable t) {
		super(msg, t);
	}
}