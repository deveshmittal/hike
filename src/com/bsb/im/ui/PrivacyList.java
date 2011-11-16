package com.bsb.im.ui;

import java.util.ArrayList;
import java.util.List;

import com.bsb.im.service.PrivacyListItem;
import com.bsb.im.ui.dialogs.builders.CreatePrivacyList;
import com.bsb.im.ui.dialogs.builders.DeletePrivacyList;
import com.bsb.im.utils.BeemBroadcastReceiver;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.bsb.im.R;
import com.bsb.im.service.aidl.IPrivacyListListener;
import com.bsb.im.service.aidl.IPrivacyListManager;
import com.bsb.im.service.aidl.IXmppFacade;

/**
 * This class represents an activity which allows the user to manage his privacy lists.
 */
public class PrivacyList extends ListActivity {

    private static final String TAG = "PrivacyList";
    private static final Intent SERVICE_INTENT = new Intent();
    static {
	SERVICE_INTENT.setComponent(new ComponentName("com.bsb.im", "com.bsb.im.BeemService"));
    }

    private static final int DIALOG_CREATE = 0;
    private static final int DIALOG_UPDATE_BUDDIES = 1;
    private static final int DIALOG_UPDATE_GROUPS = 2;
    private static final int DIALOG_DELETE = 3;

    private static final String SAVED_INSTANCE_KEY_PRIVACY_LISTS = "PRIVACY_LISTS";

    private Handler mHandler = new Handler();

    private ArrayAdapter<String> mAdapter;
    private final List<String> mPrivacyListNames = new ArrayList<String>();
    private String mCurrPrivacyListName;

    private final ServiceConnection mConn = new BeemServiceConnection();
    private final BeemBroadcastReceiver mBroadcastReceiver = new BeemBroadcastReceiver();

    private IPrivacyListManager mPrivacyListManager;
    private IPrivacyListListener mPrivacyListListener;

    /**
     * Constructor.
     */
    public PrivacyList() {
	super();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
	Log.d(TAG, "BEGIN onSaveInstanceState.");
	savedInstanceState.putStringArrayList(SAVED_INSTANCE_KEY_PRIVACY_LISTS, (ArrayList<String>) mPrivacyListNames);
	Log.d(TAG, "END onSaveInstanceState.");
	super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.d(TAG, "BEGIN onCreate.");

	setContentView(R.layout.privacy_list);
	registerForContextMenu(this.getListView());

	mHandler = new Handler();

	if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
	    mPrivacyListNames.addAll(savedInstanceState.getStringArrayList(SAVED_INSTANCE_KEY_PRIVACY_LISTS));
	}

	mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mPrivacyListNames);
	setListAdapter(mAdapter);

	bindService(SERVICE_INTENT, mConn, BIND_AUTO_CREATE);

	mPrivacyListListener = new PrivacyListListener();
	this.registerReceiver(mBroadcastReceiver, new IntentFilter(BeemBroadcastReceiver.BEEM_CONNECTION_CLOSED));

	Log.d(TAG, "END onCreate.");
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void onDestroy() {
	super.onDestroy();

	Log.v(TAG, "BEGIN onDestroy.");

	if (mPrivacyListManager != null) {
	    try {
		mPrivacyListManager.removePrivacyListListener(mPrivacyListListener);
	    } catch (RemoteException e) {
		Log.e(TAG, e.getMessage());
	    }
	}

	this.unregisterReceiver(mBroadcastReceiver);
	unbindService(mConn);

	Log.v(TAG, "END onDestroy.");
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void onStart() {
	super.onStart();

	Log.v(TAG, "BEGIN onStart.");
	Log.v(TAG, "END onStart.");
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void onStop() {
	super.onStop();

	Log.v(TAG, "BEGIN onStop.");
	Log.v(TAG, "END onStop.");
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected Dialog onCreateDialog(int id) {
	Dialog dialog;
	switch (id) {
	    case DIALOG_CREATE:
		dialog = new CreatePrivacyList(PrivacyList.this, mPrivacyListManager).create();
		dialog.setOnDismissListener(new OnDismissListener() {

		    @Override
		    public void onDismiss(DialogInterface dialog) {
			PrivacyList.this.removeDialog(DIALOG_CREATE);
		    }
		});
		break;
	    case DIALOG_DELETE:
		dialog = new DeletePrivacyList(PrivacyList.this, mPrivacyListManager, mCurrPrivacyListName).create();
		dialog.setOnDismissListener(new OnDismissListener() {

		    @Override
		    public void onDismiss(DialogInterface dialog) {
			PrivacyList.this.removeDialog(DIALOG_DELETE);
		    }

		});
		break;
	    default:
		dialog = null;
	}
	return dialog;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
	super.onCreateOptionsMenu(menu);

	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.privacy_list, menu);
	return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
	super.onCreateContextMenu(menu, v, menuInfo);
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.privacy_list_context, menu);
	mCurrPrivacyListName = mPrivacyListNames.get(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
	menu.setHeaderTitle(mCurrPrivacyListName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	    case R.id.privacy_list_context_menu_buddies_item:
		return true;
	    case R.id.privacy_list_context_menu_groups_item:
		return true;
	    case R.id.privacy_list_context_menu_delete_item:
		showDialog(DIALOG_DELETE);
		return true;
	    default:
		return super.onContextItemSelected(item);
	}
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public final boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	    case R.id.privacy_list_menu_create:
		showDialog(DIALOG_CREATE);
		return true;
	    default:
		return false;
	}
    }

    /**
     * Service connection.
     */
    private final class BeemServiceConnection implements ServiceConnection {

	private IXmppFacade mXmppFacade;

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    Log.v(TAG, "BEGIN onServiceConnected.");
	    mXmppFacade = IXmppFacade.Stub.asInterface(service);
	    try {
		mPrivacyListManager = mXmppFacade.getPrivacyListManager();
		mPrivacyListManager.addPrivacyListListener(mPrivacyListListener);
		mPrivacyListNames.clear();
		mPrivacyListNames.addAll(mPrivacyListManager.getPrivacyLists());
		mAdapter.notifyDataSetChanged();
	    } catch (RemoteException e) {
		Log.e(TAG, e.getMessage());
	    }
	    Log.v(TAG, "END onServiceConnected.");
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
	    Log.v(TAG, "BEGIN onServiceDisconnected.");
	    mXmppFacade = null;
	    try {
		mPrivacyListManager.removePrivacyListListener(mPrivacyListListener);
	    } catch (RemoteException e) {
		Log.e(TAG, e.getMessage());
	    }
	    Log.v(TAG, "END onServiceDisconnected.");
	}
    }

    /**
     * Listener.
     */
    private class PrivacyListListener extends IPrivacyListListener.Stub {

	@Override
	public void setPrivacyList(String listName, List<PrivacyListItem> listItem) throws RemoteException {
	    Log.d(TAG, "BEGIN PrivacyListListener >> setPrivacyList.");
	    Log.d(TAG, "> " + listName + " has been setted.");
	    Log.d(TAG, "END PrivacyListListener >> setPrivacyList.");
	}

	@Override
	public void updatedPrivacyList(final String listName) throws RemoteException {
	    Log.d(TAG, "BEGIN PrivacyListListener >> updatedPrivacyList.");
	    mHandler.post(new Runnable() {
		@Override
		public void run() {
		    try {
			mPrivacyListNames.clear();
			// Not that much lists and require some server queries to know if the list has been
			// updated/deleted or set to default/active by this activity or another IM client.
			mPrivacyListNames.addAll(mPrivacyListManager.getPrivacyLists());
		    } catch (RemoteException e) {
			Log.e(TAG, e.getMessage());
		    }
		    mAdapter.notifyDataSetChanged();
		}
	    });
	    Log.d(TAG, "END PrivacyListListener >> updatedPrivacyList.");
	}
    }
}
