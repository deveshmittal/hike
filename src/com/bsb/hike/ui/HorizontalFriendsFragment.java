package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.NuxSelectFriends;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.Utils;
import com.google.android.gms.plus.model.people.Person.Image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;


public class HorizontalFriendsFragment extends Fragment implements OnClickListener{
//	
//    ThingsAdapter adapter;
//    FragmentActivity listener;
	
	private Map<ContactInfo, View> linearViews;

	private LinearLayout ll;
	private int maxShowListCount = 2;
	private HorizontalScrollView hsc; 
	private NuxSelectFriends selectFriends;
	private TextView sectionDisplayMessage;
	private TextView nxtBtn;
	
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

        ll = (LinearLayout) v.findViewById(R.id.horizontalView);
        hsc = (HorizontalScrollView) v.findViewById(R.id.scrollView);
        nxtBtn = (TextView) v.findViewById(R.id.nux_next_selection_button);
        nxtBtn.setOnClickListener(this);

    	NUXManager nm = NUXManager.getInstance(getActivity());
    	selectFriends = nm.getNuxSelectFriendsPojo();
    	sectionDisplayMessage = (TextView) v.findViewById(R.id.nux_header_selection_text);
    	changeDisplayString(0);
        linearViews = new LinkedHashMap<ContactInfo,View>();
        //ll.addView();
     //   ListView lv = (ListView) v.findViewById(R.id.abs__action_bar_title);
        //lv.setAdapter(adapter);
        return v;
    }
    
    public boolean removeView(ContactInfo contactInfo){
    	//View c = linearViews.get(contactInfo); 
		//linearViews.remove(contactInfo);
    	int count = 0; 
    	int index  = 0;
    	View replaceView = null;
    	for (int i = 0; i < ll.getChildCount(); i++) {
            View v = ll.getChildAt(i);
            if(!v.getTag().toString().contains("tag")){
            	if(contactInfo.getMsisdn().equals(v.getTag().toString()))
            		replaceView = v; index = i;
            	count++;
            }
            
        }
    	//if(count  == 5) return false;
    	changeDisplayString(count - 1);
		ll.removeView(replaceView); 	
		linearViews.remove(contactInfo);
		scrollHorizontalView(index -1 , replaceView.getWidth());
		LayoutInflater inf = getLayoutInflater(null);
		View py = inf.inflate(R.layout.friends_horizontal_item,null);
		py.setTag("tag");
		ll.addView(py);

        Logger.d("UmangX", ll.getChildCount() + "");
        return true;
    }
  
    
    private void toggleNextButton(boolean show){
    		nxtBtn.setEnabled(show);
    }
    private void changeDisplayString(int selectionCount){
    	
    	int count = selectionCount;
    	
    	if(count >= maxShowListCount){
    		toggleNextButton(true);
    		if(selectFriends != null){
        		sectionDisplayMessage.setText(selectFriends.getTitle3());
        	} else {
        		sectionDisplayMessage.setText("You are set to go!");
        	}		
    	} else if(count > 0 && count < maxShowListCount){
    		toggleNextButton(false);
        	if(selectFriends != null){
        		sectionDisplayMessage.setText(String.format(selectFriends.getTitle2(), maxShowListCount - count));
        	} else {
        		sectionDisplayMessage.setText(String.format("Just %d more to go!", maxShowListCount - count));
        	}
    	} else {
    		toggleNextButton(false);
    		if(selectFriends != null){
        		sectionDisplayMessage.setText(String.format(selectFriends.getSectionTitle(), maxShowListCount - count));
        	} else {
        		sectionDisplayMessage.setText(String.format("Please select friends", maxShowListCount - count));
        	}
    	}

    }
    
    private void scrollHorizontalView(int count, int width){
    	hsc.scrollTo(count*width, 0);
    }
    public boolean addView(ContactInfo contactInfo){
    	LayoutInflater inf = getLayoutInflater(null);
		//ViewSwitcher vs = new ViewSwitcher(getActivity());
		View py = inf.inflate(R.layout.friends_horizontal_item,null);
		py.setTag(contactInfo.getMsisdn());
		//vs.addView(py); 
    	TextView tv = (TextView)py.findViewById(R.id.msisdn);
    	ImageView iv = (ImageView ) py.findViewById(R.id.profile_image);
    	iv.setImageDrawable(ContactManager.getInstance().getIcon(contactInfo.getMsisdn(),true));
    	tv.setText(contactInfo.getFirstNameAndSurname());
    	int index = 0,count = 0;
    	View replaceView = null;
    	for (int i = 0; i < ll.getChildCount(); i++) {
            View v = ll.getChildAt(i);
            if(v.getTag().toString().contains("tag")){
            	if(count == 0){
            		index = i; replaceView = v;
            	}
            	count++;
            }
            
        }
    	//count here means total non selected contacts
    	if(count ==0) return false;
    	changeDisplayString(maxShowListCount - count +1);
    	scrollHorizontalView(maxShowListCount - count - 1, replaceView.getWidth());
        Logger.d("UmangX", index+ " value of I");
    	ll.addView(py, index);
    	linearViews.put(contactInfo,py);
    	ll.removeView(replaceView);
    	//ll.addView(v);
    	//ll.indexOfChild(py);
		//linearViews.put(contactInfo,py);
		//ll.addView(py);
		return true;
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
    		//ll.addView(py);
    		ViewSwitcher vs = (ViewSwitcher) ll.findViewWithTag("umang_2");
    		vs.addView(py);
    		vs.setOutAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_down)); 
    		vs.setInAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_up)); 
    		vs.showNext();
    	}
 
    	//ll.invalidate();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	LayoutInflater inf = getLayoutInflater(savedInstanceState);
    	//ViewSwitcher vs = new ViewSwitcher(getActivity());
    	//View p = inf.inflate(R.layout.friends_horizontal_item,null);
    	//vs.setTag("umang_2");
    	//vs.addView(p);
    	//ll.addView(vs);
    	View py; 
    	for(int i=0;i<maxShowListCount;i++){
    		py = inf.inflate(R.layout.friends_horizontal_item,null);
    		py.setTag("tag");
    		ll.addView(py);
    	}
       
    }

	@Override
	public void onClick(View v) {
		NUXManager nm = NUXManager.getInstance(getActivity());
    	Logger.d("UmangX", "next clicked");
    	HashSet<String> contactsNux = new HashSet<String>();
		for(ContactInfo contactInfo : linearViews.keySet()){
			contactsNux.add(contactInfo.getMsisdn());
		}
		nm.sendMessage(contactsNux, nm.getNuxCustomMessagePojo().getSmsMessage() , getActivity());
		nm.saveNUXContact(contactsNux, getActivity());
		nm.sendMsisdnListToServer(contactsNux);
		nm.setCurrentState(NUXConstants.NUX_IS_ACTIVE);
		nm.startNuxCustomMessage(getActivity());
		// TODO call the send message activity.
	}

}
