/**
 * 
 */
package anzen.app.inject.client;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import anzen.app.inject.client.data.Client;
import anzen.app.inject.client.data.ClientFactory;
import anzen.app.inject.client.data.impl.PingingClientFactory;
import anzen.app.inject.client.events.ClientPingEvent;
import anzen.app.util.AnzenEventBus;
import anzen.client.messages.Ping;
import anzen.configuration.properties.PropertiesAnnotations;
import anzen.configuration.properties.PropertiesBound;
import anzen.configuration.properties.PropertiesField;

import com.frontier.lib.time.TimeUtil;
import com.google.common.eventbus.Subscribe;

/**
 * @author mlcs05
 *
 */
@Named
@ApplicationScoped
@PropertiesBound(propertiesName = "ClientManager")
public class ClientManager {
	
	private static final Logger LOGGER = Logger.getLogger(ClientManager.class.getName());
	
	private static final ClientFactory CLIENT_FACTORY = new PingingClientFactory();

	@PropertiesField(key = "server.client_rx_port", type = Integer.class)
	private int clientRxPort;
	
	@PropertiesField(key = "server.client_purge_expiry_hours", type = Integer.class)
	private int purgeExpiryTime;

	private final List<Client> availableClients = new LinkedList<>();
	
	@PostConstruct
	public void onCreate() {
		LOGGER.trace("onCreate");
		PropertiesAnnotations.loadConfiguration(this);
		AnzenEventBus.get().register(this);
	}
	
	@PreDestroy
	public void onDestroy() {
		LOGGER.trace("onDestroy");
		AnzenEventBus.get().unregister(this);
	}
	
	@Subscribe
	public void onPing(ClientPingEvent ping) {
		LOGGER.trace("onPing");		
		Ping clientPing = ping.getPing();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("received ping from client: " + clientPing.getClientName());
		}
		synchronized(this.availableClients) {
			Client targetPc = null;
			for (Client pc : this.availableClients) {
				if (pc.getClientId() == clientPing.getClientId()) {
					targetPc = pc;
					break;
				}
			}
			if (targetPc == null) {
				LOGGER.info("receving a ping from new client: " + clientPing.getClientName());
				targetPc = CLIENT_FACTORY.create(
					clientPing.getClientId(), 
					clientPing.getClientName(), 
					ping.getSenderAddress(),
					clientPing.getCapabilities()
				);
				this.availableClients.add(targetPc);
			}
			targetPc.setLastPingTime(timestamp());
		}
	}

	public int getClientRxPort() {
		return clientRxPort;
	}
	
	public List<Client> getAvailableClients() {
		LOGGER.trace("getAvailableClients");
		purgeAncientClients();
		return this.availableClients;
	}
	
	private void purgeAncientClients() {
		LOGGER.trace("purgeAncientClients");
		long now = timestamp();
		long expiry = TimeUnit.HOURS.toMillis(this.purgeExpiryTime);
		synchronized (this.availableClients) {
			Iterator<Client> clientItr = this.availableClients.iterator();
			while (clientItr.hasNext()) {
				Client pc = clientItr.next();
				long diff = now - pc.getLastPingTime();
				if (diff >= expiry) {
					LOGGER.debug("removing old client " + pc.getClientName());
					clientItr.remove();
				}
			}
		}
	}
	
	private static long timestamp() {
		return TimeUtil.nowUTC().getTime();
	}
}
