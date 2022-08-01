/**
 * 
 */
package anzen.app.inject.data;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;

import anzen.app.inject.client.events.ClientPingEvent;
import anzen.app.inject.data.events.ImageDataReady;
import anzen.app.util.AnzenEventBus;
import anzen.client.messages.ClientMessageType;
import anzen.client.messages.Ping;
import anzen.client.messages.PingFactory;
import anzen.client.messages.PingFactoryImpl;
import anzen.configuration.properties.PropertiesAnnotations;
import anzen.configuration.properties.PropertiesBound;
import anzen.configuration.properties.PropertiesField;
import anzen.util.SpecialExecutors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 * @author mlcs05
 *
 */
@Named
@ApplicationScoped
@PropertiesBound(propertiesName = "DataHandler")
public class DataHandler {

	private static final Logger LOGGER = Logger.getLogger(DataHandler.class.getName());

	private static final PingFactory<String> PING_FACTORY = new PingFactoryImpl();

	private static final RejectedExecutionHandler DISCARD_AND_LOG = new RejectedExecutionHandler() {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			StringBuilder msg = new StringBuilder();
			msg.append("Handler queue has reached it's limit. Will discard: ");
			msg.append(r.getClass());
			LOGGER.warn(msg);
		}
	};

	private ExecutorService executorService = null;

	private EventBus imageReadyEventBus = null;

	@PropertiesField(key = "data.thread_pool_size", type = Integer.class)
	private int threadPoolSize;

	@PropertiesField(key = "data.data_queue_capacity", type = Integer.class)
	private int queueCapacity;

	@PostConstruct
	public void onCreate() {
		LOGGER.trace("onCreate");

		LOGGER.debug("loading properties configuration items");
		PropertiesAnnotations.loadConfiguration(this);

		this.executorService = SpecialExecutors.newBoundedFixedThreadPool(this.threadPoolSize, this.queueCapacity, 0L,
				DISCARD_AND_LOG);

		this.imageReadyEventBus = new AsyncEventBus(ImageDataReady.class.getSimpleName() + "-eventBus",
				this.executorService);

		if (LOGGER.isDebugEnabled()) {
			StringBuilder msg = new StringBuilder();
			msg.append("created fixed thread pool of size ");
			msg.append(this.threadPoolSize);
			msg.append(" - queue capacity of size ");
			msg.append(this.queueCapacity);
			LOGGER.debug(msg.toString());
		}

		LOGGER.info("initialized and ready");
	}

	@PreDestroy
	public void onDestroy() {
		LOGGER.trace("onDestroy");
		this.executorService.shutdownNow();
	}

	public EventBus getImageReadyEventBus() {
		return this.imageReadyEventBus;
	}

	public void receive(byte[] data, InetAddress sender, byte dataTypeIndicator) {
		LOGGER.trace("receive");
		
		ClientMessageType messageType = ClientMessageType.fromByte(dataTypeIndicator);
		
		if (ClientMessageType.STREAM_IMAGE.equals(messageType)) {
			// this will be the critical path, assuming
			// lots of images are being streamed
			handleStreamImage(data, sender);
		} else if (ClientMessageType.PING.equals(messageType)) {
			// TODO: maybe make this apart of the CLIENT_MESSAGE indicator?
			handlePing(data, sender);
		} else if (ClientMessageType.CLIENT_MESSAGE.equals(messageType)) {
			LOGGER.warn("Received " + messageType.name());
		} else {
			LOGGER.error("UNKNOWN MESSAGE TYPE RECEIVED: " + dataTypeIndicator);
		}
	}

	private void handleStreamImage(byte[] data, InetAddress sender) {
		this.imageReadyEventBus.post(new ImageDataReady(data, sender));
	}

	private void handlePing(byte[] data, InetAddress sender) {
		// use the generic global event bus for this
		String ping = new String(data, Charsets.UTF_8);
		Ping pingObj = PING_FACTORY.fromInput(ping);
		// using the global, 'utility' event bus here
		AnzenEventBus.get().post(new ClientPingEvent(pingObj, sender.getHostAddress()));
	}
}
