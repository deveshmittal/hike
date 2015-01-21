package com.bsb.hike.ui;

import android.R;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.widget.HorizontalScrollView;

public class FriendsHorizontalFragment extends Fragment {
	HorizontalScrollView scrollView;
	
	@Override
	public void onAttach(Activity activity) {
		scrollView = new HorizontalScrollView(activity);
		
		super.onAttach(activity);
	}
}
