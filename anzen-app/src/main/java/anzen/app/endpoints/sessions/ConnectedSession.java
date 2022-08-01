/**
 * 
 */
package anzen.app.endpoints.sessions;

import javax.websocket.Session;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.frontier.lib.validation.ObjectValidator;

/**
 * @author mlcs05
 *
 */
public final class ConnectedSession {

	private final Session socketSession;
	
	private final long socketClientId;

	public ConnectedSession(Session socketSession, long socketClientId) {
		ObjectValidator.raiseIfNull(socketSession);
		this.socketSession = socketSession;
		this.socketClientId = socketClientId;
	}
	
	public long getSocketClientId() {
		return socketClientId;
	}

	public Session getSocketSession() {
		return socketSession;
	}
	
	@Override
	public String toString() {
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append(getSocketSession().getId());
		ts.append(getSocketClientId());
		return ts.toString();
	}
	
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(getSocketClientId());
		hcb.append(getSocketSession().getId());
		return hcb.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConnectedSession) {
			ConnectedSession comparison = (ConnectedSession) obj;
			EqualsBuilder equalsBuilder = new EqualsBuilder();
			equalsBuilder.append(
				socketClientId,
				comparison.socketClientId
			);
			equalsBuilder.append(
				socketSession.getId(),
				comparison.getSocketSession().getId()
			);
			return equalsBuilder.isEquals();
		} else {
			if (obj instanceof Session) {
				Session comparison = (Session)obj;
				return socketSession.getId().equals(
					comparison.getId()
				);
			}
		}
		return false;
	}
}
