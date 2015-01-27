package com.bsb.hike.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;

public class CustomLogger
{

	
	public CustomLogger()
	{
		// TODO Auto-generated constructor stub
	}
	
	public static void writeLog(Context context, String line)
	{
		File dir = context.getExternalFilesDir(null);
		if (dir == null)
		{
			return;
		}
		File file = new File(dir, "custom_logs" + ".txt");
		FileOutputStream fos = null;

		try
		{
			fos = new FileOutputStream(file, true);
			line += "\n";
			fos.write(line.getBytes());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (fos != null)
			{
					try
					{
						fos.flush();
						fos.close();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}
	}
}
