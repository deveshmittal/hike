package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class HorizontalFriendsFragment extends Fragment {
//	
//    ThingsAdapter adapter;
//    FragmentActivity listener;
	
	private Map<ContactInfo, View> linearViews;

	private LinearLayout ll;
	
    public static HorizontalFriendsFragment newInstance(int someInt, String someTitle) {
        HorizontalFriendsFragment fragmentDemo = new HorizontalFriendsFragment();
        Bundle args = new Bundle();
        args.putInt("someInt", someInt);
        args.putString("someTitle", someTitle);
        fragmentDemo.setArguments(args);
        return fragmentDemo;
    }

        // This event fires 1st, before creation of fragment or any views
    // The onAttach method is called when the Fragment instance is associated with an Activity. 
    // This does not mean the Activity is fully initialized.
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        this.listener = (FragmentActivity) activity;
    }

        // This event fires 2nd, before views are created for the fragment
    // The onCreate method is called when the Fragment instance is being created, or re-created.
    // Use onCreate for any standard setup that does not require the activity to be fully created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        ArrayList<Thing> things = new ArrayList<Thing>();
//        adapter = new ThingsAdapter(getActivity(), things);
    }

        // This event fires 3rd, and is the first time views are available in the fragment
    // The onCreateView method is called when Fragment should create its View object hierarchy. 
    // Use onCreateView to get a handle to views as soon as they are freshly inflated
    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle savedInstanceState) {
        View v =  inf.inflate(R.layout.display_selected_friends, parent, false);
        linearViews = new HashMap<ContactInfo,View>();
        ll = (LinearLayout) v.findViewById(R.id.horizontalView);
        
        //ll.addView();
        ListView lv = (ListView) v.findViewById(R.id.abs__action_bar_title);
        //lv.setAdapter(adapter);
        return v;
    }

    public void toggleViews(ContactInfo contactInfo){
    	if(linearViews.containsKey(contactInfo))
    	{
    		ll.removeView(linearViews.get(contactInfo));
    		linearViews.remove(contactInfo);
    	}
    	else
    	{
    		LayoutInflater inf = getLayoutInflater(null);
        	View py = inf.inflate(R.layout.friends_horizontal_item,null);
        	TextView tv = (TextView)py.findViewById(R.id.msisdn);
        	tv.setText(contactInfo.getFirstName());
    		linearViews.put(contactInfo,py);
    		ll.addView(py);
    	}
    	//ll.invalidate();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	LayoutInflater inf = getLayoutInflater(savedInstanceState);
    	View py = inf.inflate(R.layout.friends_horizontal_item,null);
        View py2 = inf.inflate(R.layout.friends_horizontal_item, null);
       
        ll.addView(py);
        ll.addView(py2);
    }

}
