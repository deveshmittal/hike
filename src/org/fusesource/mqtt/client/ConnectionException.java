package org.fusesource.mqtt.client;

import org.fusesource.mqtt.codec.CONNACK.Code;

public class ConnectionException extends Throwable
	{

		private Code code;

		public ConnectionException(String message, Code code)
		{
			super(message);
			this.code = code;
		}

		public Code getCode()
		{
			return code;
		}
	}