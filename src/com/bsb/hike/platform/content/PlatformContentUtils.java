package com.bsb.hike.platform.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.res.AssetManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class PlatformContentUtils
{
	public static void copyFile(InputStream in, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1)
		{
			out.write(buffer, 0, read);
		}
	}

	public static File streamToTempFile(InputStream in, String prefix, String suffix)
	{
		File tempFile = null;
		try
		{
			tempFile = File.createTempFile(prefix, suffix);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		tempFile.deleteOnExit();

		FileOutputStream out = null;

		try
		{
			out = new FileOutputStream(tempFile);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		try
		{
			copyFile(in, out);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return tempFile;
	}

	public static ParcelFileDescriptor openFile(Uri uri, String mode)
	{

		Log.d("FileContentProvider", "fetching: " + uri);

		ParcelFileDescriptor parcel = null;

		String fileNameRequested = uri.getLastPathSegment();
		String[] name = fileNameRequested.split("\\.");
		String prefix = name[0];
		String suffix = name[1];

		InputStream is = null;
		try
		{
			String filePath = uri.toString().replace(PlatformContentConstants.CONTENT_AUTHORITY_BASE, PlatformContentConstants.PLATFORM_CONTENT_DIR);

			Log.d("FileContentProvider", "FILE PATH: " + filePath);

			is = new FileInputStream(filePath);
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}

		File file = PlatformContentUtils.streamToTempFile(is, prefix, suffix);

		try
		{
			parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
		}
		catch (FileNotFoundException e)
		{
			Log.e("FileContentProvider", "uri " + uri.toString(), e);
		}
		return parcel;
	}

	public static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath)
	{
		InputStream in = null;
		OutputStream out = null;
		try
		{
			in = assetManager.open(fromAssetPath);
			new File(toPath).createNewFile();
			out = new FileOutputStream(toPath);
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
