package com.bsb.hike.filetransfer;

import java.io.Serializable;

import com.bsb.hike.filetransfer.FileTransferBase.FTState;

public class FileSavedState implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private FTState _currentState;

	private int _totalSize; // (in bytes)

	private int _transferredSize;
	
	private String _sessionId;

	public FileSavedState(FTState state, int totalSize, int transferredSize)
	{
		_currentState = state;
		_totalSize = totalSize;
		_transferredSize = transferredSize;
		_sessionId = null;
	}
	
	public FileSavedState(FTState state, int totalSize, int transferredSize,String sId)
	{
		_currentState = state;
		_totalSize = totalSize;
		_transferredSize = transferredSize;
		_sessionId = sId;
	}
	
	public FileSavedState()
	{
		_currentState = FTState.NOT_STARTED;
	}

	public int getTotalSize()
	{
		return _totalSize;
	}

	public int getTransferredSize()
	{
		return _transferredSize;
	}

	public FTState getFTState()
	{
		return _currentState;
	}
	
	public String getSessionId()
	{
		return _sessionId;
	}
}
