/**
 * 
 */
package anzen.app.inject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import com.frontier.lib.time.TimeUtil;

import anzen.app.inject.client.ClientManager;
import anzen.app.inject.data.DataGateway;
import anzen.configuration.properties.PropertiesAnnotations;
import anzen.configuration.properties.PropertiesBound;
import anzen.configuration.properties.PropertiesField;

/**
 * @author mlcs05
 */
@Named(value = "applicationBean")
@ApplicationScoped()
@PropertiesBound(propertiesName = "Application")
public class Application {
	
	private static final Logger LOGGER = Logger.getLogger(Application.class.getName());
	
	@Inject
	private ClientManager clientManager;
	
	@Inject
	private DataGateway dataGateway;
	
	@PropertiesField(key = "application.title_message", type = String[].class)
	private String[] titleMessages = new String[0];
	
	@PropertiesField(key = "application.app_name", type = String.class)
	private String appName;
	
	@PostConstruct
	public void onApplicationStart() {
		LOGGER.info("application started");
		PropertiesAnnotations.loadConfiguration(this);
		dataGateway.toString(); // force weld to instantiate, it's lazy
		clientManager.toString();
	}
	
	@PreDestroy
	public void onApplicationShutdown() {
		LOGGER.info("application shutdown");
	}
	
	public String getNextTitle() {
		int idx = TimeUtil.randomInt(titleMessages.length);
		return titleMessages[idx];
	}
	
	public String getApplicationName() {
		return appName;
	}
}
