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
import java.lang.ref.PhantomReference;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
//import de.ub0r.android.websms.connector.common.BasicConnector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * AsyncTask to manage IO to cherry-sms.com API.
 * 
 * @author flx
 */
public final class Connector extends de.ub0r.android.websms.connector.common.Connector {
	/** Tag for output. */
	private static final String TAG = "my-cool-sms.com";
	///** {@link SubConnectorSpec} ID: with sender. */
	//private static final String ID_W_SENDER = "w_sender";
	///** {@link SubConnectorSpec} ID: without sender. */
	//private static final String ID_WO_SENDER = "wo_sender";
	
	private static final String API_URL_BASE = "https://www.my-cool-sms.com/api";
	/**  Send URL. */
	private static final String API_SEND_URL = API_URL_BASE + "/send";
	/** Balance URL */
	private static final String API_BALANCE_URL = API_URL_BASE + "/quota";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConnectorSpec initSpec(final Context context) {
		final String name = context
				.getString(R.string.connector_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(// .
				context.getString(R.string.connector_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector("my-cool-sms.com", c.getName(),0);
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
	 * Check return code from cherry-sms.com.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param ret
	 *            return code
	 * @return true if no error code
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private static boolean checkReturnCode(final Context context, final int ret)
			throws WebSMSException {
//		Log.d(TAG, "ret=" + ret);
//		switch (ret) {
//		case 100:
//			return true;
//		case 10:
//			throw new WebSMSException(context, R.string.error_cherry_10);
//		case 20:
//			throw new WebSMSException(context, R.string.error_cherry_20);
//		case 30:
//			throw new WebSMSException(context, R.string.error_cherry_30);
//		case 31:
//			throw new WebSMSException(context, R.string.error_cherry_31);
//		case 40:
//			throw new WebSMSException(context, R.string.error_cherry_40);
//		case 50:
//			throw new WebSMSException(context, R.string.error_cherry_50);
//		case 60:
//			throw new WebSMSException(context, R.string.error_cherry_60);
//		case 70:
//			throw new WebSMSException(context, R.string.error_cherry_70);
//		case 71:
//			throw new WebSMSException(context, R.string.error_cherry_71);
//		case 80:
//			throw new WebSMSException(context, R.string.error_cherry_80);
//		case 90:
//			throw new WebSMSException(context, R.string.error_cherry_90);
//		default:
//			throw new WebSMSException(context, R.string.error, " code: " + ret);
//		}
		return true;
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
	private void sendData(final Context context, final ConnectorCommand command)
			throws WebSMSException {
		// do IO
		try { // get Connection
			final StringBuilder url = new StringBuilder(API_URL_BASE);
			final ConnectorSpec cs = this.getSpec(context);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			url.append("?user=");
			url.append(Utils.international2oldformat(Utils.getSender(context,
					command.getDefSender())));
			url.append("&password=");
			url.append(Utils.md5(p.getString(Preferences.PREFS_PASSWORD, "")));
			final String text = command.getText();
			if (text != null && text.length() > 0) {
				boolean sendWithSender = false;
				final String sub = command.getSelectedSubConnector();
				//if (sub != null && sub.equals(ID_W_SENDER)) {
					//sendWithSender = true;
				//}
				Log.d(TAG, "send with sender = " + sendWithSender);
				if (sendWithSender) {
					url.append("&from=1");
				}
				url.append("&message=");
				url.append(URLEncoder.encode(text, "ISO-8859-15"));
				url.append("&to=");
				url.append(Utils.joinRecipientsNumbers(Utils
						.national2international(command.getDefPrefix(), command
								.getRecipients()), ";", true));
			} else {
				url.append("&check=guthaben");
			}
			Log.d(TAG, "--HTTP GET--");
			Log.d(TAG, url.toString());
			Log.d(TAG, "--HTTP GET--");
			// send data
			HttpResponse response = Utils.getHttpClient(url.toString(), null,
					null, null, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(context, R.string.error_http, ""
						+ resp);
			}
			String htmlText = Utils.stream2str(
					response.getEntity().getContent()).trim();
			if (htmlText == null || htmlText.length() == 0) {
				throw new WebSMSException(context, R.string.error_service);
			}
			Log.d(TAG, "--HTTP RESPONSE--");
			Log.d(TAG, htmlText);
			Log.d(TAG, "--HTTP RESPONSE--");
			String[] lines = htmlText.split("\n");
			htmlText = null;
			int l = lines.length;
			if (text != null && text.length() > 0) {
				try {
					final int ret = Integer.parseInt(lines[0].trim());
					checkReturnCode(context, ret);
					if (l > 1) {
						cs.setBalance(lines[l - 1].trim());
					}
				} catch (NumberFormatException e) {
					Log.e(TAG, "could not parse ret", e);
					throw new WebSMSException(e.getMessage());
				}
			} else {
				cs.setBalance(lines[l - 1].trim());
			}
		} catch (Exception e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.getMessage());
		}
	}

	
	@Override
	protected void doSend(Context context, Intent intent)
			throws WebSMSException {
		final ConnectorContext ctx = ConnectorContext.create(context, intent);

		String url = getSendURL(ctx);
		StringBuilder sb = new StringBuilder();
		String text = ctx.getCommand().getText();
		sb.append(url);
		String number = ctx.getCommand().getRecipients()[0];
		try{
		sb.append("&number=").append(number).//
			append("message=").append(URLEncoder.encode(text, "utf-8"));//
		}catch(Exception ex){
			throw new WebSMSException(ex);
		}
	}
	
	
	@Override
	protected void doUpdate(Context context, Intent intent)
			throws WebSMSException {
		final ConnectorContext ctx = ConnectorContext.create(context, intent);
		String url = getUpdateURL(ctx);
		HttpResponse response = null;
		try{
			response =  ctx.getClient().execute(new HttpGet(url));
			String content = Utils.stream2str(response.getEntity()
				.getContent());
						if(content.startsWith("ERR: ")){
				throw new WebSMSException(content);//TODO i18n
			}else{
				if(content.length() > 0 && content.charAt(content.length() -1) == '\n'){
					content = content.substring(0,content.length()-1);
				}
				this.getSpec(ctx.getContext())
				.setBalance(content);
			}
		}catch(Exception ex){
			throw new WebSMSException(ex);
		}
	}

	private static String getSendURL(final ConnectorContext ctx){
		return getBaseLoginUrl(API_SEND_URL, ctx);
	}
	
	private static String getUpdateURL(final ConnectorContext ctx) {
		return getBaseLoginUrl(API_BALANCE_URL, ctx);
	}
	
	/**
	 * @param url
	 * @param ctx
	 * return url
	 */
	private static  String getBaseLoginUrl(final String url, ConnectorContext ctx){
		StringBuilder sb = new StringBuilder();
		 sb.append(url).//
		 append("?username=").//
		 append(ctx.getPreferences().getString(Preferences.PREFS_USERNAME, "")).//
		 append("&password=").//
		 append(ctx.getPreferences().getString(Preferences.PREFS_PASSWORD,""));
		 return sb.toString(); 
	
	}
	
}
