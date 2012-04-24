package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;

public class FeedbackActivity extends Activity implements FinishableEvent, OnEditorActionListener
{

	private TextView mFeedbackText;
	private ImageView mFeedbackButton;
	private HikeHTTPTask mTask;
	private ProgressDialog mDialog;
	private TextView mTitleView;
	private View selectedEmoticon;
	private TextView mNameText;

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return mTask;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback);

		mFeedbackText = (TextView) findViewById(R.id.feedback);
		mFeedbackButton = (ImageView) findViewById(R.id.title_icon);
		mTitleView = (TextView) findViewById(R.id.title);
		mNameText = (TextView) findViewById(R.id.user_name);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String name = settings.getString(HikeMessengerApp.NAME, "Set a name!");

		mNameText.setText(name);
		mFeedbackButton.setVisibility(View.VISIBLE);
		mFeedbackText.setOnEditorActionListener(this);
		mFeedbackButton.setImageResource(R.drawable.sendbutton);
		mTitleView.setText(getResources().getString(R.string.feedback));

		Object o = getLastNonConfigurationInstance();
		if (o instanceof HikeHTTPTask)
		{
			mTask = (HikeHTTPTask) o;
			/* we're currently executing a task, so show the progress dialog */
			mTask.setActivity(this);
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));	
		}
	}

	public void onTitleIconClick(View view)
	{
		if (view == mFeedbackButton)
		{
			String feedback = mFeedbackText.getEditableText().toString();
			mTask = new HikeHTTPTask(this, R.string.sending_feedback_failed);
			HikeHttpRequest request = new HikeHttpRequest("/feedback", new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{

				}

				public void onSuccess()
				{
					finish();
					//pass
				}
			});
			JSONObject obj = new JSONObject();
			try
			{
				obj.put("data", feedback);
				request.setJSONData(obj);
			}
			catch (JSONException e)
			{
				Log.wtf("FeedbackActivity", "Jesus, I hate this library");
			}

			mTask.execute(request);
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.sending_feedback));
		}
	}

	public void onEmoticonClicked(View view)
	{
		if(selectedEmoticon != null)
		{
			selectedEmoticon.setSelected(false);
		}
		view.setSelected(true);
		selectedEmoticon = view;
	}

	public void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
		}
	}

	@Override
	public void onFinish(boolean success)
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}

		mTask = null;
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
	{
		if ((v == mFeedbackText) &&
				(actionId == EditorInfo.IME_ACTION_SEND))
		{
			onTitleIconClick(mFeedbackButton);
		}
		return false;
	}

}
