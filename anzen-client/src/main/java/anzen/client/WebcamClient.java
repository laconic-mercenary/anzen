/**
 * 
 */
package anzen.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.RandomUtils;
import org.apache.log4j.Logger;

import anzen.client.config.WebcamClientConfigurationKeys;
import anzen.client.messages.ClientCapability;
import anzen.client.messages.ClientMessageType;
import anzen.client.messages.Ping;
import anzen.client.messages.PingFactory;
import anzen.client.messages.PingFactoryImpl;
import anzen.server.messages.ServerMessageType;
import anzen.util.SpecialExecutors;
import cerberus.devices.ImageCaptureDevice;
import cerberus.devices.imaging.CapturedImage;
import cerberus.devices.imaging.ImageDimensionsType;
import cerberus.devices.impl.WebcamDeviceManager;

import com.frontier.lib.threading.ThreadUtils;
import com.frontier.shishya.common.RandomRangePortGenerator;
import com.frontier.shishya.distributed.receiving.DataServerManager;
import com.frontier.shishya.distributed.receiving.guava.EventBusServerManager;
import com.frontier.shishya.distributed.receiving.guava.reassembly.DataPayloadReadyEvent;
import com.frontier.shishya.distributed.receiving.util.NewArrayDataPacketReAssembler;
import com.frontier.shishya.distributed.sending.DataDistributor;
import com.frontier.shishya.distributed.sending.DataReadyEvent;
import com.frontier.shishya.distributed.sending.DataSendFailedListener;
import com.frontier.shishya.distributed.sending.guava.EventBusDataDistributor;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;


/**
 * @author mlcs05
 *
 */
public class WebcamClient {
	
	private static final Logger LOGGER = Logger.getLogger(WebcamClient.class.getName());
	
	private static final String CL_SERVER_PARAMS_FILE = "spf";
	
	private static final String CL_CLIENT_PARAMS_FILE = "cpf";
	
	private static final int SERVER_BUFFER_SIZE = 56000;
	
	private static final ExecutorService SERVICE = SpecialExecutors.newBoundedFixedThreadPool(8, 200);
	
	private static final EventBus SERVICE_BUS = new AsyncEventBus("service-bus", SERVICE);
	
	private static final PingFactory<String> PING_FACTORY = new PingFactoryImpl();
	
	private static final long CLIENT_ID = getClientId();
	
	private static final String CLIENT_NAME = getClientName();
	
	private static InetAddress clientAddress = null;
		
	private static final class SendFailure implements DataSendFailedListener {

		@Override
		public void onDataSendFailure(byte[] bs, Throwable t) {
			LOGGER.debug("SEND FAILED");
			System.out.println(("SEND FAILED"));
			throw new RuntimeException(t);
		}
	}
	
	private static final class RxHandler {
		
		@Subscribe
		public void onReceivePayload(DataPayloadReadyEvent event) {
			LOGGER.info("received message from server: " + event.dataType);
			if (ServerMessageType.STREAM_START.dataType() == event.dataType) {
				webcam = true;
				runWebcam = true;
			} else if (ServerMessageType.STREAM_STOP.dataType() == event.dataType) {
				runWebcam = false;
				webcam = false;
			}
		}
		
	}
	
	private static final RxHandler HANDLER = new RxHandler();
	
	private static boolean loop = true;
	
	private static volatile boolean webcam = false;
	
	private static volatile boolean runWebcam = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LOGGER.info("starting");
		LOGGER.debug("handling command line inputs");
		CommandLine cl = handleCommandLine(args);
		validateCommandLine(cl);
		Path spf = Paths.get(cl.getOptionValue(CL_SERVER_PARAMS_FILE));
		Path cpf = Paths.get(cl.getOptionValue(CL_CLIENT_PARAMS_FILE));
		Properties spfp = readProperties(spf);
		Properties cpfp = readProperties(cpf);
		DataServerManager server = null; 
		DataDistributor client = null;
		try {
			server = launchServer(cpfp);
			client = launchDistricutor(cpfp, spfp);
			String deviceName = cpfp.getProperty(WebcamClientConfigurationKeys.CLIENT_DEVICE_NAME.getKey());
			doLoop(deviceName);
		} finally {
			SERVICE.shutdownNow();
			if (server != null)
				server.closeServers();
			if (client != null)
				client.close();
		}
		LOGGER.info("finished");
	}

	private static CommandLine handleCommandLine(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption(CL_SERVER_PARAMS_FILE, true, "Path to the server parameters properties file.");
		options.addOption(CL_CLIENT_PARAMS_FILE, true, "Path to the client parameters properties file.");
		CommandLineParser parser = new DefaultParser();
		return parser.parse(options, args);
	}
	
	private static void validateCommandLine(CommandLine cl) {
		assert cl.hasOption(CL_SERVER_PARAMS_FILE);
		assert cl.hasOption(CL_CLIENT_PARAMS_FILE);
		assert Files.exists(Paths.get(cl.getOptionValue(CL_SERVER_PARAMS_FILE)));
		assert Files.exists(Paths.get(cl.getOptionValue(CL_CLIENT_PARAMS_FILE)));
	}
	
	private static Properties readProperties(Path path) throws IOException {
		Properties props = new Properties();
		try (InputStream is = Files.newInputStream(path)) {
			props.loadFromXML(is);
		}
		return props;
	}
	
	private static DataDistributor launchDistricutor(Properties clientProperties, Properties serverProperties) throws UnknownHostException {
		String addr = (String) clientProperties.get(WebcamClientConfigurationKeys.CLIENT_BIND_ADDR.getKey());
		String svraddr = (String) serverProperties.get(WebcamClientConfigurationKeys.SERVER_ADDRESS.getKey());
		String svrport1 = (String) serverProperties.get(WebcamClientConfigurationKeys.SERVER_PORT_1.getKey());
		String svrport2 = (String) serverProperties.get(WebcamClientConfigurationKeys.SERVER_PORT_2.getKey());
		String svrport3 = (String) serverProperties.get(WebcamClientConfigurationKeys.SERVER_PORT_3.getKey());
		EventBusDataDistributor ebdd = new EventBusDataDistributor(
			SERVICE_BUS, 
			InetAddress.getByName(addr), 
			new RandomRangePortGenerator(56121, 59121), 
			40000, 
			10, 
			new SendFailure()
		);
		ebdd.addSender(Integer.parseInt(svrport1), InetAddress.getByName(svraddr));
		ebdd.addSender(Integer.parseInt(svrport2), InetAddress.getByName(svraddr));
		ebdd.addSender(Integer.parseInt(svrport3), InetAddress.getByName(svraddr));
		return ebdd;
	}
	
	private static DataServerManager launchServer(Properties serverProperties) throws NumberFormatException, UnknownHostException {
		String addr = (String) serverProperties.get(WebcamClientConfigurationKeys.CLIENT_BIND_ADDR.getKey());
		String port = (String) serverProperties.get(WebcamClientConfigurationKeys.CLIENT_BIND_PORT.getKey());
		// NOTE: global variable pass over
		clientAddress = InetAddress.getByName(addr);
		//
		EventBusServerManager server = new EventBusServerManager(
			SERVICE_BUS,
			SERVER_BUFFER_SIZE, 
			true,
			new NewArrayDataPacketReAssembler());
		server.addServer(
			Integer.parseInt(port), 
			clientAddress
		);
		SERVICE_BUS.register(HANDLER);
		return server;
	}
	
	private static void doLoop(String deviceName) throws Exception {
		while (loop) {
			if (webcam) {
				LOGGER.debug("doing webcam");
				doWebcam(deviceName);
			} else {
				LOGGER.debug("sleeping");
				ThreadUtils.sleep(30000L);
				sendPing();
			}
		}
	}
	
	private static void doWebcam(String devName) throws IOException {
		WebcamDeviceManager deviceManager = new WebcamDeviceManager();
		List<ImageCaptureDevice> devices = deviceManager.listCaptureDevices();
		if (!devices.isEmpty()) {
			ImageCaptureDevice device = null;
			for (ImageCaptureDevice dev : devices) {
				if (dev.getName().contains(devName)) {
					LOGGER.debug("will use device: " + dev.getName());
					device = dev;
					break;
				} else {
					LOGGER.debug("ignoring device: " + dev.getName());
				}
			}
			device.setImageDimensions(ImageDimensionsType.HIGHEST);
			LOGGER.debug("opening device");
			device.open();
			try {
				while (runWebcam) {
					CapturedImage image = device.captureImage();
					ThreadUtils.sleep(50L);
					sendOff(image);
				}
			} finally {
				device.close();
			}
			LOGGER.debug("closed device");
		} else {
			LOGGER.warn("no devices are connected to the machine");
		}
	}
	
	private static void sendOff(CapturedImage image) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			image.writeTo(baos);
			SERVICE_BUS.post(new DataReadyEvent(baos.toByteArray(), ClientMessageType.STREAM_IMAGE.dataType()));
		}
	}
	
	private static void sendPing() {
		Ping data = PING_FACTORY.create(
			CLIENT_ID,
			CLIENT_NAME,
			getSenderAddress(),
			_getCapabilities()
		);
		
		String dataPayload = PING_FACTORY.convertToInput(data);
		
		SERVICE_BUS.post(
			new DataReadyEvent(
				dataPayload.getBytes(Charsets.UTF_8), 
				ClientMessageType.PING.dataType()
			)
		);
	}
	
	private static String getClientName() {
		String name = _getMachineName();
		if (name == null) {
			name = _getHostName();
			if (name == null) {
				name = _getLastResortName();
			}
		}
		return name;
	}
	
	private static ClientCapability[] _getCapabilities() {
		// TODO revise and make configurable
		return new ClientCapability[] { ClientCapability.VIDEO_STREAM };
	}
	
	private static String _getMachineName() {
	    Map<String, String> env = System.getenv();
	    String name = null;
	    if (env.containsKey("COMPUTERNAME")) {
	        name = env.get("COMPUTERNAME");
	    } else if (env.containsKey("HOSTNAME")) {
	        name = env.get("HOSTNAME");
	    }
	    if (name == null) {
	    	LOGGER.warn("unable to retrieve machine name");
	    }
	    return name;
	}
	
	private static String _getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOGGER.warn("unable to retrieve hostname");
			return null;
		}
	}
	
	private static String _getLastResortName() {
		String name = WebcamClient.class.getSimpleName() + "_" + CLIENT_ID;
		LOGGER.warn("using last resort client name of: " + name);
		return name;
	}
	
	private static long getClientId() {
		long ts = System.currentTimeMillis();
		if (ts % 2 == 0) {
			ts += RandomUtils.nextLong(2L, Long.MAX_VALUE / 3L);
		} else {
			ts -= RandomUtils.nextLong(2L, 999999L);
		}
		return ts;
	}
	
	private static String getSenderAddress() {
		return clientAddress.toString();
	}
}
