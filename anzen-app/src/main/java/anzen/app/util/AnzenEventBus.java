/**
 * 
 */
package anzen.app.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 * @author mlcs05
 *
 */
public final class AnzenEventBus {
	
	private static final ExecutorService SERVICE = Executors.newSingleThreadExecutor();

	private static final EventBus BUS = new AsyncEventBus(
		AnzenEventBus.class.getSimpleName(), 
		SERVICE
	);
	
	public static EventBus get() {
		return BUS;
	}
	
	public static void shutdown() {
		SERVICE.shutdownNow();
	}
}
