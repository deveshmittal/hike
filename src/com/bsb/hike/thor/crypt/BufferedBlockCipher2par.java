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
		int gapLen = buf.length - bufOff;
		if (len > gapLen)
		{
			System.arraycopy(in, inOff, buf, bufOff, gapLen);
			resultLen += cipher.processBlock(buf, 0, out, outOff);
			bufOff = 0;
			len -= gapLen;
			inOff += gapLen;
			int pcnt = Runtime.getRuntime().availableProcessors();
			
			int num, i, num_threads = (pcnt*2)+1;
			
			int[][] s = new int[num_threads][5];
			
			ExecutorService exec = Executors.newCachedThreadPool();
			CountDownLatch latch = new CountDownLatch(num_threads);
			num = ((len - blockSize) / blockSize) / num_threads;
			int add = num * blockSize;
			s[0][0] = inOff;
			s[0][1] = outOff + resultLen;
			s[0][2] = len;
			s[0][3] = num;
			for (i = 1; i < num_threads; ++i)
			{
				s[i][0] = s[i - 1][0] + add;
				s[i][1] = s[i - 1][1] + add;
				s[i][2] = s[i - 1][2] - add;
				s[i][3] = num;
			}
			for (i = 0; i < num_threads; ++i)
			{
				exec.execute(new ECBBlockCipherpar(latch, cipher, in, out, s[i]));
			}
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
			inOff = s[num_threads-1][0];
			resultLen = s[num_threads-1][1] - outOff;
			len = s[num_threads-1][2];
		}
		while (len > buf.length)
		{
			resultLen += cipher.processBlock(in, inOff, out, outOff + resultLen);
			len -= blockSize;
			inOff += blockSize;
		}
		System.arraycopy(in, inOff, buf, bufOff, len);
		bufOff += len;
		if (bufOff == buf.length)
		{
			resultLen += cipher.processBlock(buf, 0, out, outOff + resultLen);
			bufOff = 0;
		}
		return resultLen;
	}
}
