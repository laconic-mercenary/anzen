/**
 * 
 */
package anzen.client;

import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import anzen.client.util.UserInputTask;

/**
 * @author stebbinm
 *
 */
public class AnzenDesktopClient {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AnzenClient anzenClient = new AnzenClient(Thread.currentThread(), null, null, Executors.newSingleThreadExecutor());
		try {
			// create user input thread to accept
			// input from the user	
			Thread userInputThread = new Thread(new UserInputTask());
			userInputThread.start();
			
			// this will block until the semaphore is released
			// or System.exit() is called
			anzenClient.startClient(); 
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			anzenClient.cleanup();
		}
	}

}
