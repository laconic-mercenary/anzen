/**
 * 
 */
package anzen.app.endpoints;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.log4j.Logger;

import anzen.app.endpoints.sessions.ConnectedSession;
import anzen.app.inject.data.DataServer;

/**
 * @author mlcs05
 */
@ServerEndpoint(ImageClientEndpoint.ENDPOINT_CTX)
public final class ImageClientEndpoint {
	
	private static final Logger LOGGER = Logger.getLogger(ImageClientEndpoint.class.getName());
	
	public static final String ENDPOINT_CTX = "/images";
	
	private final Lock tempSessionLock = new ReentrantLock();
	
	private final List<Session> tempSessionCache = new LinkedList<>();
	
	@Inject
	private DataServer dataServer;
	
	@OnOpen
	public void onSocketOpen(Session session) {
		LOGGER.trace("onSocketOpen");
		
		if (LOGGER.isDebugEnabled()) { 
			LOGGER.debug("websocket session OPENED, ID=" + session.getId());
		}
		
		tempSessionLock.lock();
		try {
			tempSessionCache.add(session);
		} finally {
			tempSessionLock.unlock();
		}
		
		if (LOGGER.isDebugEnabled()) {
			StringBuilder msg = new StringBuilder("websocket session report: ");
			msg.append(tempSessionCache.size());
			msg.append(" sessions are now temporarily cached, ");
			msg.append(dataServer.getWebsocketSessions().size());
			msg.append(" sessions are now active");
			LOGGER.debug(msg);
		}
	}
	
	@OnClose
	public void onSocketClose(Session session) {
		LOGGER.trace("onSocketClose");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("websocket session CLOSED, ID=" + session.getId());
		}
		
		List<ConnectedSession> connectedSessions = this.dataServer.getWebsocketSessions();
		// check connected clients
		synchronized(connectedSessions) {
			// synchronizing locally apparently works
			Iterator<ConnectedSession> itr = connectedSessions.iterator();
			while (itr.hasNext()) {
				ConnectedSession existingSession = itr.next();
				// session is obviously discarded, so remove it from the list of existing sessions
				if (existingSession.getSocketSession().getId().equals(session.getId())) {
					LOGGER.debug("removing session: " + session.getId());
					itr.remove();
					break;
				}
			}
		}
		// also check cache in-case a message wasn't sent from the browser
		this.tempSessionLock.lock();
		try {
			Iterator<Session> itr = this.tempSessionCache.iterator();
			while (itr.hasNext()) {
				// odd case if this was in the temp-cache...
				// could only mean the browser didn't send back a 'confirmation'
				// that the session was established
				if (itr.next().getId().equals(session.getId())) {
					LOGGER.debug("removing temp session: " + session.getId());
					itr.remove();
					break;
				}
			}
		} finally {
			this.tempSessionLock.unlock();
		}
		
		if (LOGGER.isDebugEnabled()) {
			StringBuilder msg = new StringBuilder("websocket session report: ");
			msg.append(tempSessionCache.size());
			msg.append(" sessions are now temporarily cached, ");
			msg.append(connectedSessions.size());
			msg.append(" sessions are now active");
			LOGGER.debug(msg);
		}
	}
	
	@OnMessage
	public void onSocketMessageRx(String message, Session session) {
		// fires when we receive a message from the browser (via the web socket)
		// right now the message is just a generated session ID
		// that comes from the browser. it's really the only way to associate a browser
		// session with websocket session server-side
		LOGGER.trace("onSocketMessageRx");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("websocket message RX: " + message);
		}
		this.tempSessionLock.lock();
		try {
			// should have the session in our temp cache
			// find it, them remove it, then put it in our 
			// first-class cache
			Iterator<Session> itr = this.tempSessionCache.iterator();
			while (itr.hasNext()) {
				Session nextSession = itr.next();
				if (nextSession.getId().equals(session.getId())) {
					// remove from temp cache
					itr.remove();

					// connection session contains the actual socket session
					// and the generated ID
					ConnectedSession newSession = new ConnectedSession(
						nextSession, 
						Long.parseLong(message)
					);
					
					// synchronize locally to add the new session
					List<ConnectedSession> establishedSessions = this.dataServer.getWebsocketSessions();
					synchronized(establishedSessions) {
						establishedSessions.add(newSession);
					}
					if (LOGGER.isDebugEnabled()) {
						StringBuilder msg = new StringBuilder();
						msg.append("added new connected session information ");
						msg.append(newSession);
						LOGGER.debug(msg);
					}
					break;
				}
			}
		} finally {
			this.tempSessionLock.unlock();
		}
	}
}
