package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gms.plus.model.people.Person.Image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;


public class HorizontalFriendsFragment extends Fragment {
//	
//    ThingsAdapter adapter;
//    FragmentActivity listener;
	
	private Map<ContactInfo, View> linearViews;

	private LinearLayout ll;
	private HorizontalScrollView hsc; 
	
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
        linearViews = new LinkedHashMap<ContactInfo,View>();
        ll = (LinearLayout) v.findViewById(R.id.horizontalView);
        hsc = (HorizontalScrollView) v.findViewById(R.id.scrollView);
        //ll.addView();
        ListView lv = (ListView) v.findViewById(R.id.abs__action_bar_title);
        //lv.setAdapter(adapter);
        return v;
    }

    public void toggleViews(ContactInfo contactInfo){
    	if(linearViews.containsKey(contactInfo))
    	{
    		View c = linearViews.get(contactInfo); 
            Logger.d("UmangX", c.getWidth() + "");
    		linearViews.remove(contactInfo);
    		ll.removeView(c);
    	}
    	else
    	{
    		LayoutInflater inf = getLayoutInflater(null);
    		//ViewSwitcher vs = new ViewSwitcher(getActivity());
    		View py = inf.inflate(R.layout.friends_horizontal_item,null);
    		//vs.addView(py); 
        	TextView tv = (TextView)py.findViewById(R.id.msisdn);
        	ImageView iv = (ImageView ) py.findViewById(R.id.profile_image);
        	// (new ProfilePicImageLoader(getActivity(), 94)).loadImage(contactInfo.getMsisdn(), iv, false, true, false);
        	//Bitmap tempBitmap = HikeBitmapFactory.scaleDownBitmap(Utils.getProfileImageFileName(contactInfo.getMsisdn()), HikeConstants.PROFILE_IMAGE_DIMENSIONS, HikeConstants.PROFILE_IMAGE_DIMENSIONS, true, true);
        	iv.setImageDrawable(ContactManager.getInstance().getIcon(contactInfo.getMsisdn(),true));
        	tv.setText(contactInfo.getFirstNameAndSurname());
    		linearViews.put(contactInfo,py);
    		ll.addView(py);
    	}
    	//ll.invalidate();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	LayoutInflater inf = getLayoutInflater(savedInstanceState);
    	View py; 
    	for(int i=0;i<savedInstanceState.getInt("count");i++){
    		py = inf.inflate(R.layout.friends_horizontal_item,null);
    		py.setTag("tag" + i);
    		ll.addView(py);
    	}
       
    }

}
