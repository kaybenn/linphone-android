package org.linphone;
/*
GCMIntentService.java
Copyright (C) 2012  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import org.linphone.core.LinphoneCoreException;
import org.linphone.core.Log;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gcm.GCMBaseIntentService;

/**
 * @author Sylvain Berfini
 */
// Warning ! Do not rename the service !
public class GCMIntentService extends GCMBaseIntentService {

	public GCMIntentService() {
		
	}
	
	@Override
	protected void onError(Context context, String errorId) {
		Log.e("Error while registering push notification : " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Log.d("Registered push notification : " + regId);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(context.getString(R.string.push_reg_id_key), regId);
		editor.commit();
		
		if (LinphoneManager.isInstanciated()) {
			try {
				LinphoneManager.getInstance().initAccounts();
			} catch (LinphoneCoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.w("Unregistered push notification : " + regId);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(context.getString(R.string.push_reg_id_key), null);
		editor.commit();
	}
}
