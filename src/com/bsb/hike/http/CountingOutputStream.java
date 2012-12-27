package com.bsb.hike.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

import com.bsb.hike.utils.ProgressListener;

public class CountingOutputStream extends FilterOutputStream {

	private final ProgressListener listener;
	private long transferred;

	public CountingOutputStream(final OutputStream out,
			final ProgressListener listener) {
		super(out);
		this.listener = listener;
		this.transferred = 0;
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		out.flush();
		this.transferred += len;
		this.listener.transferred(this.transferred);
	}

	public void write(int b) throws IOException {
		out.write(b);
		out.flush();
		this.transferred++;
		this.listener.transferred(this.transferred);
	}

	public void cancel() {
		try {
			out.close();
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), "Error while closing stream");
		}
	}
}