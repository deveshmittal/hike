package com.bsb.hike.offline;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.util.TextUtils;

import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.InitiateMultiFileTransferTask;
import com.bsb.hike.ui.GallerySelectionViewer;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.utils.Utils;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WifiNetworksListFragment extends ListFragment {

	private final int POST_TO_FRAGMENT = 0;
	
	View mContentView = null;
	private List<ScanResult> wifipeers = new ArrayList<ScanResult>();
	private int mIconImageSize;
	private IconLoader iconLoader;
	private Handler mHandler;
	Thread updateNetwork =  null ; 
	private boolean requestDialogShown = false;

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
		
		mHandler = new Handler(getActivity().getMainLooper())
		{
			@Override
			public void handleMessage(Message msg)
			{
				switch(msg.what)
				{
					case POST_TO_FRAGMENT:
						HashMap<String, ScanResult> nearbyNetworks = (HashMap<String, ScanResult>) msg.obj;
						updateWifiNetworks(nearbyNetworks);
						break;
					default:
						super.handleMessage(msg);
				}
			}
			
		};
    	super.onActivityCreated(savedInstanceState);
    	this.setListAdapter(new WiFiNetworkListAdapter(getActivity(), R.layout.conversation_item, wifipeers));
    }

	@Override
	public void onResume() {
		runNetworkScan();
		super.onResume();
	}
	
	@Override
	public void onPause() {
		updateNetwork.interrupt();
		super.onPause();
	}
	
	// starts a network scan for every 2sec
	private void runNetworkScan()
	{
		if(isAdded())
		{
		   //((DeviceActionListener) getActivity()).resetWifi();
		}
		if(updateNetwork==null)
		{
			updateNetwork =  (new Thread()
			{
				@Override
				public void run() {
					while(!isInterrupted())
					{
						if(isAdded())
						{
							HashMap<String, ScanResult> nearbyNetworks = ((DeviceActionListener) getActivity()).getDistinctWifiNetworks();
							Message targetMessage = mHandler.obtainMessage(POST_TO_FRAGMENT, nearbyNetworks);
							targetMessage.sendToTarget();	
							try 
							{
								sleep(2*1000);
							} 
							catch (InterruptedException e) 
							{
								e.printStackTrace();
							}	
						}
						
					}
				}
				
			});
			updateNetwork.start();
		}
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	mContentView = inflater.inflate(R.layout.wifinetwork_details, null);
        return mContentView;
    }
    
    public void updateWifiNetworks(HashMap<String, ScanResult> strength)
    {
		wifipeers.clear();
		wifipeers.addAll(strength.values());
		((WiFiNetworkListAdapter)getListAdapter()).notifyDataSetChanged();
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) 
    {
    	Boolean status  =  ((DeviceActionListener)getActivity()).connectToHotspot(wifipeers.get(position));
    	if(status)
    	{
    		String ssid  = wifipeers.get(position).SSID;
    		String senderMsisdn  = ssid.split("_")[1];
    		OfflineFileTransferManager.getInstance().switchOnReceivers(getActivity(), senderMsisdn);
    		Toast.makeText(getActivity(), "Connected to" +  wifipeers.get(position).SSID, Toast.LENGTH_SHORT).show();
    	}
    	else
    		Toast.makeText(getActivity(), "Connection Failed" , Toast.LENGTH_SHORT).show();	
    }
    
    private class WiFiNetworkListAdapter extends ArrayAdapter<ScanResult> 
    {
        private List<ScanResult> items;
        
        public WiFiNetworkListAdapter(Context context , int conversationItem,
				List<ScanResult> wifipeers) {
        	super(context, conversationItem, wifipeers);
        	items  =  wifipeers;
		}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
        	View v = convertView;
            if (v == null) 
            {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.conversation_item, null);
            }

            final ScanResult wifinetwork = items.get(position);
            if (wifinetwork != null) 
            {    	
            	Context context = getContext();
	            mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
        		iconLoader = new IconLoader(context, mIconImageSize);
        		
        		final String senderMsisdn = wifinetwork.SSID.split("_")[1];
        		ContactInfo deviceContact = ContactManager.getInstance().getContact(senderMsisdn);
        		TextView contact_name = (TextView) v.findViewById(R.id.contact);
        		
        		String phoneName = "";
        		if(deviceContact != null && !(TextUtils.isEmpty(deviceContact.getName())))
        			phoneName = deviceContact.getName() ;
        		else
        			phoneName = senderMsisdn;
        		
        		contact_name.setText(phoneName);
            	ImageView avatarView =  (ImageView) v.findViewById(R.id.avatar);
        		iconLoader.loadImage(wifinetwork.SSID, true, avatarView, false, true, true);
        		TextView deviceStatus =  (TextView) v.findViewById(R.id.last_message_timestamp);
        		deviceStatus.setText(wifinetwork.BSSID);
        		String stat =  wifinetwork.BSSID ;
        		deviceStatus.setText(stat);
        		if(!requestDialogShown)
        		{
        			requestDialogShown =  true ;
        			HikeDialog.showDialog(getActivity(), HikeDialog.OFFLINE_USER_REQUEST,  new HikeDialog.HikeDialogListener()
						{
							@Override
							public void onSucess(Dialog dialog)
							{ }
	
							@Override
							public void negativeClicked(Dialog dialog)
							{
								dialog.dismiss();
								((DeviceActionListener)getActivity()).forgetWifiNetwork();
								//requestDialogShown = false;
							}
							
							@Override
							public void positiveClicked(Dialog dialog)
							{
								dialog.dismiss();
								//requestDialogShown = false;
								Boolean status  =  ((DeviceActionListener)getActivity()).connectToHotspot(wifinetwork);
						    	if(status)
						    	{
						    		OfflineFileTransferManager.getInstance().switchOnReceivers(getActivity(), senderMsisdn);
						    		Toast.makeText(getActivity(), "Connected to" +  wifinetwork.SSID, Toast.LENGTH_SHORT).show();
						    	}
						    	else
						    		Toast.makeText(getActivity(), "Connection Failed" , Toast.LENGTH_SHORT).show();
							}
	
							@Override
							public void neutralClicked(Dialog dialog)
							{ 
								//requestDialogShown = false;
							}
							
						}, (Object[]) new String[]{phoneName});
        		}
	        }
            return v;
        }
    }
}
