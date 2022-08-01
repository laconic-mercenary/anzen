/**
 * 
 */
package anzen.app.inject.data;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import anzen.app.endpoints.sessions.ConnectedSession;
import anzen.configuration.properties.PropertiesAnnotations;
import anzen.configuration.properties.PropertiesBound;
import anzen.configuration.properties.PropertiesField;
import anzen.util.SpecialExecutors;

import com.frontier.shishya.distributed.receiving.DataPacketReAssembler;
import com.frontier.shishya.distributed.receiving.guava.EventBusServerManager;
import com.frontier.shishya.distributed.receiving.util.NewArrayDataPacketReAssembler;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 * @author mlcs05
 *
 */
@Named
@ApplicationScoped
@PropertiesBound(propertiesName = "DataServer")
public class DataServer {
	
	private static final Logger LOGGER = Logger.getLogger(DataServer.class.getName());
	
	private static final boolean AUTO_SUBSCRIBE_REASSEMBLER = true;

	private static final int SOCKET_SESSION_ARRAY_SIZE = 4;
	
	private static final RejectedExecutionHandler DISCARD_AND_LOG = new RejectedExecutionHandler() {
		@Override 
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			StringBuilder msg = new StringBuilder("Data queue has reached it's limit. Will discard: ");
			msg.append(r.getClass());
			LOGGER.warn(msg);
		}
	};
	
	private final DataPacketReAssembler reassembler = new NewArrayDataPacketReAssembler();
		
	private final List<ConnectedSession> websocketSessionList = 
		Collections.synchronizedList(
			new ArrayList<>(SOCKET_SESSION_ARRAY_SIZE)
		);
	
	private ExecutorService executorService = null; 
	
	private EventBusServerManager server = null;
	
	private EventBus eventBus = null;
	
	@PropertiesField(key="server.port1", type=Integer.class)
	private int serverPort1;
	
	@PropertiesField(key="server.port2", type=Integer.class)
	private int serverPort2;
	
	@PropertiesField(key="server.port3", type=Integer.class)
	private int serverPort3;
	
	@PropertiesField(key="server.thread_pool_size", type=Integer.class)
	private int serverThreadPoolSize;
	
	@PropertiesField(key="server.task_queue_max_length", type=Integer.class)
	private int serverTaskQueueSize;
	
	@PropertiesField(key="server.bind_address", type=String.class)
	private String serverBindAddress;
	
	@PropertiesField(key="server.receive_buffer_size", type=Integer.class)
	private int serverRxBufferSize;
	
	@PostConstruct
	public void onCreate() {
		LOGGER.trace("onCreate");
		
		LOGGER.debug("loading properties configuration items");
		PropertiesAnnotations.loadConfiguration(this);
		
		this.executorService = SpecialExecutors.newBoundedFixedThreadPool(
			this.serverThreadPoolSize, 
			this.serverTaskQueueSize,
			0L,
			DISCARD_AND_LOG
		);

		this.eventBus = new AsyncEventBus(
			DataServer.class.getSimpleName() + "-eventBus", 
			executorService
		);
		
		this.server = new EventBusServerManager(
			this.eventBus, 
			this.serverRxBufferSize, 
			AUTO_SUBSCRIBE_REASSEMBLER, 
			this.reassembler
		);
		
		InetAddress bindAddress = null;
		try {
			bindAddress = InetAddress.getByName(this.serverBindAddress);
		} catch (UnknownHostException e) {
			LOGGER.error("failed to get host by name with bind address: " + this.serverBindAddress);
			e.printStackTrace();
			return;
		}
		
		for (int port : new int[] { this.serverPort1, this.serverPort2, this.serverPort3 }) {
			LOGGER.debug("binding on port: " + port + ", addr: " + bindAddress);
			this.server.addServer(port, bindAddress);
		}
		
		if (LOGGER.isDebugEnabled()) {
			// string builder apparently is much more performant
			// than using string.format
			StringBuilder msg = new StringBuilder("created server manager::");
			msg.append(this.server.getClass());
			msg.append(", pool-size=");
			msg.append(this.serverThreadPoolSize);
			msg.append(", auto-subscribe=");
			msg.append(AUTO_SUBSCRIBE_REASSEMBLER);
			msg.append(", assembler=");
			msg.append(this.reassembler.getClass());
			LOGGER.debug(msg);
		}
	}
	
	@PreDestroy
	public void onDestroy() {
		LOGGER.trace("onDestroy");
		
		List<?> unexecutedTasks = Collections.emptyList();
		try {
			LOGGER.debug("shutting down executor service");
			unexecutedTasks = this.executorService.shutdownNow();
		} finally {
			LOGGER.debug("closing server");
			server.closeServers();
		}
		
		if (unexecutedTasks.isEmpty() == false) {
			LOGGER.warn("There were " + unexecutedTasks.size() + " tasks that failed to execute.");
		}
	}
	
	public String getServerBindAddress() {
		return serverBindAddress;
	}
	
	public EventBus getServerEventBus() {
		return eventBus;
	}

	public List<ConnectedSession> getWebsocketSessions() {
		return websocketSessionList;
	}
}