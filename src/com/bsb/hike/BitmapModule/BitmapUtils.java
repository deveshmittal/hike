package com.bsb.hike.BitmapModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;

public class BitmapUtils
{
	/**
	 * Returns aspect ratio of bitmap (Width to Height ratio)
	 */
	public static float getAspectRatioFromFile(String filename)
	{
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);
		return (float) (options.outWidth) / options.outHeight;
	}
	
	public static boolean isThumbnailSquare(Bitmap thumbnail)
	{
		return (thumbnail.getWidth() == thumbnail.getHeight());
	}

	public static byte[] bitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat format)
	{
		return bitmapToBytes(bitmap, format, 50);
	}

	public static byte[] bitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat format, int quality)
	{
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		bitmap.compress(format, quality, bao);
		return bao.toByteArray();
	}

	private static int iconHash(String s)
	{
		/*
		 * ignore everything after :: so that your large icon by default matches your msisdn
		 */
		s = s.split("::")[0];
		int count = 0;
		for (int i = 0; i < s.length(); ++i)
		{
			count += s.charAt(i);
		}

		return count;
	}

	public static int getDefaultAvatarResourceId(String msisdn, boolean rounded)
	{
		int count = 5;
		int id;
		switch (iconHash(msisdn) % count)
		{
		case 0:
			id = rounded ? R.drawable.avatar_01_rounded : R.drawable.avatar_01;
			break;
		case 1:
			id = rounded ? R.drawable.avatar_02_rounded : R.drawable.avatar_02;
			break;
		case 2:
			id = rounded ? R.drawable.avatar_03_rounded : R.drawable.avatar_03;
			break;
		case 3:
			id = rounded ? R.drawable.avatar_04_rounded : R.drawable.avatar_04;
			break;
		case 4:
			id = rounded ? R.drawable.avatar_05_rounded : R.drawable.avatar_05;
			break;
		default:
			id = rounded ? R.drawable.avatar_01_rounded : R.drawable.avatar_01;
			break;
		}

		return id;
	}

	public static void saveBitmapToFile(File file, Bitmap bitmap) throws IOException
	{
		saveBitmapToFile(file, bitmap, CompressFormat.PNG, 70);
	}

	public static void saveBitmapToFile(File file, Bitmap bitmap, CompressFormat compressFormat, int quality) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(file);

		byte[] b = BitmapUtils.bitmapToBytes(bitmap, compressFormat, quality);
		if (b == null)
		{
			throw new IOException();
		}
		fos.write(b);
		fos.flush();
		fos.close();
	}

	public static int getBitmapSize(Bitmap bitmap)
	{
		if (bitmap == null)
			return 0;
		// From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
		// larger than bitmap byte count.
		if (Utils.hasKitKat())
		{
			return bitmap.getAllocationByteCount();
		}

		if (Utils.hasHoneycombMR1())
		{
			return bitmap.getByteCount();
		}

		// Pre HC-MR1
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	public static byte[] getRoundedBitmapBytes(byte[] data)
	{

		Bitmap tempBitmap = HikeBitmapFactory.decodeByteArray(data, 0, data.length);
		Bitmap roundedBitmap = HikeBitmapFactory.getCircularBitmap(tempBitmap);

		try
		{
			return bitmapToBytes(roundedBitmap, Bitmap.CompressFormat.PNG);
		}
		finally
		{
			tempBitmap.recycle();
			roundedBitmap.recycle();
		}
	}

}
