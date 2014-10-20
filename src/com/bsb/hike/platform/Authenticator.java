package com.bsb.hike.platform;

public class Authenticator
{

	private static Authenticator authenticator;

	private Authenticator()
	{

	}

	public static Authenticator getInstance()
	{
		if (authenticator == null)
		{
			return new Authenticator();
		}

		return authenticator;
	}

	public boolean isTokenValid(String authToken)
	{

		return true;

	}
}
