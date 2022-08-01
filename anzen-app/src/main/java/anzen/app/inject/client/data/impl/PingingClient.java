/**
 * 
 */
package anzen.app.inject.client.data.impl;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import anzen.app.inject.client.data.Client;
import anzen.client.messages.ClientCapability;

/**
 * @author mlcs05
 *
 */
final class PingingClient implements Client {
	
	private final String sendAddress;
	
	private final String clientName;
	
	private final long clientId;
	
	private final ClientCapability[] capabilities;
	
	private long lastPingTime = -1L;
	
	public PingingClient(long id, String name, String address, ClientCapability[] capabilities) {
		this.clientId = id;
		this.clientName = name;
		this.sendAddress = address;
		this.capabilities = capabilities;
	}

	@Override
	public String getSendAddress() {
		return sendAddress;
	}

	@Override
	public String getClientName() {
		return clientName;
	}

	@Override
	public long getClientId() {
		return clientId;
	}

	@Override
	public long getLastPingTime() {
		return lastPingTime;
	}

	@Override
	public void setLastPingTime(long pingTime) {
		this.lastPingTime = pingTime;
	}
	
	@Override
	public ClientCapability[] getCapabilities() {
		return capabilities;
	}

	@Override
	public String toString() {
		ToStringBuilder tostr = new ToStringBuilder(this);
		tostr.append(getClientId());
		tostr.append(getClientName());
		tostr.append(getSendAddress());
		tostr.append(getLastPingTime());
		tostr.append(getCapabilities());
		return tostr.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean equal = false;
		if (obj instanceof PingingClient) {
			PingingClient pc = (PingingClient)obj;
			EqualsBuilder equals = new EqualsBuilder();
			equals.append(getClientName(), pc.getClientName());
			equals.append(getClientId(), pc.getClientId());
			equals.append(getSendAddress(), pc.getSendAddress());
			equals.append(getCapabilities(), pc.getCapabilities());
			equal = equals.isEquals();
		}
		return equal;
	}
	
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(getClientId());
		hcb.append(getClientName());
		hcb.append(getSendAddress());
		hcb.append(getLastPingTime());
		hcb.append(getCapabilities());
		return hcb.toHashCode();
	}
}
