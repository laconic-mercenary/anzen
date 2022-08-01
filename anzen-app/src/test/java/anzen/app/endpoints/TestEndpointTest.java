/**
 * 
 */
package anzen.app.endpoints;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import anzen.client.messages.ClientMessageType;
import cerberus.devices.ImageCaptureDevice;
import cerberus.devices.imaging.CapturedImage;
import cerberus.devices.imaging.ImageDimensionsType;
import cerberus.devices.impl.WebcamDeviceManager;

import com.frontier.shishya.common.RandomRangePortGenerator;
import com.frontier.shishya.distributed.sending.DataDistributor;
import com.frontier.shishya.distributed.sending.DataReadyEvent;
import com.frontier.shishya.distributed.sending.DataSendFailedListener;
import com.frontier.shishya.distributed.sending.guava.EventBusDataDistributor;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 * @author mlcs05
 *
 */
public class TestEndpointTest {

	static DataDistributor distributor = null;

	@Test
	@Ignore
	public void test() throws IOException {
		long stopTime = System.currentTimeMillis()
				+ TimeUnit.SECONDS.toMillis(100L);
		ExecutorService executor = Executors.newCachedThreadPool();
		WebcamDeviceManager deviceManager = new WebcamDeviceManager();
		List<ImageCaptureDevice> devices = deviceManager.listCaptureDevices();
		if (!devices.isEmpty()) {
			ImageCaptureDevice dev = devices.get(0);
			EventBus eventBus = initializeDistributor(executor);
			dev.setImageDimensions(ImageDimensionsType.HIGHEST);
			dev.open();
			try {
				while (stopTime > System.currentTimeMillis()) {
					CapturedImage image = dev.captureImage();
					sendOff(image, eventBus);
				}
			} finally {
				dev.close();
				distributor.close();
				executor.shutdownNow();
			}
		}
	}

	private static void sendOff(CapturedImage image, EventBus posterBus)
			throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			image.writeTo(baos);
			posterBus.post(new DataReadyEvent(baos.toByteArray(), ClientMessageType.STREAM_IMAGE.dataType()));
		}
	}

	private static EventBus initializeDistributor(ExecutorService executor) {
		EventBus eventBus = new AsyncEventBus("test-event-bus", executor);
		distributor = new EventBusDataDistributor(
				eventBus,
				InetAddress.getLoopbackAddress(), 
				new RandomRangePortGenerator(56652, 59652), 
				40000, 
				10, 
				new SendFailure()
		);
		distributor.addSender(39169, InetAddress.getLoopbackAddress());
		distributor.addSender(39170, InetAddress.getLoopbackAddress());
		distributor.addSender(39171, InetAddress.getLoopbackAddress());
		return eventBus;
	}

	private static final class SendFailure implements DataSendFailedListener {

		@Override
		public void onDataSendFailure(byte[] bs, Throwable t) {
			System.out.println("SEND FAILED");
			throw new RuntimeException(t);
		}
	}
}
