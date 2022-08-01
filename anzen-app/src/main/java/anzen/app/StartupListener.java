/**
 * 
 */
package anzen.app;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import anzen.app.inject.Application;

/**
 * @author mlcs05
 *
 */
public final class StartupListener implements ServletContextListener {

	@Inject
	private Application app; // leave as is

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		app.toString(); // force weld to instantiate it
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// do nothing
	}

}
