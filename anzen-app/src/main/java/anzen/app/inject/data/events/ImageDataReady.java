/**
 * 
 */
package anzen.app.inject.data.events;

import java.net.InetAddress;

import com.frontier.lib.validation.ObjectValidator;

/**
 * @author mlcs05
 *
 */
public final class ImageDataReady {

	private final byte[] imageData;
	
	private final InetAddress sender;
	
	public ImageDataReady(byte[] data, InetAddress sender) {
		ObjectValidator.raiseIfNull(data);
		ObjectValidator.raiseIfNull(sender);
		this.imageData = data;
		this.sender = sender;
	}
	
	public byte[] getImageData() {
		return imageData;
	}
	
	public InetAddress getSender() {
		return sender;
	}
}
