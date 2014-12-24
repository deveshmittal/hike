package com.bsb.hike.models;

public class NuxCustomMessage
{
	private String screentitle;

	private String smsmessage;

	private String buttext;

	private boolean togglecustommsg;

	public NuxCustomMessage(String screentitle, String smsmessage, String buttext, boolean togglecustommsg)
	{
		super();
		this.screentitle = screentitle;
		this.smsmessage = smsmessage;
		this.buttext = buttext;
		this.togglecustommsg = togglecustommsg;
	}

	/**
	 * @return the screentitle
	 */
	public String getScreenTitle()
	{
		return screentitle;
	}

	/**
	 * @return the smsmessage
	 */
	public String getSmsMessage()
	{
		return smsmessage;
	}

	/**
	 * @return the buttext
	 */
	public String getButText()
	{
		return buttext;
	}

	/**
	 * @return the togglecustommsg
	 */
	public boolean isToggleCustomMsg()
	{
		return togglecustommsg;
	}

}
