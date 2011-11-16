package com.bsb.im.ui.dialogs.builders;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.RemoteException;
import android.util.Log;

import com.bsb.im.R;
import com.bsb.im.service.aidl.IPrivacyListManager;


/**
 * Use this builder to build a dialog which allows you to delete a privacy list.
 */
public class DeletePrivacyList extends AlertDialog.Builder {

    private static final String TAG = "Dialogs.Builders > DeletePrivacyList";

    private final IPrivacyListManager mPrivacyListManager;
    private final String mPrivacyListName;

    /**
     * Constructor.
     * @param context context activity.
     * @param privacyListManager the privacy list manager managing the privacy list you want to delete.
     * @param privacyListName the name of the privacy list you want to delete.
     */
    public DeletePrivacyList(final Context context, final IPrivacyListManager privacyListManager,
	final String privacyListName) {
	super(context);

	mPrivacyListManager = privacyListManager;
	mPrivacyListName = privacyListName;

	setMessage(context.getString(R.string.privacy_list_delete_dialog_msg, privacyListName));
	DialogClickListener dl = new DialogClickListener();
	setPositiveButton(R.string.privacy_list_delete_dialog_yes, dl);
	setNegativeButton(R.string.privacy_list_delete_dialog_no, dl);
    }

    /**
     * Event click listener.
     */
    private class DialogClickListener implements DialogInterface.OnClickListener {

	/**
	 * Constructor.
	 */
	public DialogClickListener() {
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
	    if (which == DialogInterface.BUTTON_POSITIVE) {
		try {
		    mPrivacyListManager.removePrivacyList(mPrivacyListName);
		} catch (RemoteException e) {
		    Log.e(TAG, e.getMessage());
		}
	    }
	}
    }
}
