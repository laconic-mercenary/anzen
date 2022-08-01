/**
 * 
 */
package anzen.app.client;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import anzen.app.inject.Application;
import anzen.app.inject.client.ClientManager;
import anzen.app.inject.client.data.Client;

/**
 * @author mlcs05
 */
@Named
@RequestScoped()
public class ClientBean {
	
	private static final Logger LOGGER = Logger.getLogger(ClientBean.class.getName());

	private String selectedClient = null;
		
	@Inject
	private ClientManager clientManager;
	
	@Inject
	private Application application;
	
	public List<Client> getClients() {
		LOGGER.debug("fetching available clients from manager");
		return clientManager.getAvailableClients();
	}
	
	public String getSelectedClient() {
		return selectedClient;
	}
	
	public void setSelectedClient(String selectedClient) {
		this.selectedClient = selectedClient;
	}
	
	public String getApplicationName() {
		return application.getApplicationName();
	}
	
	public void onClientSelected(AjaxBehaviorEvent event) throws IOException {
		LOGGER.trace("onClientSelected");
		FacesContext ctx = FacesContext.getCurrentInstance();
		String selectClient = getSelectedClient();
		if (selectClient != null) {
			ExternalContext externalContext = ctx.getExternalContext();
			StringBuilder redirect = new StringBuilder();
			redirect.append(externalContext.getRequestContextPath());
			redirect.append("/client/client-stream.jsf?");
			redirect.append("faces-redirect=true&clientname=");
			redirect.append(selectClient);
			String url = redirect.toString();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("navigating to: " + url);
			}
			externalContext.redirect(url);
		} else {
			LOGGER.debug("no client was selected");
			FacesMessage msg = new FacesMessage();
			msg.setSummary("No client was selected.");
			msg.setSeverity(FacesMessage.SEVERITY_WARN);
			ctx.addMessage(null, msg);
		}
	}
}
