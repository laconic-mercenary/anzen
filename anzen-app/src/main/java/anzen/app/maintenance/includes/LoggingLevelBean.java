/**
 * 
 */
package anzen.app.maintenance.includes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author mlcs05
 *
 */
@Named
@ViewScoped
public class LoggingLevelBean implements Serializable {

	private static final Logger LOGGER = Logger
			.getLogger(LoggingLevelBean.class.getName());

	private static final long serialVersionUID = 201603162125L;
	
	private static final String DEFAULT_PACKAGE = "anzen.app";

	private String targetPackage = DEFAULT_PACKAGE;

	private int selectedLogLevel = Level.INFO_INT;

	public String changeLevelAction() {
		LOGGER.trace("changeLevelAction()");

		String tgtPackage = getTargetPackage();
		final int tgtLevel = getSelectedLogLevel();
		Level level = Level.toLevel(tgtLevel);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("target package was: " + tgtPackage);
			LOGGER.debug("selected logging level was: " + tgtLevel);
		}

		LOGGER.info("logging level will be updated to: " + level);
		Logger.getLogger(tgtPackage).setLevel(level);
		postMessage(level);
		return null;
	}

	public void setTargetPackage(String targetPackage) {
		this.targetPackage = targetPackage;
	}

	public String getTargetPackage() {
		return targetPackage;
	}

	public void setSelectedLogLevel(int selectedLogLevel) {
		this.selectedLogLevel = selectedLogLevel;
	}

	public int getSelectedLogLevel() {
		return selectedLogLevel;
	}

	public List<Level> getAvailableLogLevels() {
		return Arrays.asList(
				Level.OFF, 
				Level.FATAL, 
				Level.ERROR, 
				Level.WARN,
				Level.INFO, 
				Level.DEBUG, 
				Level.TRACE, 
				Level.ALL);
	}
	
	private void postMessage(Level level) {
		FacesMessage msg = new FacesMessage("(" + level.toString() + ")");
		msg.setSeverity(FacesMessage.SEVERITY_INFO);
		FacesContext.getCurrentInstance()
					.addMessage("available-log-levels-select", msg);
	}
}
