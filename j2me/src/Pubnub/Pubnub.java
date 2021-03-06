/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Pubnub;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.HttpsConnection;
import json.me.JSONArray;
import json.me.JSONException;
import json.me.JSONObject;

public class Pubnub {
	private String ORIGIN = "pubsub.pubnub.com";
	private String PUBLISH_KEY = "";
	private String SUBSCRIBE_KEY = "";
	private String SECRET_KEY = "";
	private String CIPHER_KEY = "";
	private boolean SSL = false;

	private class Channel_status {
		String channel;
		boolean connected, first;
	}

	private Vector subscriptions;

	/**
	 * PubNub 3.1 with Cipher Key
	 * 
	 * Prepare PubNub State.
	 * 
	 * @param String
	 *            Publish Key.
	 * @param String
	 *            Subscribe Key.
	 * @param String
	 *            Secret Key.
	 * @param String
	 *            Cipher Key.
	 * @param boolean SSL Enabled.
	 */
	public Pubnub(String publish_key, String subscribe_key, String secret_key,
			String cipher_key, boolean ssl_on) {
		this.init(publish_key, subscribe_key, secret_key, cipher_key, ssl_on);
	}

	/**
	 * PubNub 3.0
	 * 
	 * Prepare PubNub Class State.
	 * 
	 * @param String
	 *            Publish Key.
	 * @param String
	 *            Subscribe Key.
	 * @param String
	 *            Secret Key.
	 * @param boolean SSL Enabled.
	 */
	public Pubnub(String publish_key, String subscribe_key, String secret_key,
			boolean ssl_on) {
		this.init(publish_key, subscribe_key, secret_key, "", ssl_on);
	}

	/**
	 * PubNub 2.0 Compatibility
	 * 
	 * Prepare PubNub Class State.
	 * 
	 * @param String
	 *            Publish Key.
	 * @param String
	 *            Subscribe Key.
	 */
	public Pubnub(String publish_key, String subscribe_key) {
		this.init(publish_key, subscribe_key, "", "", false);
	}

	/**
	 * PubNub 3.0 without SSL
	 * 
	 * Prepare PubNub Class State.
	 * 
	 * @param String
	 *            Publish Key.
	 * @param String
	 *            Subscribe Key.
	 * @param String
	 *            Secret Key.
	 */
	public Pubnub(String publish_key, String subscribe_key, String secret_key) {
		this.init(publish_key, subscribe_key, secret_key, "", false);
	}

	/**
	 * Init
	 * 
	 * Prepare PubNub Class State.
	 * 
	 * @param String
	 *            Publish Key.
	 * @param String
	 *            Subscribe Key.
	 * @param String
	 *            Secret Key.
	 * @param String
	 *            Cipher Key.
	 * @param boolean SSL Enabled.
	 */
	public void init(String publish_key, String subscribe_key,
			String secret_key, String cipher_key, boolean ssl_on) {
		this.PUBLISH_KEY = publish_key;
		this.SUBSCRIBE_KEY = subscribe_key;
		this.SECRET_KEY = secret_key;
		this.CIPHER_KEY = cipher_key;
		this.SSL = ssl_on;

		// SSL On?
		if (this.SSL) {
			this.ORIGIN = "https://" + this.ORIGIN;
		} else {
			this.ORIGIN = "http://" + this.ORIGIN;
		}
	}

	/**
	 * Publish
	 * 
	 * Send a message to a channel.
	 * 
	 * @param String
	 *            channel name.
	 * @param JSONObject
	 *            message.
	 * @return JSONArray.
	 */
	public JSONArray publish(String channel, JSONObject message) {
		Hashtable args = new Hashtable(2);
		args.put("channel", channel);
		args.put("message", message);
		return publish(args);
	}

	/**
	 * Publish
	 * 
	 * Send a message to a channel.
	 * 
	 * @param HashMap
	 *            <String, Object> containing channel name, message.
	 * @return JSONArray.
	 */
	public JSONArray publish(Hashtable args) {

		String channel = (String) args.get("channel");
		Object message = args.get("message");

		if (message instanceof JSONObject) {
			JSONObject obj = (JSONObject) message;
			if (this.CIPHER_KEY.length() > 0) {
				// TODO: Encrypt Message
			} else {
				message = obj;
			}
		} else if (message instanceof String) {
			String obj = (String) message;
			if (this.CIPHER_KEY.length() > 0) {
				// TODO: Encrypt Message
			} else {
				message = obj;
			}
			message = "\"" + message + "\"";

		} else if (message instanceof JSONArray) {
			JSONArray obj = (JSONArray) message;

			if (this.CIPHER_KEY.length() > 0) {
				// TODO: Encrypt Message
			} else {
				message = obj;
			}
			System.out.println();
		}

		// Generate String to Sign
		String signature = "0";

		if (this.SECRET_KEY.length() > 0) {
			// TODO: Sign Message
		}

		// Build URL
		Vector url = new Vector();
		url.addElement("publish");
		url.addElement(this.PUBLISH_KEY);
		url.addElement(this.SUBSCRIBE_KEY);
		url.addElement(signature);
		url.addElement(channel);
		url.addElement("0");
		url.addElement(message.toString());

		return _request(url);
	}

	/**
	 * Subscribe
	 * 
	 * Listen for a message on a channel.
	 * 
	 * @param HashMap
	 *            <String, Object> containing channel name, function callback.
	 */
	public void subscribe(final Hashtable args) {
		args.put("timetoken", "0");
		_subscribe(args);

	}

	/**
	 * Subscribe - Private Interface
	 * 
	 * Patch provided by petereddy on GitHub
	 * 
	 * @param HashMap
	 *            <String, Object> containing channel name, function callback,
	 *            timetoken.
	 */
	private void _subscribe(Hashtable args) {

		String channel = (String) args.get("channel");
		String timetoken = (String) args.get("timetoken");
		Callback callback, connect_cb, disconnect_cb, reconnect_cb, error_cb;

		// Validate Arguments
		if (args.get("callback") != null) {
			callback = (Callback) args.get("callback");
		} else {
			System.out.println("Invalid Callback.");
			return;
		}
		if (args.get("connect_cb") != null)
			connect_cb = (Callback) args.get("connect_cb");
		else
			connect_cb = new TempCallback();
		if (args.get("disconnect_cb") != null)
			disconnect_cb = (Callback) args.get("disconnect_cb");
		else
			disconnect_cb = new TempCallback();
		if (args.get("reconnect_cb") != null)
			reconnect_cb = (Callback) args.get("reconnect_cb");
		else
			reconnect_cb = new TempCallback();
		if (args.get("error_cb") != null)
			error_cb = (Callback) args.get("error_cb");
		else
			error_cb = (Callback) args.get("callback");

		if (channel == null || channel.equals("")) {
			error_cb.execute("Invalid Channel.");
			return;
		}

		// Ensure Single Connection
		if (subscriptions != null && subscriptions.size() > 0) {
			boolean channel_exist = false;
			Channel_status it;
			for (int i = 0; i < subscriptions.size(); i++) {
				it = (Channel_status) subscriptions.elementAt(i);
				if (it.channel.equals(channel)) {
					channel_exist = true;
					break;
				}
			}
			if (!channel_exist) {
				Channel_status cs = new Channel_status();
				cs.channel = channel;
				cs.connected = true;
				subscriptions.addElement(cs);
			} else {
				error_cb.execute("Already Connected");
				return;
			}
		} else {
			// New Channel
			Channel_status cs = new Channel_status();
			cs.channel = channel;
			cs.connected = true;
			subscriptions = new Vector();
			subscriptions.addElement(cs);
		}

		while (true) {
			try {
				// Build URL
				Vector url = new Vector();
				url.addElement("subscribe");
				url.addElement(this.SUBSCRIBE_KEY);
				url.addElement(channel);
				url.addElement("0");
				url.addElement(timetoken);

				// Stop Connection?
				boolean is_disconnect = false;
				Channel_status it;
				for (int i = 0; i < subscriptions.size(); i++) {
					it = (Channel_status) subscriptions.elementAt(i);
					if (it.channel.equals(channel)) {
						if (!it.connected) {
							disconnect_cb.execute("Disconnected to channel : "
									+ channel);
							is_disconnect = true;
							break;
						}
					}
				}
				if (is_disconnect)
					return;

				// Wait for Message
				JSONArray response = _request(url);

				// Stop Connection?
				for (int i = 0; i < subscriptions.size(); i++) {
					it = (Channel_status) subscriptions.elementAt(i);
					if (it.channel.equals(channel)) {
						if (!it.connected) {
							disconnect_cb.execute("Disconnected to channel : "
									+ channel);
							is_disconnect = true;
							break;
						}
					}
				}

				if (is_disconnect)
					return;

				// Problem?
				if (response == null || response.optInt(1) == 0) {

					for (int i = 0; i < subscriptions.size(); i++) {
						it = (Channel_status) subscriptions.elementAt(i);
						if (it.channel.equals(channel)) {
							subscriptions.removeElement(it);
							disconnect_cb.execute("Disconnected to channel : "
									+ channel);
						}
					}
					// Ensure Connected (Call Time Function)
					boolean is_reconnected = false;
					while (true) {
						double time_token = this.time();
						if (time_token == 0) {
							// Reconnect Callback
							reconnect_cb.execute("Reconnecting to channel : "
									+ channel);
							Thread.sleep(5000);
						} else {
							this._subscribe(args);
							is_reconnected = true;
							break;
						}
					}
					if (is_reconnected) {
						break;
					}
				} else {

					for (int i = 0; i < subscriptions.size(); i++) {
						it = (Channel_status) subscriptions.elementAt(i);
						if (it.channel.equals(channel)) {
							// Connect Callback
							if (!it.first) {
								it.first = true;
								connect_cb.execute("Connected to channel : "
										+ channel);
								break;
							}
						}
					}
				}

				JSONArray messages = response.optJSONArray(0);

				// Update TimeToken
				if (response.optString(1).length() > 0)
					timetoken = response.optString(1);

				for (int i = 0; messages.length() > i; i++) {
					JSONObject message = messages.optJSONObject(i);
					if (message != null) {

						if (this.CIPHER_KEY.length() > 0) {
							// TODO: Decrypt Message
						}
						if (callback != null)
							callback.execute(message);
					} else {

						JSONArray arr = messages.optJSONArray(i);
						if (arr != null) {
							if (this.CIPHER_KEY.length() > 0) {
								// TODO: Decrypt Message
							}
							if (callback != null)
								callback.execute(arr);
						} else {
							String msgs = messages.getString(0);
							if (this.CIPHER_KEY.length() > 0) {
								// TODO: Decrypt Message
							}
							if (callback != null)
								callback.execute(msgs);
						}
					}
				}
			} catch (Exception e) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
				}
			}
		}
	}

	/**
	 * Time
	 * 
	 * Timestamp from PubNub Cloud.
	 * 
	 * @return double timestamp.
	 */
	public double time() {
		Vector url = new Vector();

		url.addElement("time");
		url.addElement("0");

		JSONArray response = _request(url);
		try {
			return (Double.parseDouble(response.get(0).toString()));
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
		return 0;
	}

	/**
	 * UUID
	 * 
	 * 32 digit UUID generation at client side.
	 * 
	 * @return String uuid.
	 */
	public static String uuid() {
		// TODO: UUID genetaration
//		String str = new String(System.currentTimeMillis() + "");
//		System.out.println("Length::" + str.length());
		return "";
	}

	/**
	 * History
	 * 
	 * Load history from a channel.
	 * 
	 * @param String
	 *            channel name.
	 * @param int limit history count response.
	 * @return JSONArray of history.
	 */
	public JSONArray history(String channel, int limit) {
		Hashtable args = new Hashtable(2);
		args.put("channel", channel);
		args.put("limit", new Integer(limit));
		return history(args);
	}

	/**
	 * History
	 * 
	 * Load history from a channel.
	 * 
	 * @param HashMap
	 *            <String, Object> containing channel name, limit history count
	 *            response.
	 * @return JSONArray of history.
	 */
	public JSONArray history(Hashtable args) {

		String channel = (String) args.get("channel");
		Integer limit = (Integer) args.get("limit");

		Vector url = new Vector();

		url.addElement("history");
		url.addElement(this.SUBSCRIBE_KEY);
		url.addElement(channel);
		url.addElement("0");
		url.addElement(limit.toString());

		JSONArray response = _request(url);

		if (this.CIPHER_KEY.length() > 0) {
			// TODO: Decrpyt Messages
		}
		return response;

	}

	/**
	 * Unsubscribe
	 * 
	 * Unsubscribe/Disconnect to channel.
	 * 
	 * @param HashMap
	 *            <String, Object> containing channel name.
	 */
	public void unsubscribe(Hashtable args) {
		String channel = (String) args.get("channel");
		Channel_status it;
		for (int i = 0; i < subscriptions.size(); i++) {
			it = (Channel_status) subscriptions.elementAt(i);
			if (it.channel.equals(channel) && it.connected) {
				it.connected = false;
				it.first = false;
				break;
			}
		}
	}

	/**
	 * Request URL
	 * 
	 * @param List
	 *            <String> request of url directories.
	 * @return JSONArray from JSON response.
	 */
	private JSONArray _request(Vector url_components) {
		String json = "";
		StringBuffer url = new StringBuffer();
		Enumeration url_iterator = url_components.elements();
		String request_for = (String) url_components.elementAt(0);

		url.append(this.ORIGIN);

		// Generate URL with UTF-8 Encoding
		while (url_iterator.hasMoreElements()) {
			try {
				String url_bit = (String) url_iterator.nextElement();
				url.append("/").append(_encodeURIcomponent(url_bit));
			} catch (Exception e) {
				// e.printStackTrace();
				JSONArray jsono = new JSONArray();
				try {
					jsono.put("Failed UTF-8 Encoding URL.");
				} catch (Exception jsone) {
				}
				return jsono;
			}
		}
		try {
			String _responce = getViaHttpsConnection(url.toString());
			return new JSONArray(_responce);
		} catch (JSONException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (OAuthServiceProviderException ex) {
			ex.printStackTrace();
		}

		return new JSONArray();
	}

	private String _encodeURIcomponent(String s) {
		StringBuffer o = new StringBuffer();
		char[] array = s.toCharArray();
		for (int i = 0; i < array.length; i++) {
			char ch = array[i];
			if (isUnsafe(ch)) {
				o.append('%');
				o.append(toHex(ch / 16));
				o.append(toHex(ch % 16));
			} else
				o.append(ch);
		}
		return o.toString();
	}

	private char toHex(int ch) {
		return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
	}

	private boolean isUnsafe(char ch) {
		return " ~`!@#$%^&*()+=[]\\{}|;':\",./<>?É‚é¡¶".indexOf(ch) >= 0;
	}

	public static final String getViaHttpsConnection(String url)
			throws IOException, OAuthServiceProviderException {
		HttpConnection c = null;
		DataInputStream dis = null;
		OutputStream os = null;
		int rc;

		String respBody = new String(""); // return empty string on bad things
		// TODO -- better way to handle unexpected responses
		try {
			c = (HttpConnection) Connector.open(url, Connector.READ_WRITE,
					false);
			c.setRequestMethod(HttpConnection.GET);
			c.setRequestProperty("V", "3.1");
			c.setRequestProperty("User-Agent", "J2ME");
			rc = c.getResponseCode();
			// Get the length and process the data
			int len = c.getHeaderFieldInt("Content-Length", 0);
			System.out.println("content-length=" + len);
			dis = c.openDataInputStream();

			byte[] data = null;
			ByteArrayOutputStream tmp = new ByteArrayOutputStream();
			int ch;
			while ((ch = dis.read()) != -1) {
				tmp.write(ch);
			}
			data = tmp.toByteArray();
			respBody = new String(data);
			System.out.println("Temp respBody data ::" + respBody);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Not an HTTP URL");
		} finally {
			if (dis != null)
				dis.close();
			if (c != null)
				c.close();
		}
		if (rc != HttpConnection.HTTP_OK) {
			throw new OAuthServiceProviderException(
					"HTTP response code: " + rc, rc, respBody);
		}
		return respBody;
	}

	// Temporary callback, using if optional callback not provided
	private class TempCallback implements Callback {

		public boolean execute(Object message) {
			// DO NOTHING
			return false;
		}
	}

}
