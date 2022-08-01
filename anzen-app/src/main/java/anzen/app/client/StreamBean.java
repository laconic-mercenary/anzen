/**
 * 
 */
package anzen.app.client;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped; // do not confuse with javax.faces.bean.ViewScoped
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import com.frontier.lib.time.TimeUtil;
import com.frontier.shishya.common.RandomRangePortGenerator;
import com.frontier.shishya.distributed.sending.DataDistributor;
import com.frontier.shishya.distributed.sending.DataReadyEvent;
import com.frontier.shishya.distributed.sending.DataSendFailedListener;
import com.frontier.shishya.distributed.sending.guava.EventBusDataDistributor;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import anzen.app.endpoints.ImageClientEndpoint;
import anzen.app.endpoints.sessions.ConnectedSession;
import anzen.app.inject.client.ClientManager;
import anzen.app.inject.client.data.Client;
import anzen.app.inject.data.DataHandler;
import anzen.app.inject.data.DataServer;
import anzen.app.inject.data.events.ImageDataReady;
import anzen.server.messages.ServerMessageType;

/**
 * @author mlcs05
 */
@Named
@ViewScoped
public class StreamBean implements Serializable {
	
	private static final long serialVersionUID = 201604231015L;

	private static final boolean SENDING = true;

	private static final boolean DORMANT = false;

	private static final Logger LOGGER = Logger.getLogger(StreamBean.class.getName());

	private static final String IMAGE_ENC = "data:image/jpeg;base64,";

	private static final byte STREAM_START_FLAG = ServerMessageType.STREAM_START.dataType();

	private static final byte STREAM_STOP_FLAG = ServerMessageType.STREAM_STOP.dataType();

	private final AtomicBoolean sendLockFlag = new AtomicBoolean(DORMANT);

	private long websocketStreamId = -1L;

	private ConnectedSession websocketSession = null;

	private Client selectedClient;

	private volatile boolean websocketSessionActive = false;

	@Inject
	private ClientManager clientManager;

	@Inject
	private DataServer dataServer;

	@Inject
	private DataHandler dataHandler;

	@PostConstruct
	public void onPageLoaded() {
		LOGGER.trace("onPageLoaded");
		// generate an ID to associate with a websocket session
		websocketStreamId = generateSocketSessionId();
		// pass over the client information
		resolveClient();

		if (LOGGER.isDebugEnabled()) {
			StringBuilder msg = new StringBuilder();
			if (selectedClient != null) {
				msg.append("user has selected client address ");
				msg.append(selectedClient.getClientName());
			} else {
				msg.append("no client was selected or parameterized");
			}
			msg.append(" - generated websocket stream id ");
			msg.append(websocketStreamId);
			LOGGER.debug(msg);
		}
		// tell the anzen client to start sending images
		sendStartStream(selectedClient);
		// register this for image events
		LOGGER.debug("registering on image ready event bus");
		dataHandler.getImageReadyEventBus().register(this);
	}

	@PreDestroy
	public void onPageLeave() {
		LOGGER.trace("onPageLeave");
		LOGGER.debug("unregistering on image ready event bus");
		dataHandler.getImageReadyEventBus().unregister(this);
		sendStopStream(selectedClient);
		websocketSessionActive = false;
		selectedClient = null;
	}
	
	public String getSelectedClientName() {
		String result = FacesContext.getCurrentInstance()
									.getExternalContext()
									.getRequestParameterMap()
									.get("clientname");
		return result;
	}

	@Subscribe
	public void imageDataReady(ImageDataReady payload) {
		// NOTE: this is in a separate thread pool from the Faces service thread
		// any beans injected cannot be used here
		LOGGER.trace("imageDataReady");
		if (isConnected(websocketSession) == false) {
			LOGGER.debug("not connected");
			return;
		}
		if (isSenderOfImage(payload) == false) {
			LOGGER.debug("ignoring image data from different sender");
			return;
		}
		LOGGER.debug("checking send flag...");
		if (sendLockFlag.compareAndSet(DORMANT, SENDING)) {
			try {
				if (websocketSessionActive == false) {
					LOGGER.debug("iterating through connected websocket sessions...");
					List<ConnectedSession> websocketSessions = dataServer.getWebsocketSessions();
					// if it's a property on a class, it can be synchronized
					// 'locally'
					synchronized (websocketSessions) {
						// iterate through all the connected websocket sessions
						// and find the one this bean (specific to user and
						// view)
						// is associated with so that we can receive images
						for (ConnectedSession session : websocketSessions) {
							// check if it matches our artificially generated ID
							if (session.getSocketClientId() == this.websocketStreamId) {
								LOGGER.debug("found connected session");
								// now that we've found it, cache it
								websocketSession = session;
								websocketSessionActive = true;
								break;
							}
						}
					}
				}
				// encode and send
				LOGGER.trace("sending image data");
				sendImage(encodeImage(payload.getImageData()), websocketSession);
			} finally {
				sendLockFlag.set(DORMANT);
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.warn("send lock is set!! will NOT handle image data");
			}
		}
	}

	public String getStreamId() {
		return String.valueOf(this.websocketStreamId);
	}

	public String getImageEncodingSrcString() {
		return IMAGE_ENC;
	}
	
	public String getImageSocketPath() {
		return ImageClientEndpoint.ENDPOINT_CTX;
	}

	public String getStreamSocketUrl() {
		LOGGER.trace("getStreamSocketUrl");
		StringBuilder url = new StringBuilder();
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		// these values are also available in the EL
		url.append(externalContext.getRequestServerName());
		url.append(":");
		url.append(externalContext.getRequestServerPort());
		url.append(externalContext.getRequestContextPath());
		url.append(getImageSocketPath());
		return url.toString();
	}

	private static String encodeImage(byte[] data) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("decoding image data of length " + data.length);
		}
		return Base64.getEncoder().encodeToString(data);
	}

	private static void sendImage(String data, ConnectedSession session) {
		// ObjectValidator.raiseIfNull(session);
		// LOGGER.debug("sending image through web socket...");
		try {
			// NOTE: if there is an overlapping call to send*(), an
			// illegal state exception is thrown, hence the atomic boolean lock
			session.getSocketSession().getBasicRemote().sendText(data);
		} catch (IOException e) {
			LOGGER.error("exception trapped attempting to post image data to browser");
			e.printStackTrace(); // send to server.log
		}
	}

	private static long generateSocketSessionId() {
		LOGGER.trace("generateSocketSessionId");
		return TimeUtil.nowUTC().getTime();
	}

	private boolean isConnected(ConnectedSession session) {
		if (session != null) {
			return session.getSocketSession().isOpen();
		}
		return true; // allow it to pass through
	}
	
	private void resolveClient() {
		LOGGER.trace("resolveClient");
		String clientName = getSelectedClientName();
		Collection<Client> clients = clientManager.getAvailableClients();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("selectClientName = " + clientName);
			LOGGER.debug("fetched available clients " + clients);
		}
		for (Client client : clients) {
			if (client.getClientName().equals(clientName)) {
				LOGGER.debug("successfully resolved passed client");
				selectedClient = client;
				break;
			}
		}
	}

	private boolean isSenderOfImage(ImageDataReady event) {
		// TODO : use a client ID of some kind instead of the host address?
		String sender = event.getSender().getHostAddress();
		if (LOGGER.isTraceEnabled()) {
			StringBuilder msg = new StringBuilder();
			msg.append("comparing sender-of-data=");
			msg.append(sender);
			msg.append(", with selected-client=");
			msg.append(selectedClient);
			LOGGER.trace(msg);
		}
		// NOTE: selected client may be NULL, which is ok
		if (selectedClient != null) {
			return selectedClient.getSendAddress().equals(sender);
		}
		return false;
	}

	private void sendStartStream(Client client) {
		if (client != null) {
			LOGGER.trace("sendStartStream");
			send(client, STREAM_START_FLAG);
		}
	}

	private void sendStopStream(Client client) {
		if (client != null) {
			LOGGER.trace("sendStopStream");
			send(client, STREAM_STOP_FLAG);
		}
	}

	private void send(Client client, byte code) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("sending code " + code + "  to " + client.getClientName());
		}
		DataDistributor sender = null;
		try {
			try {
				// this should be synchronous
				EventBus ev = new EventBus();
				// bind the local stuff
				sender = new EventBusDataDistributor(ev, InetAddress.getByName(dataServer.getServerBindAddress()),
						new RandomRangePortGenerator(45000, 60000), 200, // packet
																			// size
						1L, // send delay
						new DataSendFailedListener() {
							@Override
							public void onDataSendFailure(byte[] bs, Throwable t) {
								LOGGER.error("failed to send code " + code + " to client " + client);
							}
						});
				// prep send to client
				sender.addSender(clientManager.getClientRxPort(), InetAddress.getByName(client.getSendAddress()) // TODO,
																													// requires
																													// address,
																													// which
																													// I
																													// think
																													// is
																													// inevitable
																													// but...
				);

				ev.post(new DataReadyEvent(new byte[] { code }, code));

			} finally {
				if (sender != null) {
					sender.close();
				}
			}
		} catch (Exception e) {
			LOGGER.error("failed to send stop stream command to " + client);
			e.printStackTrace();
		}
	}
}
