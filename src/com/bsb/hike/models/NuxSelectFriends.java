package com.bsb.hike.models;

import java.util.HashSet;

public class NuxSelectFriends
{

	private String sectiontitle;

	private String recosectiontitle;

	private boolean togglerecosection;

	private HashSet<String> recolist;

	private boolean togglecontactsection;

	private String buttext;

	private String progresstext;

	private boolean searchtoggle;

	public NuxSelectFriends(String sectiontitle, String recosectiontitle, boolean togglerecosection, HashSet<String> recolist, boolean togglecontactsection, String buttext,
			String progresstext, boolean searchtoggle)
	{
		super();
		this.sectiontitle = sectiontitle;
		this.recosectiontitle = recosectiontitle;
		this.togglerecosection = togglerecosection;
		this.recolist = recolist;
		this.togglecontactsection = togglecontactsection;
		this.buttext = buttext;
		this.progresstext = progresstext;
		this.searchtoggle = searchtoggle;
	}

	/**
	 * @return the sectiontitle
	 */
	public String getSectiontitle()
	{
		return sectiontitle;
	}

	/**
	 * @return the recosectiontitle
	 */
	public String getRecosectiontitle()
	{
		return recosectiontitle;
	}

	/**
	 * @return the togglerecosection
	 */
	public boolean isTogglerecosection()
	{
		return togglerecosection;
	}

	/**
	 * @return the recolist
	 */
	public HashSet<String> getRecoList()
	{
		return recolist;
	}

	/**
	 * @return the togglecontactsection
	 */
	public boolean isToggleContactSection()
	{
		return togglecontactsection;
	}

	/**
	 * @return the buttext
	 */
	public String getButText()
	{
		return buttext;
	}

	/**
	 * @return the progresstext
	 */
	public String getProgressText()
	{
		return progresstext;
	}

	/**
	 * @return the searchtoggle
	 */
	public boolean isSearchToggle()
	{
		return searchtoggle;
	}

}
