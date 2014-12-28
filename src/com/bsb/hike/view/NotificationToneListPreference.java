package com.bsb.hike.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class NotificationToneListPreference extends ListPreference implements DialogInterface.OnClickListener
{
	private Drawable mIcon;

	private Context mContext;

	private int mClickedDialogEntryIndex;

	private int mCancelDialogEntryIndex;

	private List<String> ringtonesList;

	private Map<String, Uri> ringtonesNameURIMap;

	private Cursor notifSoundCursor;

	private CharSequence[] rintoneCharSeq;

	private boolean isClickedButtonPressed = false;

	public NotificationToneListPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
		this.ringtonesList = new ArrayList<String>();
		this.ringtonesNameURIMap = new HashMap<String, Uri>();
		setIcon(context, attrs);
	}

	private void setIcon(Context context, AttributeSet attrs)
	{
		String iconName = attrs.getAttributeValue(null, "icon");
		iconName = iconName.split("/")[1];
		int id = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());

		this.mIcon = context.getResources().getDrawable(id);
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
		if ((imageView != null) && (this.mIcon != null))
		{
			imageView.setImageDrawable(this.mIcon);
			imageView.setVisibility(View.VISIBLE);
		}
	}

	private int getValueIndex()
	{
		return findIndexOfValue(this.getValue().toString());
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder)
	{
		updateEntryAndValues();
		mClickedDialogEntryIndex = getValueIndex();
		mCancelDialogEntryIndex = getValueIndex();
		builder.setSingleChoiceItems(this.getEntries(), mClickedDialogEntryIndex, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				mClickedDialogEntryIndex = which;
				playSoundAsPerToneClicked();
			}
		});

		builder.setPositiveButton(R.string.ok, this);
		builder.setNegativeButton(R.string.cancel, this);
	}

	private void playSoundAsPerToneClicked()
	{
		this.setValue(this.getEntryValues()[mClickedDialogEntryIndex].toString());
		Object newValue = this.getEntryValues()[mClickedDialogEntryIndex];
		if (mContext.getString(R.string.notif_sound_off).equals(newValue.toString()))
		{
			// Here No Sound is played
			return;
		}
		else if (mContext.getString(R.string.notif_sound_Hike).equals(newValue.toString()))
		{
			Utils.playSoundFromRaw(mContext, R.raw.hike_jingle_15);
		}
		else if (mContext.getString(R.string.notif_sound_default).equals(newValue.toString()))
		{
			Utils.playSound(mContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		}
		else
		{
			Utils.playSound(mContext, ringtonesNameURIMap.get((String) newValue));
		}
	}

	public void onClick(DialogInterface dialog, int which)
	{
		switch (which)
		{
		case DialogInterface.BUTTON_POSITIVE:
			String selectedRingtoneValue = this.getEntryValues()[mClickedDialogEntryIndex].toString();
			this.setValue(selectedRingtoneValue);
			setTitle(mContext.getString(R.string.notificationSoundTitle) + " - " + selectedRingtoneValue);
			String selectedRintoneUri = ringtonesNameURIMap.get(selectedRingtoneValue) != null ? ringtonesNameURIMap.get(selectedRingtoneValue).toString() : Uri.EMPTY.toString();
			HikeSharedPreferenceUtil.getInstance(mContext).saveData(HikeMessengerApp.NOTIFICATION_TONE_URI, selectedRintoneUri);
			dialog.dismiss();
			break;

		case DialogInterface.BUTTON_NEGATIVE:
			this.setValue(this.getEntryValues()[mCancelDialogEntryIndex].toString());
			dialog.dismiss();
			break;

		default:
			break;
		}
	}

	/**
	 * Sets entries and Values for SoundListPref via AsyncTask
	 */
	public void updateSoundPrefData()
	{
		new AsyncTask<Void, Void, Void>()
		{
			protected void onPreExecute()
			{
				initRingtoneLists(ringtonesList);
			};

			@Override
			protected Void doInBackground(Void... params)
			{
				try
				{
					RingtoneManager ringtoneMgr = new RingtoneManager(mContext);
					ringtoneMgr.setType(RingtoneManager.TYPE_NOTIFICATION);
					notifSoundCursor = ringtoneMgr.getCursor();
					if (notifSoundCursor != null)
					{
						int notifSoundCount = notifSoundCursor.getCount();
						if (notifSoundCount == 0 && !notifSoundCursor.moveToFirst())
						{
							//No Notification Tone was present in device
						}
						else
						{
							String ringtoneName = "";
							while (!notifSoundCursor.isAfterLast() && notifSoundCursor.moveToNext())
							{
								int currentPosition = notifSoundCursor.getPosition();
								ringtoneName = ringtoneMgr.getRingtone(currentPosition).getTitle(mContext);
								ringtonesNameURIMap.put(ringtoneName, ringtoneMgr.getRingtoneUri(currentPosition));
								ringtonesList.add(ringtoneName);
							}
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				return null;
			}

			protected void onPostExecute(Void result)
			{
				if (notifSoundCursor != null)
					notifSoundCursor.close();
				updateEntryAndValues();
				notifyChanged();
				
				// User has clicked on Notification Button on preferences screen
				if (isClickedButtonPressed)
					NotificationToneListPreference.super.onClick();
				else // Was a case of screen rotation
					NotificationToneListPreference.super.showDialog(null);
			};

		}.execute();
	}

	private void initRingtoneLists(List<String> ringtonesList)
	{
		ringtonesList.add(mContext.getResources().getString(R.string.notif_sound_off));
		ringtonesList.add(mContext.getResources().getString(R.string.notif_sound_default));
		ringtonesList.add(mContext.getResources().getString(R.string.notif_sound_Hike));
	}

	private void updateEntryAndValues()
	{
		rintoneCharSeq = ringtonesList.toArray(new CharSequence[ringtonesList.size()]);
		setEntries(rintoneCharSeq);
		setEntryValues(rintoneCharSeq);
	}

	/**
	 * Handles Click of Notification Option in Preferences Screen
	 */
	@Override
	protected void onClick()
	{
		isClickedButtonPressed = true;
		updateSoundPrefData();
	}

	@Override
	protected void showDialog(Bundle state)
	{
		// It is the case of screen rotation
		if (!isClickedButtonPressed)
		{
			updateSoundPrefData();
		}
		else // User has clicked Notification Button
		{
			super.showDialog(state);
		}
	}
}
