package com.bsb.hike.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.NuxInviteFriends;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.Utils;

public class NUXInviteActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{

	private Button butInviteFriends, butSkip;

	private TextView tvRewardMain, tvRewardSubText;

	private ImageView imgvInviteFrd;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		if (Utils.requireAuth(this))
		{
			return;
		}

		Utils.blockOrientationChange(this);
		
		getSupportActionBar().hide();

		HikeMessengerApp app = (HikeMessengerApp) getApplication();

		app.connectToService();

		setContentView(R.layout.nux_invite_friends);

		bindViews();

		bindListeners();

		processViewElemets();

	}

	private void bindViews()
	{
		butInviteFriends = (Button) findViewById(R.id.but_inviteFrnds);

		butSkip = (Button) findViewById(R.id.but_skip);

		tvRewardMain = (TextView) findViewById(R.id.tv_reward_main);

		tvRewardSubText = (TextView) findViewById(R.id.tv_reward_subText);

		imgvInviteFrd = (RecyclingImageView) findViewById(R.id.imgv_invitefrd);
	}

	private void bindListeners()
	{
		butInviteFriends.setOnClickListener(this);

		butSkip.setOnClickListener(this);

	}

	private void processViewElemets()
	{
		NUXManager mmNuxManager = NUXManager.getInstance();
		NuxInviteFriends mmInviteFriends = mmNuxManager.getNuxInviteFriendsPojo();
		if (mmInviteFriends != null)
		{
			if (!TextUtils.isEmpty(mmInviteFriends.getButText()))
			{
				butInviteFriends.setText(mmInviteFriends.getButText());
			}

			if (!TextUtils.isEmpty(mmInviteFriends.getRewardTitle()))
			{
				tvRewardMain.setText(mmInviteFriends.getRewardTitle());
			}

			if (!TextUtils.isEmpty(mmInviteFriends.getRewardSubText()))
			{
				tvRewardSubText.setText(mmInviteFriends.getRewardSubText());
			}
			if (mmInviteFriends.getImageBitmap() != null)
			{
				imgvInviteFrd.setImageBitmap(mmInviteFriends.getImageBitmap());
			}
			if (!mmInviteFriends.isToggleSkipButton())
			{
				butSkip.setVisibility(View.INVISIBLE);
			}
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.but_skip:
			IntentManager.openHomeActivity(this);
			NUXManager.getInstance().setCurrentState(NUXConstants.NUX_SKIPPED);
			finish();
			break;

		case R.id.but_inviteFrnds:
		
			NUXManager.getInstance().startNuxSelector(this);
		
			break;
		}

	}

	@Override
	protected void onDestroy()
	{
		// TODO Auto-generated method stub
		super.onDestroy();
	}
}
