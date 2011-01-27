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

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Preferences.
 * 
 * @author lado
 */
public final class Preferences extends PreferenceActivity {
    /** Preference key: enabled. */
    static final String PREFS_ENABLED = "enable_mycoolsms";

    /** Preference's name: user's password. */
    static final String PREFS_USERNAME = "username_mycoolsms";

    /** Preference's name: user's password. */
    static final String PREFS_PASSWORD = "password_mycoolsms";

    static final String SHOW_SMS_AMOUNT = "show_sms_amount";

    static final String ENABLE_CHAIN_SMS = //
    "enable_chain_sms";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.connector_mycoolsms_prefs);
    }
}
