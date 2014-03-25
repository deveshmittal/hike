package com.bsb.hike.thor.crypt;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;

public class BufferedBlockCipher2par extends BufferedBlockCipher
{
	public BufferedBlockCipher2par(BlockCipher cipher)
	{
		// TODO Auto-generated constructor stub
		super(cipher);
	}

	private int blockSize;

	public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException, IllegalStateException
	{
		if (len < 0)
		{
			throw new IllegalArgumentException("Can't have a negative input length!");
		}
		blockSize = getBlockSize();
		int length = getUpdateOutputSize(len);
		if (length > 0)
		{
			if ((outOff + length) > out.length)
			{
				throw new DataLengthException("output buffer too short");
			}
		}
		int resultLen = 0;
		if (len > 0)
		{
			int pcnt = Runtime.getRuntime().availableProcessors();
			int num, i, numThreads = pcnt;
			
			int[][] s = new int[numThreads][5];
			ExecutorService exec = Executors.newCachedThreadPool();
			CountDownLatch latch = new CountDownLatch(numThreads);
			num = (len / blockSize) / numThreads;
			int add = num * blockSize;
			s[0][0] = inOff;
			s[0][1] = outOff + resultLen;
			s[0][2] = len;
			s[0][3] = num;
			for (i = 1; i < numThreads; ++i)
			{
				s[i][0] = s[i - 1][0] + add;
				s[i][1] = s[i - 1][1] + add;
				s[i][2] = s[i - 1][2] - add;
				s[i][3] = num;
			}
			for (i = 0; i < numThreads; ++i)
				exec.execute(new ECBBlockCipherpar(latch, cipher, in, out, s[i]));
			exec.shutdown();
			try
			{
				latch.await();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			inOff = s[numThreads - 1][0];
			resultLen = s[numThreads - 1][1] - outOff;
			len = s[numThreads - 1][2];
		}
		while (len > 0)
		{
			cipher.processBlock(in, inOff, out, outOff + resultLen);
			len -= blockSize;
			inOff += blockSize;
			resultLen += blockSize;
		}
		return resultLen;
	}
}
