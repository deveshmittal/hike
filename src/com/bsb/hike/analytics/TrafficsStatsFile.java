package com.bsb.hike.analytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.bsb.hike.utils.Logger;

public class TrafficsStatsFile
{

	private static final String LOGGING_TAG = TrafficsStatsFile.class.getSimpleName();

	private static final int EXCEPTION_READING_FILES = -1;
	
	private static final int UID_DIRECTORY_DOES_NOT_EXISTS = -2;
	
	private static final int FILES_NOT_READABLE = -3;
	
	public static long getTotalBytesManual(int localUid)
	{

		File uidFileDir = new File("/proc/uid_stat/" + String.valueOf(localUid));
		if(uidFileDir.exists())
		{
			try
			{
				File uidActualTCPFileReceived = new File(uidFileDir, "tcp_rcv");
				File uidActualTCPFileSent = new File(uidFileDir, "tcp_snd");
				File uidActualUDPFileReceived = new File(uidFileDir, "udp_rcv");
				File uidActualUDPFileSent = new File(uidFileDir, "udp_snd");
				
				if(!uidActualTCPFileReceived.canRead() && !uidActualUDPFileReceived.canRead()
						&& !uidActualTCPFileSent.canRead() && !uidActualUDPFileSent.canRead())
				{
					return FILES_NOT_READABLE;
				}
				
				String textReceived = "0";
				
				String textSent = "0";

				BufferedReader brReceivedTcp = new BufferedReader(new FileReader(uidActualTCPFileReceived));
				BufferedReader brSentTcp = new BufferedReader(new FileReader(uidActualTCPFileSent));
				BufferedReader brReceivedUdp = new BufferedReader(new FileReader(uidActualUDPFileReceived));
				BufferedReader brSentUdp = new BufferedReader(new FileReader(uidActualUDPFileSent));
				String receivedLine;
				String sentLine;
				long recvBytes = 0;
				long sentBytes = 0;
				
				if ((receivedLine = brReceivedTcp.readLine()) != null)
				{
					recvBytes = Long.valueOf(receivedLine);
				}
				if ((receivedLine = brReceivedUdp.readLine()) != null)
				{
					recvBytes += Long.valueOf(receivedLine);
				}
				if ((sentLine = brSentTcp.readLine()) != null)
				{
					sentBytes = Long.valueOf(sentLine);
				}
				if ((sentLine = brSentUdp.readLine()) != null)
				{
					sentBytes += Long.valueOf(sentLine);
				}
				return (sentBytes/1024) + (recvBytes/1024);
			}
			catch(NumberFormatException nfe)
			{
				nfe.printStackTrace();
				Logger.w(LOGGING_TAG, "Number not long: ");
			}
			catch (FileNotFoundException fnfex)
			{
				fnfex.printStackTrace();
				Logger.w(LOGGING_TAG, "File not found: ");
			}
			catch (IOException ioex)
			{
				ioex.printStackTrace();
				Logger.w(LOGGING_TAG, "IOException: ");
			}
			return EXCEPTION_READING_FILES;

		}
		else
		{
			return UID_DIRECTORY_DOES_NOT_EXISTS;
		}
	}

}
