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

public class NUXStartActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{

	private Button butInviteFriends, butSkip;

	private TextView tvRewardMain, tvRewardSubText;

	private ImageView imgvInviteFrd;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

//		if (!NUXManager.getInstance(this).is_NUX_Active() && NUXManager.getInstance(this).getCurrentState() == NUXConstants.INVFRD)
//		{
//			return;
//		}
//		if (Utils.requireAuth(this))
//		{
//			return;
//		}

		getSupportActionBar().hide();

		HikeMessengerApp app = (HikeMessengerApp) getApplication();

		app.connectToService();

		setContentView(R.layout.nux_start);

		bindViews();

		bindListeners();

		// processViewElemets();

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
		NUXManager mmNuxManager = NUXManager.getInstance(this);
		NuxInviteFriends mmInviteFriends = mmNuxManager.getNuxInviteFriendsPojo();

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
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.but_skip:
			IntentManager.openHomeActivity(this);
			Toast.makeText(this, "Button Skip", Toast.LENGTH_LONG).show();
			break;

		case R.id.but_inviteFrnds:
			Toast.makeText(this, "Invite Friends", Toast.LENGTH_LONG).show();
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
