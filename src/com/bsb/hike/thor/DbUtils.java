package com.bsb.hike.thor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class DbUtils
{
	private static final String TAG = "DecryptDbUtils";

	public static long getOldTs(int days)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		long daysAgo = cal.getTimeInMillis();
		return daysAgo;
	}

	public static Timestamp daysbeforetime(int days)
	{
		java.util.Calendar cal = java.util.Calendar.getInstance();
		java.util.Date utilDate = cal.getTime();
		Date datebefore = new Date(utilDate.getTime() - ((long) (days * 24)) * 3600 * 1000);
		return (new Timestamp(datebefore.getTime()));
	}

	public static void getTables(Connection con) throws SQLException
	{
		DatabaseMetaData md = con.getMetaData();
		ResultSet rs = md.getTables(null, null, "%", null);
		//Logger.d("TABLES", "Printing tables .....");
		while (rs.next())
		{
			//Logger.d("TABLES", rs.getString(3));
		}
	}

	public static List<String> getConvs(Connection con)
	{
		List<String> convs = null;
		PreparedStatement pt = null;
		ResultSet msg = null;
		try
		{
			String query = "SELECT key_remote_jid FROM chat_list";
			pt = con.prepareStatement(query);
			msg = pt.executeQuery();
			convs = new LinkedList<String>();
			while (msg.next())
			{
				try
				{
					String conv = msg.getString("key_remote_jid");
					if (!conv.contains("-")) // this is 1-1 chat
						convs.add(conv);
				}
				catch (Exception e)
				{

				}
			}
		}
		catch (SQLException e)
		{
		}
		catch (Exception e)
		{
		}
		finally
		{
			close(pt, msg);
		}
		return convs;
	}

	public static void calculateScore(HashMap<String, Integer> freq, Connection con, String key_remote_jid, long ts)
	{
		ResultSet msg = null;
		PreparedStatement pt = null;
		try
		{
			String query = "SELECT count(*) as cc FROM messages where key_remote_jid = ? AND timestamp > ?";
			pt = con.prepareStatement(query);
			pt.setString(1, key_remote_jid);
			pt.setLong(2, ts);
			msg = pt.executeQuery();
			while (msg.next())
			{
				int count = msg.getInt("cc");
				if(count > 0)
					freq.put(key_remote_jid, count);
			}
		}
		catch (SQLException e)
		{
		}
		catch (Exception e)
		{
		}
		finally
		{
			close(pt, msg);
		}
	}

	private static void close(PreparedStatement pt, ResultSet msg)
	{
		if (msg != null)
			try
			{
				msg.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		if (pt != null)
			try
			{
				pt.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
	}
}
