package com.bsb.hike.thor.crypt;

import java.util.concurrent.CountDownLatch;

import org.bouncycastle.crypto.BlockCipher;

public class ECBBlockCipherpar implements Runnable
{
	private int blockSize;

	private BlockCipher cipher = null;

	private byte[] in, out;

	private int[] s;

	private CountDownLatch latch;

	public ECBBlockCipherpar(CountDownLatch latch, BlockCipher cipher, byte[] in, byte[] out, int[] s)
	{
		// TODO Auto-generated constructor stub
		this.cipher = cipher;
		this.blockSize = cipher.getBlockSize();
		this.in = in;
		this.out = out;
		this.latch = latch;
		this.s = s;
	}

	public void run()
	{
		// TODO Auto-generated method stub
		while (s[3] > 0 && s[2] > 0)
		{
			cipher.processBlock(in, s[0], out, s[1]);
			s[2] -= (blockSize);
			s[0] += (blockSize);
			s[1] += (blockSize);
			s[3] -= 1;
		}
		latch.countDown();
	}
}
