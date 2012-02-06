/*
 * Copyright (C) 2010 Lado Kumsiashvili
 * 
 * This file is part of websms-connector-my-cool-sms.com
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */

package org.herrlado.websms.connector.mycoolsms;

import java.net.HttpURLConnection;
import java.text.DecimalFormat;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * AsyncTask to manage IO to cherry-sms.com API.
 * 
 * @author lado
 */
public final class Connector extends
		de.ub0r.android.websms.connector.common.Connector {

	private static String PACKAGE_NAME = Connector.class.getPackage().getName();

	/** A table of hex digits */
	// private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
	// '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/** Tag for output. */
	private static final String TAG = "my-cool-sms.com";

	private static final String API_URL_BASE = "https://www.my-cool-sms.com/api-socket.php";

	private static final String API_USER_AGENT = "WebSMS";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(// .
		context.getString(R.string.connector_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector("my-cool-sms.com", c.getName(),
				SubConnectorSpec.FEATURE_CUSTOMSENDER);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			if (p.getString(Preferences.PREFS_PASSWORD, "").length() > 0) {
				connectorSpec.setReady();
			} else {
				connectorSpec.setStatus(ConnectorSpec.STATUS_ENABLED);
			}
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * Send data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private JSONObject sendData(ConnectorContext ctx, JSONObject obj)
			throws WebSMSException {
		// do IO

		try { // get Connection
				// send data
			HttpResponse response = Utils.getHttpClient(
					API_URL_BASE.toString(), null, obj, API_USER_AGENT, null,
					"utf-8", false);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(ctx.context, R.string.error_http, ""
						+ resp);
			}
			String jsonText = Utils.stream2str(
					response.getEntity().getContent()).trim();
			if (jsonText == null || jsonText.length() == 0) {
				throw new WebSMSException(ctx.context, R.string.error_service);
			}
			Log.d(TAG, "--HTTP RESPONSE--");
			Log.d(TAG, jsonText);
			Log.d(TAG, "--HTTP RESPONSE--");
			JSONObject r = new JSONObject(jsonText);
			return r;
		} catch (Exception e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.getMessage());
		}
	}

	protected void doSendImpl(ConnectorContext ctx, String number) {
		JSONObject req = getSend(ctx);

		try {

			String text = ctx.getCommand().getText();
			req.put("message", text);
			req.put("number", number);
			String sender = ctx.getCommand().getCustomSender();
			if (sender == null) {
				sender = Utils.getSender(ctx.getContext(), ctx.getCommand()
						.getDefSender());
			}
			req.put("senderid", sender);

			JSONObject json = sendData(ctx, req);

			ensureSuccess(ctx, json);

			updateBalance(ctx, json);
		} catch (Exception ex) {
			if (ex instanceof WebSMSException) {
				throw (WebSMSException) ex;
			}
			throw new WebSMSException(ex);
		}
	}

	protected void doSendImpl(ConnectorContext ctx) throws WebSMSException {
		for (String r : ctx.getCommand().getRecipients()) {
			String number = Utils.getRecipientsNumber(r);
			doSendImpl(ctx, number);
		}
	}

	// private void sendImpl(ConnectorContext ctx, String number, String text,
	// String unicode) {
	// StringBuilder sb = new StringBuilder();
	// sb.append(url);
	// try {
	// sb.append("&number=")
	// .append(URLEncoder.encode(Utils.national2international(
	// ctx.command.getDefPrefix(),
	// Utils.getRecipientsNumber(number))))
	// .//
	// append("&message=")
	// .append(URLEncoder.encode(text, "utf-8"))
	// .//
	// append("&senderid=")
	// .append(URLEncoder.encode(
	// Utils.getSender(ctx.context,
	// ctx.command.getDefSender()), "utf-8")).//
	// append("&unicode=").append(unicode);
	//
	// ;//
	// } catch (Exception ex) {
	// throw new WebSMSException(ex);
	// }
	// // String content = sendData(ctx, null);
	// // ensureSuccess(ctx, null);
	// // validateSend(ctx, content);
	// }

	@Override
	protected void doSend(Context context, Intent intent)
			throws WebSMSException {
		final ConnectorContext ctx = ConnectorContext.create(context, intent);
		try {
			doSendImpl(ctx);
		} catch (Exception ex) {
			if (ex instanceof WebSMSException) {
				throw (WebSMSException) ex;
			}
			throw new WebSMSException(ex);
		}
	}

	private static void ensureSuccess(ConnectorContext ctx, JSONObject resp)
			throws Exception {
		if (resp.getBoolean("success") == false) {

			String msg = null;
			try {
				String code = resp.getString("errorcode");
				msg = getStringResourceByName(ctx.getContext(), "errorcode_"
						+ code);
			} catch (Exception ex) {
				android.util.Log.w(TAG, ex);
			}
			if (msg == null) {
				msg = resp.getString("description");
			}
			throw new WebSMSException(msg);
		}
	}

	private static String getStringResourceByName(Context ctx, String aString) {

		int resId = ctx.getResources().getIdentifier(aString, "string",
				PACKAGE_NAME);
		return ctx.getString(resId);
	}

	// private static void validate(ConnectorContext ctx, String prefix,
	// String content) throws WebSMSException {
	// if (!content.startsWith("ERR:")) {
	// return;
	// }
	// content = content.substring(4).trim();
	// Field f = null;
	// int id = -1;
	// try {
	// f = R.string.class.getField(prefix + content);
	// id = f.getInt(null);
	// } catch (Exception ex) {
	// throw new WebSMSException("can not get errorcode: " + content);
	// }
	// throw new WebSMSException(ctx.getContext(), id, content);
	// }

	// private static void validateSend(ConnectorContext ctx, String content)
	// throws WebSMSException {
	// validate(ctx, "http_error_send_", content);
	// }

	protected void updateBalance(final ConnectorContext ctx,
			final JSONObject json) throws Exception {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		String balance = twoDForm.format(json.getDouble("balance"));
		this.getSpec(ctx.getContext()).setBalance(balance + "\u20AC");
	}

	protected void doUpdateImpl(final ConnectorContext ctx)
			throws WebSMSException {
		JSONObject req = getUpdate(ctx);
		JSONObject json = sendData(ctx, req);
		try {
			ensureSuccess(ctx, json);
			updateBalance(ctx, json);
		} catch (Exception ex) {
			if (ex instanceof WebSMSException) {
				throw (WebSMSException) ex;
			}
			throw new WebSMSException(ex);
		}
	}

	@Override
	protected void doUpdate(Context context, Intent intent)
			throws WebSMSException {
		try {
			doUpdateImpl(ConnectorContext.create(context, intent));
		} catch (Exception ex) {
			if (ex instanceof WebSMSException) {
				throw (WebSMSException) ex;
			}
			throw new WebSMSException(ex);
		}
	}

	// /**
	// * @param ctx
	// * @return
	// */
	// private static String getSendURL(final ConnectorContext ctx) {
	// return getBaseLoginUrl(API_SEND_URL, ctx);
	// }

	// /**
	// * @param ctx
	// * @return
	// */
	// private static String getUpdateURL(final ConnectorContext ctx) {
	// return getBaseLoginUrl(API_BALANCE_URL, ctx);
	// }

	/**
	 * @param ctx
	 * @return
	 */
	private static JSONObject getUpdate(final ConnectorContext ctx) {
		JSONObject o = getUpdateBase(ctx);
		try {
			o.put("function", "getBalance");
			return o;
		} catch (Exception ex) {
			throw new WebSMSException(ex);
		}
	}

	/**
	 * @param ctx
	 * @return
	 */
	private static JSONObject getSend(final ConnectorContext ctx) {
		JSONObject o = getUpdateBase(ctx);
		try {
			o.put("function", "sendSms");
			return o;
		} catch (Exception ex) {
			throw new WebSMSException(ex);
		}
	}

	/**
	 * @param ctx
	 * @return
	 */
	private static JSONObject getUpdateBase(final ConnectorContext ctx) {
		JSONObject o = new JSONObject();
		try {
			o.put("username",
					ctx.getPreferences().getString(Preferences.PREFS_USERNAME,
							"!NOT_SET!"));
			o.put("password",
					ctx.getPreferences().getString(Preferences.PREFS_PASSWORD,
							"!NOT_SET!"));
			return o;
		} catch (Exception ex) {
			throw new WebSMSException(ex);
		}
	}

	// /**
	// * BaseUrl with credentials specified
	// *
	// * @param url
	// * @param ctx
	// * @return url
	// * @throws UnsupportedEncodingException
	// */
	// private static String getBaseLoginUrl(final String url, ConnectorContext
	// ctx) {
	// StringBuilder sb = new StringBuilder();
	// try {
	// sb.append(url)
	// .//
	// append("?username=")
	// .//
	// append(URLEncoder.encode(
	// ctx.getPreferences().getString(
	// Preferences.PREFS_USERNAME, ""), "utf-8")).//
	// append("&password=").//
	// append(URLEncoder.encode(
	// ctx.getPreferences().getString(
	// Preferences.PREFS_PASSWORD, ""), "utf-8"));
	// } catch (UnsupportedEncodingException use) {
	// throw new RuntimeException(ctx.getContext().getString(
	// R.string.contact_dev_on_error)
	// + "; "
	// + use.getClass().getSimpleName()
	// + ": "
	// + use.getMessage(), use);
	// }
	// return sb.toString();
	// }

	// /**
	// * makce ucs2 hex without leading \\u
	// *
	// * @param str
	// * @return
	// */
	// public static String convertUnicodeToEncoded(String str, String unicode)
	// {
	// if (unicode.equals("0")) {
	// return str;
	// }
	// int len = str.length();
	// StringBuffer outBuffer = new StringBuffer(len * 2);
	//
	// for (int x = 0; x < len; x++) {
	// char aChar = str.charAt(x);
	// // if ((aChar < 0x0020) || (aChar > 0x007e)) {
	// // outBuffer.append('\\');
	// // outBuffer.append('u');
	// outBuffer.append(toHex((aChar >> 12) & 0xF));
	// outBuffer.append(toHex((aChar >> 8) & 0xF));
	// outBuffer.append(toHex((aChar >> 4) & 0xF));
	// outBuffer.append(toHex(aChar & 0xF));
	// // } else {
	// // outBuffer.append(aChar);
	// // }
	// }
	// return outBuffer.toString();
	// }
	//
	// /**
	// * Converts a nibble to a hex character
	// *
	// * @param nibble
	// * the nibble to convert.
	// * @return a converted character
	// */
	// private static char toHex(int nibble) {
	// char hexChar = HEX_DIGITS[(nibble & 0xF)];
	// return hexChar;
	// }

}
