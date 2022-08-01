
package anzen.app.inject.data;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import com.frontier.shishya.distributed.receiving.guava.reassembly.DataPayloadReadyEvent;
import com.google.common.eventbus.Subscribe;

/**
 * @author mlcs05
 *
 */
@Named
@ApplicationScoped
public class DataGateway {
	
	private static final Logger LOGGER = Logger.getLogger(DataGateway.class.getName());
	
	@Inject
	private DataServer dataServer;
	
	@Inject
	private DataHandler dataHandler;
	
	@PostConstruct
	public void onCreate() {
		LOGGER.trace("onCreate");
		LOGGER.debug("registering on server event bus");
		dataServer.getServerEventBus().register(this);
	}
	
	@PreDestroy
	public void onDestroy() {
		LOGGER.trace("onDestory");
		LOGGER.debug("unregistering on server event bus");
		dataServer.getServerEventBus().unregister(this);
	}
	
	@Subscribe
	public void onReceivePayload(DataPayloadReadyEvent event) {
		LOGGER.trace("onReceivePayload");
		dataHandler.receive(event.payload, event.sender, event.dataType);
	}
}
