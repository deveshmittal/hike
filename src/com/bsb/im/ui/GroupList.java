package com.bsb.im.ui;

import java.util.ArrayList;
import java.util.List;

import com.bsb.im.BeemService;
import com.bsb.im.service.Contact;
import com.bsb.im.utils.BeemBroadcastReceiver;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.bsb.im.R;
import com.bsb.im.service.aidl.IRoster;
import com.bsb.im.service.aidl.IXmppFacade;

/**
 * That activity permit to manage user groups.
 */
public class GroupList extends ListActivity {

    private static final Intent SERVICE_INTENT = new Intent();

    private final ServiceConnection mServConn = new BeemServiceConnection();
    private final BeemBroadcastReceiver mReceiver =  new BeemBroadcastReceiver();
    private IXmppFacade mXmppFacade;
    private IRoster mRoster;
    private String mJID;
    private ArrayAdapter<String> mGroups;
    private Contact mContact;
    private TextView mText;
    private final List<String> mStrings = new ArrayList<String>();

    static {
	SERVICE_INTENT.setComponent(new ComponentName("com.bsb.im", "com.bsb.im.BeemService"));
    }

    /**
     * Constructor.
     */
    public GroupList() {
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.group_list);
	mContact = getIntent().getParcelableExtra("contact");
	mJID = mContact.getJID();
	final ListView listView = getListView();

	listView.setItemsCanFocus(false);
	listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	listView.setOnItemClickListener(new GroupOnItemClickListener());

	mText = (TextView) findViewById(R.id.GroupListText);
	mText.setOnKeyListener(new GroupListOnKeyListener());
	this.registerReceiver(mReceiver, new IntentFilter(BeemBroadcastReceiver.BEEM_CONNECTION_CLOSED));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
	super.onResume();
	bindService(new Intent(this, BeemService.class), mServConn, BIND_AUTO_CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
	super.onPause();
	unbindService(mServConn);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
	super.onDestroy();
	this.unregisterReceiver(mReceiver);
    }

    /**
     * init activity list adapter.
     */
    private void setAdapter() {
	try {
	    for (String group : mRoster.getGroupsNames()) {
		mStrings.add(group);
	    }
	    mGroups = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, mStrings);
	    setListAdapter(mGroups);
	    mContact = mRoster.getContact(mJID);
	    for (String group : mContact.getGroups()) {
		getListView().setItemChecked(mGroups.getPosition(group), true);
	    }
	} catch (RemoteException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Event when group is added.
     */
    private class GroupListOnKeyListener implements OnKeyListener {

	/**
	 * Constructor.
	 */
	public GroupListOnKeyListener() {
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
	    boolean result = false;
	    if (event.getAction() == KeyEvent.ACTION_DOWN) {
		switch (keyCode) {
		    case KeyEvent.KEYCODE_DPAD_CENTER:
		    case KeyEvent.KEYCODE_ENTER:
			if (mText.getText().length() == 0)
			    return false;
			String groupname = mText.getText().toString();
			mGroups.add(groupname);
			mText.setText(null);
			result = true;
			break;
		    default:
			result = false;
		}
	    }
	    return result;
	}

    }

    /**
     * Event click on list group contact.
     */
    private class GroupOnItemClickListener implements OnItemClickListener {

	/**
	 * Constructor.
	 */
	public GroupOnItemClickListener() {
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
	    CheckedTextView textView = (CheckedTextView) v;
	    if (!textView.isChecked()) {
		try {
		    mRoster.addContactToGroup(textView.getText().toString(), mJID);
		} catch (RemoteException e) {
		    e.printStackTrace();
		}
	    } else {
		try {
		    mRoster.removeContactFromGroup(textView.getText().toString(), mJID);
		} catch (RemoteException e) {
		    e.printStackTrace();
		}
	    }

	}

    }

    /**
     * The ServiceConnection used to connect to the Beem service.
     */
    private class BeemServiceConnection implements ServiceConnection {

	/**
	 * Constructor.
	 */
	public BeemServiceConnection() {
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    mXmppFacade = IXmppFacade.Stub.asInterface(service);
	    try {
		mRoster = mXmppFacade.getRoster();
		setAdapter();
	    } catch (RemoteException e) {
		e.printStackTrace();
	    }
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
	    mXmppFacade = null;
	    mRoster = null;
	}
    }

}
