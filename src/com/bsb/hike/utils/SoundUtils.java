package com.bsb.hike.utils;

import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.bsb.hike.HikeConstants;

public class SoundUtils
{

	public static boolean isPlayTickSound(Context context)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getCallState() == TelephonyManager.CALL_STATE_IDLE && !am.isMusicActive()
				&& (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.TICK_SOUND_PREF, true));
	}

	/**
	 * we are using stream_ring so that use can control volume from mobile and this stream is not in use when user is chatting and vice-versa
	 * 
	 * @param context
	 * @param soundId
	 */
	public static void playSoundFromRaw(final Context context, int soundId)
	{

		Logger.i("sound", "playing sound " + soundId);
		MediaPlayer mp = new MediaPlayer();
		mp.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
		final int notifVol = getCurrentVolume(context, AudioManager.STREAM_NOTIFICATION);
		if(isAnyMusicPlaying(context))
		{
			setCurrentVolume(context, AudioManager.STREAM_NOTIFICATION, notifVol);
		}
		Resources res = context.getResources();
		AssetFileDescriptor afd = res.openRawResourceFd(soundId);

		try
		{
			mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			afd.close();

			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
			{

				@Override
				public void onCompletion(MediaPlayer mp)
				{
					mp.release();
					setCurrentVolume(context, AudioManager.STREAM_NOTIFICATION, notifVol);
				}
			});
			mp.prepare();
			mp.start();

		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			mp.release();
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
			mp.release();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			mp.release();
		}
	}

	public static void playDefaultNotificationSound(Context context)
	{
		try
		{
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(context, notification);
			r.play();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Plays non-ducking sound from given Uri. Plays on {@link android.Media.AudioManager#STREAM_SYSTEM AudioManager.STREAM_SYSTEM} to enable non-ducking playback.
	 * 
	 * @param context
	 * @param soundUri
	 */
	public static void playSound(final Context context, Uri soundUri)
	{
		MediaPlayer mp = new MediaPlayer();
		mp.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
		final int notifVol = getCurrentVolume(context, AudioManager.STREAM_NOTIFICATION);
		if(isAnyMusicPlaying(context))
		{
			setCurrentVolume(context, AudioManager.STREAM_NOTIFICATION, notifVol);
		}
		try
		{
			mp.setDataSource(context, soundUri);

			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
			{

				@Override
				public void onCompletion(MediaPlayer mp)
				{
					mp.release();
					setCurrentVolume(context, AudioManager.STREAM_NOTIFICATION, notifVol);
				}
			});
			mp.prepare();
			mp.start();

		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			mp.release();
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
			mp.release();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			mp.release();
		}
	}
	
	public static int getCurrentVolume(Context context, int streamType)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return am.getStreamVolume(streamType);
	}
	
	public static void setCurrentVolume(Context context, int streamType, int vol)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(streamType, vol, AudioManager.ADJUST_SAME);
	}
	
	public static boolean isAnyMusicPlaying(Context context)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return am.isMusicActive();
	}
}
