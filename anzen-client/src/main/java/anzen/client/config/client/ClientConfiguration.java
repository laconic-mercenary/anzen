/**
 * 
 */
package anzen.client.config.client;

/**
 * @author stebbinm
 *
 */
public interface ClientConfiguration {

	String getLocalBindAddress();
	
	int getLocalTxBindPortLower();
	
	int getLocalTxBindPortUpper();
	
	int getLocalRxBindPort();

	int getTxPacketSize();
	
	long getTxSendDelayMS();
	
	int getRxBufferSize();
	
	long getSendDelayMS();
		
	String getTxFailureListenerClassname();
	
}
