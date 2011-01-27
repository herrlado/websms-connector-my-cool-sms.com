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

import de.ub0r.android.lib.apis.TelephonyWrapper;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;

/**
 * AsyncTask to manage IO to cherry-sms.com API.
 * 
 * @author lado
 */
public final class Connector extends de.ub0r.android.websms.connector.common.Connector {
    /** A table of hex digits */
    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /** Tag for output. */
    private static final String TAG = "my-cool-sms.com";

    private static final String API_URL_BASE = "https://www.my-cool-sms.com/api";

    /** Send URL. */
    private static final String API_SEND_URL = API_URL_BASE + "/send";

    /** Balance URL */
    private static final String API_BALANCE_URL = API_URL_BASE + "/quota";

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
        c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE | ConnectorSpec.CAPABILITIES_SEND
                | ConnectorSpec.CAPABILITIES_PREFS);
        c.addSubConnector("my-cool-sms.com", c.getName(), 0);
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectorSpec updateSpec(final Context context, final ConnectorSpec connectorSpec) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
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
     * @param context {@link Context}
     * @param command {@link ConnectorCommand}
     * @throws WebSMSException WebSMSException
     */
    private String sendData(ConnectorContext ctx, String url) throws WebSMSException {
        // do IO
        try { // get Connection
            Log.d(TAG, "--HTTP GET--");
            Log.d(TAG, url.toString());
            Log.d(TAG, "--HTTP GET--");
            // send data
            HttpResponse response = Utils.getHttpClient(url.toString(), null, null, API_USER_AGENT,
                    null);
            int resp = response.getStatusLine().getStatusCode();
            if (resp != HttpURLConnection.HTTP_OK) {
                throw new WebSMSException(ctx.context, R.string.error_http, "" + resp);
            }
            String htmlText = Utils.stream2str(response.getEntity().getContent()).trim();
            if (htmlText == null || htmlText.length() == 0) {
                throw new WebSMSException(ctx.context, R.string.error_service);
            }
            Log.d(TAG, "--HTTP RESPONSE--");
            Log.d(TAG, htmlText);
            Log.d(TAG, "--HTTP RESPONSE--");
            return htmlText;
        } catch (Exception e) {
            Log.e(TAG, null, e);
            throw new WebSMSException(e.getMessage());
        }
    }

    protected void doSendImpl(ConnectorContext ctx) throws WebSMSException {

        boolean sms_splitt_allowed = ctx.getPreferences().getBoolean(Preferences.ENABLE_CHAIN_SMS,
                false);
        // int smscount =
        // int lastsms =;
        int limit = -1;
        // throw new
        // WebSMSException(ctx.getContext().getString(R.string.text_too_long));
        // }

        String text = ctx.getCommand().getText();

        int[] result = TelephonyWrapper.getInstance().calculateLength(text, false);
        String unicode = "0";
        if (result[3] > 1) {
            if (text.length() > 70) {
                if (sms_splitt_allowed == true) {
                    limit = 70;
                } else {
                    throw new WebSMSException(ctx.context, R.string.error_long_for_ucs2);
                }

            }
            unicode = "1";
        } else if (text.length() > 160) {
            if (sms_splitt_allowed) {
                limit = 160;
            } else {
                throw new WebSMSException(ctx.context, R.string.error_long_for_gsm);
            }
        }

        String number = ctx.getCommand().getRecipients()[0];

        if (limit == -1) {
            text = convertUnicodeToEncoded(text, unicode);
            sendImpl(ctx, number, text, unicode);
            return;
        }

        int smscount = text.length() / limit;
        int lastsms = text.length() % limit;

        int end = 0;
        for (int i = 0; i < smscount; ++i) {
            int start = i * limit;
            end = start + limit;
            sendImpl(ctx, number, convertUnicodeToEncoded(text.substring(start, end), unicode),
                    unicode);
        }
        if (lastsms > 0) {
            sendImpl(ctx, number, convertUnicodeToEncoded(text.substring(end), unicode), unicode);
        }

    }

    private void sendImpl(ConnectorContext ctx, String number, String text, String unicode) {
        StringBuilder sb = new StringBuilder();
        String url = getSendURL(ctx);
        sb.append(url);
        try {
            sb.append("&number=")
                    .append(URLEncoder.encode(Utils.national2international(
                            ctx.command.getDefPrefix(), Utils.getRecipientsNumber(number))))
                    .//
                    append("&message=").append(URLEncoder.encode(text, "utf-8"))
                    .//
                    append("&senderid=")
                    .append(URLEncoder.encode(
                            Utils.getSender(ctx.context, ctx.command.getDefSender()), "utf-8")).//
                    append("&unicode=").append(unicode);

            ;//
        } catch (Exception ex) {
            throw new WebSMSException(ex);
        }
        String content = sendData(ctx, sb.toString());
        validateSend(ctx, content);
    }

    @Override
    protected void doSend(Context context, Intent intent) throws WebSMSException {
        final ConnectorContext ctx = ConnectorContext.create(context, intent);
        doSendImpl(ctx);
        try {
            doUpdateImpl(ctx);
        } catch (WebSMSException ex) {
            Log.w(TAG, "Can not retrieve balance after send: " + ex.getMessage());
        }
    }

    private static void validate(ConnectorContext ctx, String prefix, String content)
            throws WebSMSException {
        if (!content.startsWith("ERR:")) {
            return;
        }
        content = content.substring(4).trim();
        Field f = null;
        int id = -1;
        try {
            f = R.string.class.getField(prefix + content);
            id = f.getInt(null);
        } catch (Exception ex) {
            throw new WebSMSException("can not get errorcode: " + content);
        }
        throw new WebSMSException(ctx.getContext(), id, content);
    }

    private static void validateSend(ConnectorContext ctx, String content) throws WebSMSException {
        validate(ctx, "http_error_send_", content);
    }

    protected void doUpdateImpl(final ConnectorContext ctx) throws WebSMSException {
        String url = getUpdateURL(ctx);
        String content = sendData(ctx, url).trim();
        validateBalance(ctx, content);
        try {
            DecimalFormat twoDForm = new DecimalFormat("#.##");
            content = twoDForm.format(Double.parseDouble(content));
        } catch (IllegalArgumentException ex) {
            throw new WebSMSException("Can not parse a string as amound: " + content + "; "
                    + ex.getMessage());
        }
        this.getSpec(ctx.getContext()).setBalance(content + "\u20AC");
    }

    private static void validateBalance(ConnectorContext ctx, String content)
            throws WebSMSException {
        validate(ctx, "http_error_balance_", content);
    }

    @Override
    protected void doUpdate(Context context, Intent intent) throws WebSMSException {
        doUpdateImpl(ConnectorContext.create(context, intent));
    }

    /**
     * @param ctx
     * @return
     */
    private static String getSendURL(final ConnectorContext ctx) {
        return getBaseLoginUrl(API_SEND_URL, ctx);
    }

    /**
     * @param ctx
     * @return
     */
    private static String getUpdateURL(final ConnectorContext ctx) {
        return getBaseLoginUrl(API_BALANCE_URL, ctx);
    }

    /**
     * BaseUrl with credentials specified
     * 
     * @param url
     * @param ctx
     * @return url
     * @throws UnsupportedEncodingException
     */
    private static String getBaseLoginUrl(final String url, ConnectorContext ctx) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(url).//
                    append("?username=")
                    .//
                    append(URLEncoder
                            .encode(ctx.getPreferences().getString(Preferences.PREFS_USERNAME, ""),
                                    "utf-8")).//
                    append("&password=").//
                    append(URLEncoder
                            .encode(ctx.getPreferences().getString(Preferences.PREFS_PASSWORD, ""),
                                    "utf-8"));
        } catch (UnsupportedEncodingException use) {
            throw new RuntimeException(ctx.getContext().getString(R.string.contact_dev_on_error)
                    + "; " + use.getClass().getSimpleName() + ": " + use.getMessage(), use);
        }
        return sb.toString();
    }

    /**
     * makce ucs2 hex without leading \\u
     * 
     * @param str
     * @return
     */
    public static String convertUnicodeToEncoded(String str, String unicode) {
        if (unicode.equals("0")) {
            return str;
        }
        int len = str.length();
        StringBuffer outBuffer = new StringBuffer(len * 2);

        for (int x = 0; x < len; x++) {
            char aChar = str.charAt(x);
            // if ((aChar < 0x0020) || (aChar > 0x007e)) {
            // outBuffer.append('\\');
            // outBuffer.append('u');
            outBuffer.append(toHex((aChar >> 12) & 0xF));
            outBuffer.append(toHex((aChar >> 8) & 0xF));
            outBuffer.append(toHex((aChar >> 4) & 0xF));
            outBuffer.append(toHex(aChar & 0xF));
            // } else {
            // outBuffer.append(aChar);
            // }
        }
        return outBuffer.toString();
    }

    /**
     * Converts a nibble to a hex character
     * 
     * @param nibble the nibble to convert.
     * @return a converted character
     */
    private static char toHex(int nibble) {
        char hexChar = HEX_DIGITS[(nibble & 0xF)];
        return hexChar;
    }

}
