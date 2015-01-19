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
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.bsb.hike.HikeConstants;

public class SoundUtils
{
	private static int systemStreamVol;
	
	private static Context mContext;

	private static Handler soundHandler = new Handler(Looper.getMainLooper());

	private static MediaPlayer mediaPlayer = new MediaPlayer();

	private static Runnable stopSoundRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			mediaPlayer.reset();
			setCurrentVolume(mContext, AudioManager.STREAM_SYSTEM, systemStreamVol);
		}
	};

	private static MediaPlayer.OnCompletionListener completeListener = new MediaPlayer.OnCompletionListener()
	{

		@Override
		public void onCompletion(MediaPlayer mp)
		{
			mediaPlayer.reset();
			soundHandler.removeCallbacks(stopSoundRunnable);
			setCurrentVolume(mContext, AudioManager.STREAM_SYSTEM, systemStreamVol);
		}
	};

	private static MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener()
	{

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra)
		{
			Logger.e("SoundUtils", "MediaPlayer -- OnERROR!!! WHAT:: " + what + " EXTRAS:: " + extra);
			// This is being removed as onError and on IOEx was called together so accessing 
			// stopMediaPlayerProperly at same time causing NPE
			//stopMediaPlayerProperly();
			return true;
		}
	};

	private static Runnable resetSystemStreamVolRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			setCurrentVolume(mContext, AudioManager.STREAM_SYSTEM, systemStreamVol);
		}
	};
	
	public static boolean isTickSoundEnabled(Context context)
	{
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.TICK_SOUND_PREF, true) && tm.getCallState() == TelephonyManager.CALL_STATE_IDLE && !isAnyMusicPlaying(context));
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

		// Initializing Player if it has been killed by onErrorListener
		if (mediaPlayer == null)
		{
			mediaPlayer = new MediaPlayer();
		}
		else
		{
			// resetting media player
			mediaPlayer.reset();
		}

		mContext = context;
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_SYSTEM);
		setVolToSystemStreamVol(context);
		Resources res = context.getResources();
		AssetFileDescriptor afd = res.openRawResourceFd(soundId);

		try
		{
			mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			afd.close();

			mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
			{

				@Override
				public void onCompletion(MediaPlayer mp)
				{
					mp.reset();
				}
			});
			mediaPlayer.setOnErrorListener(errorListener);
			mediaPlayer.prepare();
			mediaPlayer.start();

		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
	}

	public static void playDefaultNotificationSound(Context context)
	{
		try
		{
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(context, notification);
			r.setStreamType(AudioManager.STREAM_SYSTEM);
			mContext = context;
			setVolToSystemStreamVol(context);
			r.play();
			soundHandler.postDelayed(resetSystemStreamVolRunnable, HikeConstants.STOP_NOTIF_SOUND_TIME);
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
		// remove any previous handler
		soundHandler.removeCallbacks(stopSoundRunnable);

		// Initializing Player if it has been killed by onErrorListener
		if (mediaPlayer == null)
		{
			mediaPlayer = new MediaPlayer();
		}
		else
		{
			// resetting media player
			mediaPlayer.reset();
		}

		mediaPlayer.setAudioStreamType(AudioManager.STREAM_SYSTEM);
		mContext = context;
		setVolToSystemStreamVol(context);
		
		try
		{
			mediaPlayer.setDataSource(context, soundUri);

			mediaPlayer.setOnCompletionListener(completeListener);
			mediaPlayer.setOnErrorListener(errorListener);
			mediaPlayer.prepare();
			mediaPlayer.start();
			soundHandler.postDelayed(stopSoundRunnable, HikeConstants.STOP_NOTIF_SOUND_TIME);

		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
	}

	public static void setVolToSystemStreamVol(Context context)
	{
		systemStreamVol = getCurrentVolume(context, AudioManager.STREAM_SYSTEM);
		int notifVol = getCurrentVolume(context, AudioManager.STREAM_NOTIFICATION);
		setCurrentVolume(context, AudioManager.STREAM_SYSTEM, notifVol);
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

	private static void stopMediaPlayerProperly()
	{
		soundHandler.removeCallbacks(stopSoundRunnable);

		// Add NULL check here because media player, on throwing exception calls this method again.
		// Which results in stopMediaPlayerProperly() method to be called twice.
		if (mediaPlayer != null)
		{
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}
}
