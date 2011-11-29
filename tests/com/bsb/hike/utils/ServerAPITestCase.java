package com.bsb.hike.utils;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

public class ServerAPITestCase extends TestCase {

	public void testGetMSISDN() {
		String msisdn = AccountUtils.getMSISDN();
		assertEquals(msisdn, "123456789");
	}

	public void testCreateAccount() throws UnsupportedEncodingException {
		AccountUtils.AccountInfo accountInfo = AccountUtils.registerAccount();
		assertNotNull(accountInfo.token);
	}
}
