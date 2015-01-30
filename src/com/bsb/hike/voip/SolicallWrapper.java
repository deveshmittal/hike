package com.bsb.hike.voip;

import com.bsb.hike.utils.Logger;

public class SolicallWrapper {
	
	private native int packageInit();
	private native int AECInit();
	private native int processSpeakerFrame(byte[] stream);
	private native int processMicFrame(byte[] input, byte[] output, int start);
	private native int terminate();
	
	private final int FRAME_SIZE = 4;	// 4 ms
	private final int FRAME_MULTIPLIER = 5;		// We want to pass 20ms of data
	private final int SPEAKER_BUFFER_SIZE = 1024 * 100;
	private final int MIC_BUFFER_SIZE = SPEAKER_BUFFER_SIZE;

	static {
		System.loadLibrary("solicall");
	}
	
	public void init() {
		int init = packageInit();
		Logger.d(VoIPConstants.TAG, "AEC packageInit: " + init);

		init = AECInit();
		Logger.d(VoIPConstants.TAG, "AEC init: " + init);
	}
	
	public void destroy() {
		int ret = terminate();
		Logger.d(VoIPConstants.TAG, "AEC terminate: " + ret);
	}
	
	public void processSpeaker(byte[] frame) {
		int ret = 0;

		synchronized (this) {
			ret = processSpeakerFrame(frame);
		}
//		Logger.d(VoIPConstants.TAG, "AEC processSpeaker, size: " + frame.length + ", ret: " + ret);
	}
	
	public int processMic(byte[] frame) {
//		Logger.d(VoIPConstants.TAG, "AEC mic sending: " + frame.length);
		int ret = 0;
		for (int i = 0; i < frame.length; i += OpusWrapper.OPUS_FRAME_SIZE * 2) {
//			byte[] newFrame = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];
//			System.arraycopy(frame, i, newFrame, 0, newFrame.length);
			synchronized (this) {
//				Logger.d(VoIPConstants.TAG, "AEC mic sending: " + i);
				ret = processMicFrame(frame, null, i);
			}
//			System.arraycopy(newFrame, 0, frame, i, newFrame.length);
		}
		return ret;
	}
}
