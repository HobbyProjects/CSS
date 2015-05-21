package com.CssServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.net.ssl.SSLSocketFactory;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;

import com.CssServer.GcmPacketExtension;
import com.CssServer.Database;

public class CcsServer {
	public static final Logger logger = Logger.getLogger(CcsServer.class.getName());
	public static final String GCM_SERVER = "gcm.googleapis.com";
	public static final int GCM_PORT = 5235;
	public static final String GCM_SENDER_ID = "908527372899";
	public static final String GCM_SERVER_KEY = "AIzaSyAGlPY70VXA4wJAOlAnxqmESnvIzMfbsrU";
	public static final String GCM_ELEMENT_NAME = "gcm";
	public static final String GCM_NAMESPACE = "google:mobile:data";
	public static final String DATABASE_FILE = "C:/Users/Hi/Documents/GitHub/CSS/GSM_CCS_SERVER/databases/test.db";
	public static final String LOG_FILENAME = "./CcsServer.log";

	static Random random = new Random();
	XMPPConnection connection;
	ConnectionConfiguration config;
	Database database;

	public CcsServer() {
		/*try {
			database = new Database(DATABASE_FILE);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		initializeLogging(LOG_FILENAME);
		ProviderManager.getInstance().addExtensionProvider(GCM_ELEMENT_NAME,
				GCM_NAMESPACE, new PacketExtensionProvider() {
					@Override
					public PacketExtension parseExtension(XmlPullParser parser)
							throws Exception {
						String json = parser.nextText();
						GcmPacketExtension packet = new GcmPacketExtension(json);
						return packet;
					}
				});
	}

	/**
	 * Initializes the logger.
	 * 
	 * <p>
	 * The logs are directed to a file specified by logFilename.
	 * 
	 * @param logFilename
	 *        The name of the log file.
	 */
	private void initializeLogging(String logFilename) {
		Handler fileHandler = null;
		Formatter simpleFormatter = null;
		try {
			fileHandler = new FileHandler(logFilename);
			simpleFormatter = new SimpleFormatter();
			logger.addHandler(fileHandler);
			fileHandler.setFormatter(simpleFormatter);
			fileHandler.setLevel(Level.ALL);
			logger.setLevel(Level.ALL);
			logger.info("Logging started...");
			
			if(database != null) {
				database.initializeLogging(fileHandler, simpleFormatter);
			}

		} catch (IOException exception) {
			logger.log(Level.SEVERE, "Unable to redirect the log to a file: ", exception);
		}
	}
	/**
	 * Returns message id to uniquely identify a message.
	 * 
	 * <p>
	 * Unique message ids will be produced.
	 * 
	 */
	public String getRandomMessageId() {
		String msg_uuid =  UUIDGenerator.generate();
		if (msg_uuid.isEmpty())
		{
			// Fall back on a weak ID generator
			logger.log(Level.SEVERE, "UUIDGenerator failed to produce UUID");
			return "m-" + Long.toString(random.nextLong());
		}
		return msg_uuid;
	}

	/**
	 * Sends a downstream GCM message.
	 */
	public void send(String jsonRequest) {
		Packet request = new GcmPacketExtension(jsonRequest).toPacket();
		connection.sendPacket(request);
	}

	/**
	 * Handles an upstream data message from a device application.
	 * 
	 * <p>
	 * Subclasses should override this method to process an upstream message.
	 */
	public void handleIncomingDataMessage(Map<String, Object> jsonObject) {
		@SuppressWarnings("unchecked")
		Map<String, String> payload = (Map<String, String>) jsonObject
				.get("data");

		String from = jsonObject.get("from").toString();
		// PackageName of the application that sent this message.
		String category = jsonObject.get("category").toString();
	
		logger.log(Level.INFO, "Application: " + category);
		logger.log(Level.INFO, "From: " + from);
		
		// Handle what happens with upstream messages
		Iterator<String> iterator = payload.keySet().iterator();
		while(iterator.hasNext())
		{
			String key = iterator.next();
			String value = payload.get(key);
			logger.log(Level.INFO, "Data.key: " + key + " Data.value: " + value);
		}
	}

	/**
	 * Handles an ACK.
	 * 
	 * <p>
	 * By default, it only logs a INFO message, but subclasses could override it
	 * to properly handle ACKS.
	 */
	public void handleAckReceipt(Map<String, Object> jsonObject) {
		String messageId = jsonObject.get("message_id").toString();
		String from = jsonObject.get("from").toString();
		logger.log(Level.INFO, "handleAckReceipt() from: " + from
				+ ", messageId: " + messageId);
	}

	/**
	 * Handles a NACK.
	 * 
	 * <p>
	 * By default, it only logs a INFO message, but subclasses could override it
	 * to properly handle NACKS.
	 */
	public void handleNackReceipt(Map<String, Object> jsonObject) {
		String messageId = jsonObject.get("message_id").toString();
		String from = jsonObject.get("from").toString();
		logger.log(Level.INFO, "handleNackReceipt() from: " + from
				+ ", messageId: " + messageId);
	}

	/**
	 * Creates a JSON encoded GCM message.
	 * 
	 * @param to
	 *            RegistrationId of the target device (Required).
	 * @param messageId
	 *            Unique messageId for which CCS will send an "ack/nack"
	 *            (Required).
	 * @param payload
	 *            Message content intended for the application. (Optional).
	 * @param collapseKey
	 *            GCM collapse_key parameter (Optional).
	 * @param timeToLive
	 *            GCM time_to_live parameter (Optional).
	 * @param delayWhileIdle
	 *            GCM delay_while_idle parameter (Optional).
	 * @return JSON encoded GCM message.
	 */
	public static String createJsonMessage(String to, String messageId,
			Map<String, String> payload, String collapseKey, Long timeToLive,
			Boolean delayWhileIdle) {
		Map<String, Object> message = new HashMap<String, Object>();
		message.put("to", to);
		if (collapseKey != null) {
			message.put("collapse_key", collapseKey);
		}
		if (timeToLive != null) {
			message.put("time_to_live", timeToLive);
		}
		if (delayWhileIdle != null && delayWhileIdle) {
			message.put("delay_while_idle", true);
		}
		message.put("message_id", messageId);
		message.put("data", payload);
		return JSONValue.toJSONString(message);
	}

	/**
	 * Creates a JSON encoded ACK message for an upstream message received from
	 * an application.
	 * 
	 * @param to
	 *            RegistrationId of the device who sent the upstream message.
	 * @param messageId
	 *            messageId of the upstream message to be acknowledged to CCS.
	 * @return JSON encoded ack.
	 */
	public static String createJsonAck(String to, String messageId) {
		Map<String, Object> message = new HashMap<String, Object>();
		message.put("message_type", "ack");
		message.put("to", to);
		message.put("message_id", messageId);
		return JSONValue.toJSONString(message);
	}

	/**
	 * Connects to GCM Cloud Connection Server using the supplied credentials.
	 * 
	 * @param username
	 *            GCM_SENDER_ID@gcm.googleapis.com
	 * @param password
	 *            API Key
	 * @throws XMPPException
	 */
	public void connect(String username, String password) throws XMPPException {
		config = new ConnectionConfiguration(GCM_SERVER, GCM_PORT);
		config.setSecurityMode(SecurityMode.enabled);
		config.setReconnectionAllowed(true);
		config.setRosterLoadedAtLogin(false);
		config.setSendPresence(false);
		config.setSocketFactory(SSLSocketFactory.getDefault());

		// NOTE: Set to true to launch a window with information about packets
		// sent and received
		config.setDebuggerEnabled(true);

		XMPPConnection.DEBUG_ENABLED = true;

		connection = new XMPPConnection(config);
		connection.connect();

		connection.addConnectionListener(new ConnectionListener() {

			@Override
			public void reconnectionSuccessful() {
				logger.info("Reconnecting..");
			}

			@Override
			public void reconnectionFailed(Exception e) {
				logger.log(Level.INFO, "Reconnection failed.. ", e);
			}

			@Override
			public void reconnectingIn(int seconds) {
				logger.log(Level.INFO, "Reconnecting in %d secs", seconds);
			}

			@Override
			public void connectionClosedOnError(Exception e) {
				logger.log(Level.INFO, "Connection closed on error.");
			}

			@Override
			public void connectionClosed() {
				logger.info("Connection closed.");
			}
		});

		// Handle incoming packets
		connection.addPacketListener(new PacketListener() {

			@Override
			public void processPacket(Packet packet) {
				logger.log(Level.INFO, "Received: " + packet.toXML());
				Message incomingMessage = (Message) packet;
				GcmPacketExtension gcmPacket = (GcmPacketExtension) incomingMessage
						.getExtension(GCM_NAMESPACE);
				String json = gcmPacket.getJson();
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> jsonObject = (Map<String, Object>) JSONValue
							.parseWithException(json);

					// present for "ack"/"nack", null otherwise
					Object messageType = jsonObject.get("message_type");

					if (messageType == null) {
						// Normal upstream data message
						handleIncomingDataMessage(jsonObject);

						// Send ACK to CCS
						String messageId = jsonObject.get("message_id")
								.toString();
						String from = jsonObject.get("from").toString();
						String ack = createJsonAck(from, messageId);
						send(ack);
					} else if ("ack".equals(messageType.toString())) {
						// Process Ack
						handleAckReceipt(jsonObject);
					} else if ("nack".equals(messageType.toString())) {
						// Process Nack
						handleNackReceipt(jsonObject);
					} else {
						logger.log(Level.WARNING,
								"Unrecognized message type (%s)",
								messageType.toString());
					}
				} catch (ParseException e) {
					logger.log(Level.SEVERE, "Error parsing JSON " + json, e);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Couldn't send echo.", e);
				}
			}
		}, new PacketTypeFilter(Message.class));

		// Log all outgoing packets
		connection.addPacketInterceptor(new PacketInterceptor() {
			@Override
			public void interceptPacket(Packet packet) {
				logger.log(Level.INFO, "Sent: {0}", packet.toXML());
			}
		}, new PacketTypeFilter(Message.class));

		connection.login(username, password);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final String userName = GCM_SENDER_ID + "@gcm.googleapis.com";
		final String password = GCM_SERVER_KEY;
		
		CcsServer ccsClient = new CcsServer();
		try {
			// Setup CCS client 
			ccsClient.connect(userName, password);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
}
