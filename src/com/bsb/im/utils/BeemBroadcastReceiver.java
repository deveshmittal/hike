package com.bsb.im.utils;

import com.bsb.im.BeemService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.widget.Toast;

import com.bsb.im.R;

/**
 * Manage broadcast disconnect intent.
 */
public class BeemBroadcastReceiver extends BroadcastReceiver {

    /** Broadcast intent type. */
    public static final String BEEM_CONNECTION_CLOSED = "ImConnectionClosed";

    /**
     * constructor.
     */
    public BeemBroadcastReceiver() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
	String intentAction = intent.getAction();
	if (intentAction.equals(BEEM_CONNECTION_CLOSED)) {
	    CharSequence message = intent.getCharSequenceExtra("message");
	    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	    if (context instanceof Activity) {
		Activity act = (Activity) context;
		act.finish();
		// The service will be unbinded in the destroy of the activity.
	    }
	} else if (intentAction.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
	    if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
		Toast.makeText(context, context.getString(R.string.BeemBroadcastReceiverDisconnect),
		    Toast.LENGTH_SHORT).show();
		context.stopService(new Intent(context, BeemService.class));
	    }
	}
    }
}
