package com.bsb.hike.smartImageLoader;

import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.adapters.ProfileAdapter;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class VoipProfilePicImageLoader extends ProfilePicImageLoader
{

	private ScaleType defaultAvatarScaleType;
	
	public VoipProfilePicImageLoader(Context ctx, int imageSize) 
	{
		super(ctx, imageSize);
		// TODO Auto-generated constructor stub
	}
	
	public void setDefaultAvatarScaleType(ScaleType scaleType)
	{
		defaultAvatarScaleType = scaleType;
	}

	@Override
	protected void setDefaultAvatar(ImageView imageView, String data)
	{
		int idx = data.indexOf(ROUND_SUFFIX);
		if (idx > 0)
		{
			data = new String(data.substring(0, idx));
		}
		else
		{
			int idx1 = data.indexOf(ProfileAdapter.PROFILE_PIC_SUFFIX);
			if (idx1 > 0)
				data = new String(data.substring(0, idx1));
		}
		if(defaultAvatarScaleType!=null)
		{
			imageView.setScaleType(defaultAvatarScaleType);
		}
		imageView.setImageResource(R.drawable.ic_avatar_voip_hires);
	}
}
