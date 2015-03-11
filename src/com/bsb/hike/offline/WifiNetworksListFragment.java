package com.bsb.hike.offline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.util.TextUtils;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WifiNetworksListFragment extends ListFragment {

	View mContentView = null;
	private List<ScanResult> wifipeers = new ArrayList<ScanResult>();
	private int mIconImageSize;
	private IconLoader iconLoader;
	

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
    }
	
	
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	mContentView = inflater.inflate(R.layout.wifinetwork_details, null);
        return mContentView;
    }
    
    public void updateWifiNetworks(HashMap<String, ScanResult>  strength)
    {
    	  this.wifipeers = new ArrayList<ScanResult>( strength.values()); 
    	  
    	  this.setListAdapter(new WiFiNetworkListAdapter(getActivity(), R.layout.conversation_item, wifipeers));
      	
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	ScanResult scanResult  =   wifipeers.get(position);
    	Boolean status  =  ((DeviceActionListener)getActivity()).connectToHotspot(scanResult);
    	if(status)
    		Toast.makeText(getActivity(), "Connected to" +  scanResult.SSID, Toast.LENGTH_SHORT).show();
    	else
    		Toast.makeText(getActivity(), "Connection Failed" , Toast.LENGTH_SHORT).show();
    	
    }
    private class WiFiNetworkListAdapter extends ArrayAdapter<ScanResult> {

        private List<ScanResult> items;

        public WiFiNetworkListAdapter(Context context , int conversationItem,
				List<ScanResult> wifipeers) {
        	super(context, conversationItem, wifipeers);
        	items  =  wifipeers;
		}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.conversation_item, null);
            }
            ScanResult wifinetwork = items.get(position);
            if (wifinetwork != null) {
            	
            	
                //TextView top = (TextView) v.findViewById(R.id.device_name);
                
                //TextView bottom = (TextView) v.findViewById(R.id.device_details);
            	
            	Context context = getContext();
            	mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
        		iconLoader = new IconLoader(context, mIconImageSize);
        		
        		ContactInfo deviceContact = ContactManager.getInstance().getContact(wifinetwork.SSID);
        		TextView contact_name = (TextView) v.findViewById(R.id.contact);
        		String phoneName = "";
        		if(deviceContact != null && !(TextUtils.isEmpty(deviceContact.getName())))
        			phoneName = deviceContact.getName();
        		else
        			phoneName = wifinetwork.SSID;
        		
        		contact_name.setText(phoneName);
            	ImageView avatarView =  (ImageView) v.findViewById(R.id.avatar);
        		iconLoader.loadImage(wifinetwork.SSID, true, avatarView, false, true, true);
        		TextView deviceStatus =  (TextView) v.findViewById(R.id.last_message_timestamp);
        		deviceStatus.setText(wifinetwork.BSSID);
        		TextView chatStatus = (TextView) v.findViewById(R.id.last_message);
        		String stat =  wifinetwork.BSSID ;
        		deviceStatus.setText(stat);
        		
            }

            return v;

        }

		@Override
		public ScanResult getItem(int position) {
			// TODO Auto-generated method stub
			return super.getItem(position);
			
		}

		@Override
		public int getPosition(ScanResult item) {
			// TODO Auto-generated method stub
			return super.getPosition(item);
		}
    }
    
    
		


}
