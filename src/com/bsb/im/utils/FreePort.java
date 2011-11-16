package com.bsb.im.utils;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Utility class to get a free port.
 */
public final class FreePort {

    private static final int MAGIC_10 = 10;
    private static final int MAGIC_10000 = 10000;

    /**
     * Private default constructor.
     */
    private FreePort() {
    }

    /**
     * return a free port.
     * @return free socket port.
     */
    public static int getFreePort() {
	ServerSocket ss;
	int freePort = 0;

	for (int i = 0; i < MAGIC_10; i++) {
	    freePort = (int) (MAGIC_10000 + Math.round(Math.random() * MAGIC_10000));
	    try {
		ss = new ServerSocket(freePort);
		freePort = ss.getLocalPort();
		ss.close();
		return freePort;
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
	try {
	    ss = new ServerSocket(0);
	    freePort = ss.getLocalPort();
	    ss.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return freePort;
    }
}
