package com.bsb.hike.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.Sticker;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class ShareUtils
{
	static Intent intent;

	static Context mContext = HikeMessengerApp.getInstance().getApplicationContext();
    
	static String imgHead, imgDesc, imgCap, stiHead, stiDesc, stiCap, textHead, textCap ; 
	
	public static Intent shareContent(int type, Object obj)
	{ 
		imgHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_HEADING, mContext.getString(R.string.image_share_heading));
    	imgDesc = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_DESCRIPTION, mContext.getString(R.string.image_share_description));
	    imgCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_CAPTION, mContext.getString(R.string.image_share_caption));
	    stiHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.STICKER_HEADING, mContext.getString(R.string.sticker_share_heading));
	    stiDesc = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.STICKER_DESCRIPTION, mContext.getString(R.string.sticker_share_description));
	    stiCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.STICKER_CAPTION, mContext.getString(R.string.sticker_share_caption));
	    textHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.TEXT_HEADING, mContext.getString(R.string.text_share_heading));
	    textCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.TEXT_CAPTION, mContext.getString(R.string.text_share_caption));
	  
	    String path = (String) obj;
		
		switch (type)
		{
		
		case HikeConstants.Extras.ShareTypes.STICKER_SHARE:
			intent = stickerShare(path, HikeConstants.Extras.ShareTypes.STICKER_SHARE);
			break;

		case HikeConstants.Extras.ShareTypes.IMAGE_SHARE:
			intent = imageShare(path);
			break;

		case HikeConstants.Extras.ShareTypes.TEXT_SHARE:
			intent = textShare(path);
			break;

		/* STICKER SHARED FROM THE PALLETE AREA TO ANY APP */
		case HikeConstants.Extras.ShareTypes.STICKER_SHARE_PALLETE:
			intent = stickerShare(path, HikeConstants.Extras.ShareTypes.STICKER_SHARE_PALLETE);
			break;
		}

		return intent;

	}
	
	private static void deleteFile(File toDelete)
	{
		if (toDelete != null && toDelete.exists())
		{
			toDelete.deleteOnExit();
		}
		
	}
	
	private static void closeFos(FileOutputStream fos)
	{
		if (fos != null)
		{
			try
			{
				fos.close();
			}
			catch (IOException e)
			{
				// Do nothing
				e.printStackTrace();
			}
		}
		
	}

	private static View setViewImage(String filePath)
	{

	    View share = LayoutInflater.from(mContext).inflate(R.layout.image_share_layout, null);
		ImageView image = (ImageView) share.findViewById(R.id.user_image);
		Bitmap bmp = HikeBitmapFactory.decodeFile(filePath);
		image.setImageBitmap(bmp);
		TextView heading = (TextView) share.findViewById(R.id.imageShareHeading);
		heading.setText(imgHead);
		TextView tv = (TextView) share.findViewById(R.id.imageShareDescription);
		tv.setText(Html.fromHtml(imgDesc));

		return share;
	}

	private static Intent imageShare(String imagePath)
	{
		Logger.d("ShareUtils", "imageShareMethod");
		File shareImageFile = null;
		FileOutputStream fos = null;
		Intent imageIntent;
		try
		{
			shareImageFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
			fos = new FileOutputStream(shareImageFile);
			View share = setViewImage(imagePath);
			Bitmap shB = Utils.undrawnViewToBitmap(share);
			
			shB.compress(CompressFormat.JPEG, 100, fos);
			 
			fos.flush();
			imageIntent = IntentManager.shareIntent("image/jpeg", "file://" + shareImageFile.getAbsolutePath(), imgCap , HikeConstants.Extras.ShareTypes.IMAGE_SHARE, true);
			return imageIntent;
		}

		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(mContext, mContext.getString(com.bsb.hike.R.string.error_sharing), Toast.LENGTH_SHORT).show();
			return null;
		}
		finally
		{
			closeFos(fos);
			deleteFile(shareImageFile);		
		}
	}

	private static Intent textShare(String text)
	{
		Logger.d("ShareUtils", "smsShare");
		text = text + "\n\n" + textHead + "\n" + textCap ;
		Intent textIntent = IntentManager.shareIntent("text/plain", null, text, HikeConstants.Extras.ShareTypes.TEXT_SHARE, true);
		return textIntent;

	}

	
	
	private static View setViewSticker(String stickerFilePath)
	{   
		View share = LayoutInflater.from(mContext).inflate(R.layout.sticker_share_layout, null);
		ImageView image = (ImageView) share.findViewById(R.id.sticker_image);
		
		Bitmap bmp = BitmapFactory.decodeFile(stickerFilePath);
		image.setImageBitmap(bmp);
		
		TextView heading = (TextView) share.findViewById(R.id.stickerShareHeading);
		heading.setText(stiHead);

		TextView tv = (TextView) share.findViewById(com.bsb.hike.R.id.stickerShareDescription);
		tv.setText(Html.fromHtml(stiDesc));

		return share;
	}

	private static Intent stickerShare(String stickerPath, int type)
	{
		Logger.d("ShareUtils", "stickerShare");
		File shareStickerFile = null;
		FileOutputStream fos = null;
		Intent stickerIntent;
		try
		{
			shareStickerFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
			fos = new FileOutputStream(shareStickerFile);

			View share = setViewSticker(stickerPath);

			Bitmap shB = Utils.undrawnViewToBitmap(share);
			shB.compress(CompressFormat.JPEG, 100, fos);

			fos.flush();
			if (type == HikeConstants.Extras.ShareTypes.STICKER_SHARE_PALLETE)
			{
				stickerIntent = IntentManager.shareIntent("image/jpeg", "file://" + shareStickerFile.getAbsolutePath(), stiCap, HikeConstants.Extras.ShareTypes.STICKER_SHARE_PALLETE, false);
			}
			else
			{
				stickerIntent = IntentManager.shareIntent("image/jpeg", "file://" + shareStickerFile.getAbsolutePath(), stiCap , HikeConstants.Extras.ShareTypes.STICKER_SHARE, true);
			}
			return stickerIntent;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		finally
		{
			    closeFos(fos);
				deleteFile(shareStickerFile);
			
		}

	}
	
	
	
	

}