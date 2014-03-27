package com.bsb.hike.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;

public final class WelcomeTutorialFragment extends Fragment
{
	private int fragmentNum;
	private boolean isMicromaxDevice;

	public static WelcomeTutorialFragment newInstance(int position, boolean isMicromaxDevice) {
		WelcomeTutorialFragment fragment = new WelcomeTutorialFragment();
	    fragment.setFragmentNum(position);
	    fragment.setIsMicromaxDevice(isMicromaxDevice);
	    return fragment;
	}
	
	public WelcomeTutorialFragment()
	{
		fragmentNum = 0;
		this.isMicromaxDevice = isMicromaxDevice;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.tutorial_fragments, null);
		TextView tutorialHeader = (TextView) parent.findViewById(R.id.tutorial_title);
		ImageView tutorialImage = (ImageView) parent.findViewById(R.id.tutorial_img);
		ImageView micromaxImage = (ImageView) parent.findViewById(R.id.ic_micromax);
		switch (fragmentNum)
		{
		case 0:
			tutorialHeader.setText(R.string.tutorial1_header_title);
			tutorialImage.setBackgroundResource(R.drawable.tutorial1_img);
			micromaxImage.setVisibility(isMicromaxDevice ? View.VISIBLE : View.GONE);
			break;
		case 1:
			tutorialHeader.setText(R.string.tutorial2_header_title);
			tutorialImage.setBackgroundResource(R.drawable.tutorial2_img);
			micromaxImage.setVisibility(View.GONE);
			break;
		case 2:
			tutorialHeader.setText(R.string.tutorial3_header_title);
			tutorialImage.setBackgroundResource(R.drawable.tutorial3_img);
			micromaxImage.setVisibility(View.GONE);
			break;
		}
		return parent;
	}

	public void setFragmentNum(int fragmentNum)
	{
		this.fragmentNum = fragmentNum;
	}
	
	public void setIsMicromaxDevice(boolean isMicromaxDevice)
	{
		this.isMicromaxDevice = isMicromaxDevice;
	}

}