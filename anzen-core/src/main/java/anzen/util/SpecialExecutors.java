/**
 * 
 */
package anzen.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;

import com.frontier.lib.validation.NumberValidation;

/**
 * @author mlcs05
 *
 */
public abstract class SpecialExecutors {

	public static ExecutorService newBoundedFixedThreadPool(
			int nThreads, int capacity) {
		return newBoundedFixedThreadPool(nThreads, capacity, 0L);
	}
	
	public static ExecutorService newBoundedFixedThreadPool(
			int nThreads, int capacity,	long keepAliveTimeMS) {
		return newBoundedFixedThreadPool(nThreads, capacity, keepAliveTimeMS, new DiscardPolicy());
	}
	
	public static ThreadPoolExecutor newBoundedFixedThreadPool(
			int nThreads, 
			int capacity,	
			long keepAliveTimeMS, 
			RejectedExecutionHandler rejectionHandler) {
		NumberValidation.raiseIfLessThan(nThreads, 1L);
		NumberValidation.raiseIfLessThan(capacity, 1L);
		NumberValidation.raiseIfLessThan(keepAliveTimeMS, 0L);
		return new ThreadPoolExecutor(
			nThreads, 
			nThreads,
			0L, 
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(capacity),
			rejectionHandler
		);
	}
}
