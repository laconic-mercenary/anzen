/**
 * 
 */
package anzen.client.util;

import java.util.Scanner;

/**
 * @author stebbinm
 *
 */
public final class UserInputTask implements Runnable {

	@Override
	public void run() {
		System.out.println("Press Any Key to EXIT");
		try (Scanner scanner = new Scanner(System.in)) {
			scanner.nextLine();
			System.exit(0);
		}
	}

}
