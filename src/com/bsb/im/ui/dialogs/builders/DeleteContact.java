package com.bsb.im.ui.dialogs.builders;

import com.bsb.im.service.Contact;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.RemoteException;
import android.util.Log;

import com.bsb.im.R;
import com.bsb.im.service.aidl.IRoster;


/**
 * Use this builder to build a dialog which allows you to delete a contact from a specific roster.
 */
public class DeleteContact extends AlertDialog.Builder {

    private static final String TAG = "Dialogs.Builders > DeleteContact";

    private IRoster mRoster;
    private Contact mContact;

    /**
     * Constructor.
     * @param context context activity.
     * @param roster the roster which has the contact you want to delete.
     * @param contact the contact to delete.
     */
    public DeleteContact(final Context context, final IRoster roster, final Contact contact) {
	super(context);

	mContact = contact;
	mRoster = roster;

	setMessage(R.string.userinfo_sure2delete);
	DialogClickListener dl = new DialogClickListener();
	setPositiveButton(R.string.userinfo_yes, dl);
	setNegativeButton(R.string.userinfo_no, dl);
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
		    mRoster.deleteContact(mContact);
		} catch (RemoteException e) {
		    Log.e(TAG, e.getMessage());
		}
	    }
	}
    }
}
