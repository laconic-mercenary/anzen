/**
 * 
 */
package anzen.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import com.frontier.shishya.common.RandomRangePortGenerator;
import com.frontier.shishya.distributed.receiving.DataServerManager;
import com.frontier.shishya.distributed.receiving.guava.EventBusServerManager;
import com.frontier.shishya.distributed.receiving.guava.reassembly.DataPayloadReadyEvent;
import com.frontier.shishya.distributed.receiving.util.NewArrayDataPacketReAssembler;
import com.frontier.shishya.distributed.sending.DataDistributor;
import com.frontier.shishya.distributed.sending.DataDistributorFactory;
import com.frontier.shishya.distributed.sending.DataReadyEvent;
import com.frontier.shishya.distributed.sending.DataSendFailedListener;
import com.frontier.shishya.distributed.sending.guava.EventBusDataDistributorFactory;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import anzen.client.config.AnzenClientConfiguration;
import anzen.client.config.client.ClientConfiguration;
import anzen.client.config.server.ServerConfiguration;
import anzen.device.Device;
import anzen.device.DeviceException;
import anzen.server.messages.ServerMessageType;

/**
 * @author stebbinm
 */
public final class AnzenClient implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(AnzenClient.class.getName());

	private final Semaphore exitSemaphore = new Semaphore(0);
	private final Thread shutdownHook;
	private final Thread mainThread;

	private final AnzenClientConfiguration config;
	private final Device masterDevice;

	private final EventBus commEventBus;
	
	private DataDistributor commTx;
	private DataServerManager commRx;
	
	private final Lock pendingCommandLock = new ReentrantLock();
	private PendingCommand pendingCommand = PendingCommand.NONE;

	public AnzenClient(Thread mainThread, AnzenClientConfiguration config, Device masterDevice,
			ExecutorService commThreadPool) {
		Validate.notNull(mainThread);
		Validate.notNull(config);
		Validate.notNull(config.getClientConfiguration());
		Validate.notNull(config.getServerConfiguration());
		Validate.notNull(masterDevice);

		this.shutdownHook = new Thread(this);
		this.mainThread = mainThread;
		this.config = config;
		this.masterDevice = masterDevice;
		this.commEventBus = new AsyncEventBus(getClass().getSimpleName() + "-commThreadPool", commThreadPool);		
	}

	public void startClient() throws UnknownHostException {
		LOGGER.finest("startClient");
		addShutdownHook();
		try {
			// launch other threads and stuff here
			// with the intention that another object will call signalExit()
			// possibly or the JVM will shutdown
			LOGGER.info("creating communication assets...");
			commRx = createService();
			commTx = createClient();
			commEventBus.register(this); // receiving data from server
			createClients(config.getServerConfiguration(), commTx);
			
			LOGGER.fine("waiting for exit signal...");
			waitForExitSignal();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void cleanup() {
		LOGGER.finest("cleanup");
		LOGGER.info("stopping all client activity...");
		stopDevice();
		stopComms();
		LOGGER.info("stopped client");
	}

	public void signalExit() {
		LOGGER.finest("signalExit");
		exitSemaphore.release();
	}

	@Override
	public void run() {
		LOGGER.finest("run");
		signalExit();
		try {
			LOGGER.fine("joining main thread");
			mainThread.join(); // pauses until main terminates
		} catch (InterruptedException ie) {
			// think this current thread should interrupt as well?
			LOGGER.warning("interrupted after joining main thread");
			LOGGER.fine("interrupting current thread...");
			Thread.currentThread().interrupt();
		}
	}
	
	@Subscribe
	public void dataReceived(DataPayloadReadyEvent event) {
		ServerMessageType message = ServerMessageType.fromByte(event.dataType);
		setPendingCommand(message);
	}
	
	private void setPendingCommand(ServerMessageType serverMessage) {
		pendingCommandLock.lock();
		try {
			if (ServerMessageType.STREAM_START.equals(serverMessage)) {
				pendingCommand = PendingCommand.START_STREAM;
			} else if (ServerMessageType.STREAM_STOP.equals(serverMessage)) {
				pendingCommand = PendingCommand.STOP_STREAM;
			} else {
				pendingCommand = PendingCommand.NONE;
			}
		} finally {
			pendingCommandLock.unlock();
		}
	}

	private void waitForExitSignal() throws InterruptedException {
		LOGGER.finest("waitForExitSignal");
		exitSemaphore.acquire();
	}

	private void addShutdownHook() {
		LOGGER.finest("addShutdownHook");
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private void removeShutdownHook() {
		LOGGER.finest("removeShutdownHook");
		Runtime.getRuntime().removeShutdownHook(shutdownHook);
	}

	private void openDevice() {
		if (masterDevice.isOpen() == false) {
			LOGGER.info("device is not open, attempting to start device...");
			try {
				masterDevice.open();
			} catch (DeviceException e) {
				LOGGER.severe("unable to start device");
				LOGGER.severe(e.getMessage());
			}
		}
	}

	private void runDevice() {
		/**
		 * handles the cases of: > device is newly ready > device is closed
		 */
		LOGGER.finest("runDevice");
		if (masterDevice.isOpen()) {
			LOGGER.fine("device is open, starting loop...");
			while (true) {
				try {
					byte[] data = masterDevice.getCurrentData();
					if (data == null) {
						LOGGER.fine("null data returned, stopping device loop");
						break;
					}
					if (LOGGER.isLoggable(Level.FINEST)) {
						LOGGER.finest("posting image data of length " + data.length);
					}
					commEventBus.post(new DataReadyEvent(data));
				} catch (DeviceException e) {
					LOGGER.severe("error attempting to capture current data");
					LOGGER.severe(e.getMessage());
				}
			}
		}
	}

	private void stopDevice() {
		LOGGER.finest("stopDevice");
		if (masterDevice.isOpen()) {
			LOGGER.fine("device is open, attempting to close...");
			try {
				masterDevice.close();
				LOGGER.info("device [" + masterDevice.getId() + "] closed");
			} catch (DeviceException e) {
				LOGGER.severe("error on stopping device...");
				LOGGER.severe(e.getMessage());
			}
		}
	}

	private void stopComms() {
		LOGGER.finest("stopComms");
		try {
			LOGGER.fine("closing distributor...");
			commTx.close();
		} catch (IOException e) {
			LOGGER.severe("error on closing distributor");
			LOGGER.severe(e.getMessage());
		}

		// TODO kill the command receiver as well
	}

	private static DataDistributorFactory createClientFactory(ClientConfiguration config, EventBus commEventBus)
			throws UnknownHostException {
		DataDistributorFactory factory = new EventBusDataDistributorFactory(
				InetAddress.getByName(config.getLocalBindAddress()),
				new RandomRangePortGenerator(config.getLocalTxBindPortLower(), config.getLocalTxBindPortUpper(), true),
				commEventBus, config.getTxPacketSize(), config.getSendDelayMS(), new DataSendFailedListener() {
					@Override
					public void onDataSendFailure(byte[] bs, Throwable t) {
						LOGGER.severe("Unable to send data of payload length " + bs.length);
						LOGGER.severe(t.getMessage());
					}
				});
		return factory;
	}

	private static void createClients(ServerConfiguration config, DataDistributor distributor)
			throws UnknownHostException {
		InetAddress targetAddress = InetAddress.getByName(config.getServerAddress());
		for (int port : config.getServerPorts()) {
			distributor.addSender(port, targetAddress);
		}
	}
	
	private DataDistributor createClient() throws UnknownHostException {
		DataDistributorFactory factory = createClientFactory(config.getClientConfiguration(), commEventBus);
		return factory.create();		
	}
	
	private DataServerManager createService() {
		return new EventBusServerManager(
				this.commEventBus, 
				this.config.getClientConfiguration().getRxBufferSize(), 
				true, 
				new NewArrayDataPacketReAssembler());
	}
}
