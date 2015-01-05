package com.bsb.hike.view;

import java.util.LinkedHashMap;
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

	private Map<String, Uri> ringtonesNameURIMap;

	private Cursor notifSoundCursor;

	private static int HIKE_JINNGLE_INDEX = 2;
	
	private RingtoneFetcherTask fetcherTask;
	
	public NotificationToneListPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
		this.ringtonesNameURIMap = new LinkedHashMap<String, Uri>();
		setIcon(context, attrs);
		this.setValueIndex(HIKE_JINNGLE_INDEX);
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
		setEntryAndValues();
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
		Object newValue = this.getEntryValues()[mClickedDialogEntryIndex];
		String selectedNotificationTone = newValue.toString();
		if (mContext.getString(R.string.notif_sound_off).equals(selectedNotificationTone))
		{
			// Here No Sound is played
			return;
		}
		else if (mContext.getString(R.string.notif_sound_Hike).equals(selectedNotificationTone))
		{
			Utils.playSoundFromRaw(mContext, R.raw.hike_jingle_15);
		}
		else if (mContext.getString(R.string.notif_sound_default).equals(selectedNotificationTone))
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
			ringtonesNameURIMap.clear();
			dialog.dismiss();
			break;

		default:
			break;
		}
	}

	/**
	 * Sets entries and Values for SoundListPref via AsyncTask
	 */
	public void fetchSoundPrefData()
	{
		fetcherTask = new RingtoneFetcherTask();
		fetcherTask.execute();
	}

	private void initRingtoneLists()
	{
		ringtonesNameURIMap.put(mContext.getResources().getString(R.string.notif_sound_off), null);
		ringtonesNameURIMap.put(mContext.getResources().getString(R.string.notif_sound_default), null);
		ringtonesNameURIMap.put(mContext.getResources().getString(R.string.notif_sound_Hike), null);
	}

	private void setEntryAndValues()
	{
		CharSequence[] rintoneCharSeq = ringtonesNameURIMap.keySet().toArray(new CharSequence[ringtonesNameURIMap.size()]);
		setEntries(rintoneCharSeq);
		setEntryValues(rintoneCharSeq);
	}

	/**
	 * Handles Click of Notification Option in Preferences Screen
	 */
	@Override
	protected void onClick()
	{
		//To avoid opening of multiple Dialogs 
		this.setEnabled(false);
				
		fetchSoundPrefData();
		
	}

	@Override
	protected void showDialog(Bundle state)
	{
		// It is the case of screen rotation
		if (fetcherTask == null)
		{
			fetchSoundPrefData();
		}
		else // User has clicked Notification Button
		{
			super.showDialog(state);
		}
	}
	
	/**
	 * 
	 * Fetches Available Ringtones from Device
	 *
	 */
	private class RingtoneFetcherTask extends AsyncTask<Void, Void, Void>{

		protected void onPreExecute()
		{
			initRingtoneLists();
		};

		@Override
		protected Void doInBackground(Void... params)
		{
			if (!fetcherTask.isCancelled())
			{
				try
				{
					RingtoneManager ringtoneMgr = new RingtoneManager(mContext);
					ringtoneMgr.setType(RingtoneManager.TYPE_NOTIFICATION);
					notifSoundCursor = ringtoneMgr.getCursor();
					if (notifSoundCursor != null)
					{
						int notifSoundCount = notifSoundCursor.getCount();
						if (notifSoundCount != 0 && notifSoundCursor.moveToFirst())
						{
							String ringtoneName = "";
							while (!notifSoundCursor.isAfterLast() && notifSoundCursor.moveToNext())
							{
								int currentPosition = notifSoundCursor.getPosition();
								ringtoneName = ringtoneMgr.getRingtone(currentPosition).getTitle(mContext);
								ringtonesNameURIMap.put(ringtoneName, ringtoneMgr.getRingtoneUri(currentPosition));
							}
						}
						else
						{
							//No Notification Tone was present in device
						}
						
						//The returned cursor will be the same cursor returned each time this method is called, 
						//so do not Cursor.close() the cursor. The cursor can be Cursor.deactivate() safely.
						notifSoundCursor.deactivate();
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			return null;
		}

		protected void onPostExecute(Void result)
		{
			setEntryAndValues();
			notifyChanged();
			
			showDialog(null);
		};
		
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		super.onDismiss(dialog);
		if(!fetcherTask.isCancelled())
		{
			fetcherTask.cancel(true);
		}
		this.setEnabled(true);
	}
}
