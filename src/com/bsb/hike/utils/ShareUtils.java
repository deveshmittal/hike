package com.bsb.hike.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
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

public class ShareUtils
{
	static Intent intent;

	static Context mContext = HikeMessengerApp.getInstance().getApplicationContext();

	public static Intent shareContent(int type, Object obj)
	{
		switch (type)
		{
		case HikeConstants.Extras.ShareTypes.STICKER_SHARE:
			Sticker sticker = (Sticker) obj;
			intent = stickerShare(sticker, HikeConstants.Extras.ShareTypes.STICKER_SHARE);
			break;

		case HikeConstants.Extras.ShareTypes.IMAGE_SHARE:
			String imagePath = (String) obj;
			intent = imageShare(imagePath);
			break;

		case HikeConstants.Extras.ShareTypes.SMS_SHARE:
			String sms = (String) obj;
			intent = smsShare(sms);
			break;
            
			/*STICKER SHARED FROM THE PALLETE AREA TO ANY APP*/
		case HikeConstants.Extras.ShareTypes.STICKER_SHARE_PALLETE:
			Sticker stickerpallete = (Sticker) obj;
			intent = stickerShare(stickerpallete, HikeConstants.Extras.ShareTypes.STICKER_SHARE_PALLETE);
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
		heading.setText(R.string.imageShareHeading);
		TextView tv = (TextView) share.findViewById(R.id.imageShareDescription);
		tv.setText(Html.fromHtml(mContext.getString(com.bsb.hike.R.string.imageShareDescription)));

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
			imageIntent = IntentManager.shareImageIntentWhatsapp("image/jpeg", "file://" + shareImageFile.getAbsolutePath(), mContext.getString(R.string.downloadHike));
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

	private static Intent smsShare(String sms)
	{
		Logger.d("ShareUtils", "smsShare");
		sms = sms + "\n\n" + mContext.getResources().getString(R.string.smsShareHeading) + "\n" + mContext.getResources().getString(R.string.smsShareDescription);
		Intent smsIntent = IntentManager.shareSmsIntentWhatsapp(sms);
		return smsIntent;

	}

	
	
	private static View setViewSticker(Sticker sticker)
	{   
		View share = LayoutInflater.from(mContext).inflate(R.layout.sticker_share_layout, null);
		ImageView image = (ImageView) share.findViewById(R.id.sticker_image);
		String filePath = StickerManager.getInstance().getStickerDirectoryForCategoryId(sticker.getCategoryId()) + HikeConstants.LARGE_STICKER_ROOT;
		File stickerFile = new File(filePath, sticker.getStickerId());
		if (stickerFile.exists())
		{
			Bitmap bmp = BitmapFactory.decodeFile(stickerFile.getAbsolutePath());
			image.setImageBitmap(bmp);
		}
		TextView heading = (TextView) share.findViewById(R.id.stickerShareHeading);
		heading.setText(R.string.stickerShareHeading);

		TextView tv = (TextView) share.findViewById(com.bsb.hike.R.id.stickerShareDescription);
		tv.setText(Html.fromHtml(mContext.getString(com.bsb.hike.R.string.stickerShareDescription)));

		return share;
	}

	private static Intent stickerShare(Sticker sticker, int type)
	{
		Logger.d("ShareUtils", "stickerShare");
		File shareStickerFile = null;
		FileOutputStream fos = null;
		Intent stickerIntent;
		try
		{
			shareStickerFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
			fos = new FileOutputStream(shareStickerFile);

			View share = setViewSticker(sticker);

			Bitmap shB = Utils.undrawnViewToBitmap(share);
			shB.compress(CompressFormat.JPEG, 100, fos);

			fos.flush();
			if (type == HikeConstants.Extras.ShareTypes.STICKER_SHARE_PALLETE)
			{
				stickerIntent = IntentManager.shareImageIntent("image/jpeg", "file://" + shareStickerFile.getAbsolutePath(), mContext.getString(R.string.downloadHike));
			}
			else
			{
				stickerIntent = IntentManager.shareImageIntentWhatsapp("image/jpeg", "file://" + shareStickerFile.getAbsolutePath(), mContext.getString(R.string.downloadHike));
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