package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;

public class FeedbackActivity extends Activity implements OnClickListener, FinishableEvent, OnEditorActionListener
{

	private TextView mFeedbackText;
	private Button mFeedbackButton;
	private HikeHTTPTask mTask;
	private ProgressDialog mDialog;
	private TextView mTitleView;


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
		mFeedbackButton = (Button) findViewById(R.id.feedback_submit);
		mTitleView = (TextView) findViewById(R.id.title);

		mFeedbackButton.setOnClickListener(this);
		mFeedbackText.setOnEditorActionListener(this);

		mTitleView.setText(getResources().getString(R.string.contact_us));

		Object o = getLastNonConfigurationInstance();
		if (o instanceof HikeHTTPTask)
		{
			mTask = (HikeHTTPTask) o;
			/* we're currently executing a task, so show the progress dialog */
			mTask.setActivity(this);
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));	
		}
	}

	@Override
	public void onClick(View view)
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
			onClick(mFeedbackButton);
		}
		return false;
	}
	
}
