package com.bsb.hike.ui;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.NuxSelectFriends;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;


public class HorizontalFriendsFragment extends Fragment implements OnClickListener{
	
	private Map<String, View> viewMap;
	private final String emptyTag="emptyView";

	private LinearLayout viewStack;
	private int maxShowListCount;
	private int preSelectedCount;
	private HorizontalScrollView hsc; 
	private NuxSelectFriends selectFriends;
	private TextView sectionDisplayMessage;
	private TextView nxtBtn;
	private ImageView backBtn;
	private HashSet<String> contactsDisplayed;

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle savedInstanceState) {

        View v =  inf.inflate(R.layout.display_selected_friends, parent, false); 
        String selectedFriendsString = getActivity().getIntent().getStringExtra("selected_friends");

        viewStack = (LinearLayout) v.findViewById(R.id.horizontalView);
        hsc = (HorizontalScrollView) v.findViewById(R.id.scrollView);
		sectionDisplayMessage = (TextView) v.findViewById(R.id.nux_header_selection_text);
        nxtBtn = (TextView) v.findViewById(R.id.nux_next_selection_button);
        backBtn = (ImageView) v.findViewById(R.id.back_button);
        nxtBtn.setOnClickListener(this);
        backBtn.setOnClickListener(this);
        
        viewMap = new LinkedHashMap<String, View>();
        contactsDisplayed = new HashSet<String>();
		NUXManager nm = NUXManager.getInstance();
		selectFriends = nm.getNuxSelectFriendsPojo();
		preSelectedCount = nm.getCountLockedContacts() + nm.getCountUnlockedContacts();
		//First Time Nux
		if(nm.getCurrentState() == NUXConstants.NUX_NEW||NUXManager.getInstance().getCurrentState()==NUXConstants.NUX_SKIPPED)
		{
			maxShowListCount = nm.getNuxTaskDetailsPojo().getMin();
		}
		// invite more nux 
		else if(nm.getCurrentState() == NUXConstants.NUX_IS_ACTIVE)
		{
			maxShowListCount = nm.getNuxTaskDetailsPojo().getMax();
			for (String msisdn : nm.getLockedContacts()) {
				addContactView(msisdn, viewStack.getChildCount());
			}
			showNextButton(true);
			scrollHorizontalView(0, viewStack.getChildAt(0).getWidth());
		}
		
		//this only appears for custom message screen
		if (!TextUtils.isEmpty(selectedFriendsString)) {
			String[] arrmsisdn = selectedFriendsString.split(NUXConstants.STRING_SPLIT_SEPERATOR);
			
			contactsDisplayed.addAll(Arrays.asList(arrmsisdn));
			for (String msisdn : contactsDisplayed) {
				if(nm.getLockedContacts().contains(msisdn) || nm.getUnlockedContacts().contains(msisdn)) {
					viewStack.removeView(viewMap.get(msisdn));
				} else {
					addContactView(msisdn, viewStack.getChildCount());
				}
			}
		} else {
			for (int i = 0; i < maxShowListCount - preSelectedCount; i++) 
				addEmptyView();
			changeDisplayString(0);
		}
		return v;
	}
    
    private void addEmptyView(){
    	View emptyView = getLayoutInflater(null).inflate(R.layout.friends_horizontal_item,null);
    	emptyView.setTag(emptyTag);
    	viewStack.addView(emptyView);
    }
    
    private void addContactView(String msisdn, int index){
    	if(!viewMap.containsKey(msisdn)){
    		View contactView = getLayoutInflater(null).inflate(R.layout.friends_horizontal_item,null);
    		contactView.setTag(msisdn);
        	TextView tv = (TextView)contactView.findViewById(R.id.msisdn);
        	ImageView iv = (ImageView ) contactView.findViewById(R.id.profile_image);
        	
			if(ContactManager.getInstance().getIcon(msisdn, true) ==null){
				iv.setScaleType(ScaleType.CENTER_INSIDE);
				iv.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(msisdn, true));
				iv.setImageResource(R.drawable.ic_profile);
			}
			else
			{
				iv.setImageDrawable(ContactManager.getInstance().getIcon(msisdn, true));
			}
        	ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn);
        	
        	if(contactInfo != null)
        		tv.setText(contactInfo.getFirstNameAndSurname());
        	else
        		tv.setText(msisdn);
        	viewStack.addView(contactView, index);
        	viewMap.put(msisdn,contactView);
    	}
    	
    }
   
    public boolean removeView(ContactInfo contactInfo){
    	
    	if(NUXManager.getInstance().getLockedContacts().contains(contactInfo.getMsisdn()) || NUXManager.getInstance().getUnlockedContacts().contains(contactInfo.getMsisdn()))
    		return false;
    	
    	int filledCount = 0; 
    	int index  = 0;
    	View replaceView = null;
    	for (int i = 0; i < viewStack.getChildCount(); i++) {
            View v = viewStack.getChildAt(i);
            if(!v.getTag().toString().contains(emptyTag)){
            	if(contactInfo.getMsisdn().equals(v.getTag().toString()))
            		replaceView = v; index = i;
            	filledCount++;
            }
            
        }
    	//if(count  == 5) return false;
    	changeDisplayString(filledCount - 1 - preSelectedCount);
		viewStack.removeView(replaceView); 	
		viewMap.remove(contactInfo.getMsisdn());
		scrollHorizontalView(index -1 , replaceView.getWidth());
		addEmptyView();

        return true;
    }
  
    
    private void showNextButton(boolean show){
    	if(show){
    		nxtBtn.setTextColor(getResources().getColor(R.color.blue_hike));
    	} else {
    		nxtBtn.setTextColor(getResources().getColor(R.color.light_gray_hike));
    	}
    	nxtBtn.setEnabled(show);
    }

    private void changeDisplayString(int selectionCount){
    	
    	if(selectionCount >= maxShowListCount - preSelectedCount){
    		showNextButton(true);
        	sectionDisplayMessage.setText(selectFriends.getTitle3());
    	} else if(selectionCount > 0 && selectionCount < maxShowListCount - preSelectedCount){
    		showNextButton(NUXManager.getInstance().getCurrentState() == NUXConstants.NUX_IS_ACTIVE);
        	sectionDisplayMessage.setText(String.format(selectFriends.getTitle2(), maxShowListCount - selectionCount - preSelectedCount));
    	} else if(selectionCount <= 0){
    		showNextButton(false);
    		sectionDisplayMessage.setText(String.format(selectFriends.getSectionTitle(), maxShowListCount - selectionCount - preSelectedCount));
    	}

    }
    
    private void scrollHorizontalView(int count, int width){
    	hsc.scrollTo(count*width, 0);
    }

    public boolean addView(ContactInfo contactInfo){
		
    	if(viewMap.containsKey(contactInfo.getMsisdn())){
    		return false;
    	}
    	int index = 0,emptyCount = 0;
    	View replaceView = null;
    	for (int i = 0; i < viewStack.getChildCount(); i++) {
            View v = viewStack.getChildAt(i);
            if(v.getTag().toString().contains(emptyTag)){
            	if(emptyCount == 0){
            		index = i; replaceView = v;
            	}
            	emptyCount++;
            }
            
        }
    	//count here means total non selected contacts
    	if(emptyCount == 0) return false;
    	changeDisplayString((maxShowListCount - preSelectedCount) - (emptyCount - 1));
    	addContactView(contactInfo.getMsisdn(), index);
    	scrollHorizontalView(maxShowListCount - emptyCount - 1, replaceView.getWidth());
    	viewStack.removeView(replaceView);
		return true;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);    
    }
    
	@Override
	public void onClick(View v) 
	{
		switch(v.getId()){
		
			case R.id.back_button:
				getActivity().finish();
				break;
				
			case R.id.nux_next_selection_button:
	
				NUXManager nm = NUXManager.getInstance();
				if(getActivity() instanceof ComposeChatActivity)
				{
					HashSet<String> contactsNux = new HashSet<String>(viewMap.keySet());
					nm.startNuxCustomMessage(contactsNux.toString().replace("[", "").replace("]", ""), getActivity());
					
			}
			else if (getActivity() instanceof NuxSendCustomMessageActivity)
			{
				nm.sendMessage(contactsDisplayed, ((NuxSendCustomMessageActivity) getActivity()).getCustomMessage());
				nm.saveNUXContact(contactsDisplayed);
				nm.sendMsisdnListToServer(contactsDisplayed);
				nm.setCurrentState(NUXConstants.NUX_IS_ACTIVE);
				Intent intent = new Intent(getActivity(), HomeActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);

				}
				break;
		}
		
	}

}
