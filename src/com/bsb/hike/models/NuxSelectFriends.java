package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.Set;

public class NuxSelectFriends
{

	private String sectionTitle;

	private String title2;

	/**
	 * @param sectionTitle
	 * @param title2
	 * @param title3
	 * @param recoSectionTitle
	 * @param toggleRecoSection
	 * @param recoList
	 * @param hideList
	 * @param toggleContactSection
	 * @param butText
	 * @param searchToggle
	 * @param contactSectionType
	 */
	public NuxSelectFriends(String sectionTitle, String title2, String title3, String recoSectionTitle, Set<String> recoList, ArrayList<String> hideList,
			boolean toggleContactSection, String butText, boolean searchToggle, int contactSectionType)
	{
		super();
		this.sectionTitle = sectionTitle;
		this.title2 = title2;
		this.title3 = title3;
		this.recoSectionTitle = recoSectionTitle;
		this.recoList = recoList;
		this.hideList = hideList;
		this.toggleContactSection = toggleContactSection;
		this.butText = butText;
		this.searchToggle = searchToggle;
		this.contactSectionType = contactSectionType;
	}

	/**
	 * @return the sectionTitle
	 */
	public String getSectionTitle()
	{
		return sectionTitle;
	}

	/**
	 * @return the title2
	 */
	public String getTitle2()
	{
		return title2;
	}

	/**
	 * @return the title3
	 */
	public String getTitle3()
	{
		return title3;
	}

	/**
	 * @return the recoSectionTitle
	 */
	public String getRecoSectionTitle()
	{
		return recoSectionTitle;
	}

	/**
	 * @return the toggleRecoSection
	 */
	public boolean isToggleRecoSection()
	{
		return toggleRecoSection;
	}

	/**
	 * @return the recoList
	 */
	public Set<String> getRecoList()
	{
		return recoList;
	}

	/**
	 * @return the hideList
	 */
	public ArrayList<String> getHideList()
	{
		return hideList;
	}

	/**
	 * @return the toggleContactSection
	 */
	public boolean isToggleContactSection()
	{
		return toggleContactSection;
	}

	/**
	 * @return the butText
	 */
	public String getButText()
	{
		return butText;
	}

	/**
	 * @return the searchToggle
	 */
	public boolean isSearchToggle()
	{
		return searchToggle;
	}

	/**
	 * @return the contactSectionType
	 */
	public int getContactSectionType()
	{
		return contactSectionType;
	}

	private String title3;

	private String recoSectionTitle;

	private boolean toggleRecoSection;

	private Set<String> recoList;

	private ArrayList<String> hideList;

	private boolean toggleContactSection;

	private String butText;

	private boolean searchToggle;

	private int contactSectionType;

	public enum s2ContactSectionTypeEnum
	{
		all(0), hike(1), nonhike(2), both(3), none(4), unknown(-1);

		private int value;

		public int getValue()
		{
			return value;
		}

		private s2ContactSectionTypeEnum(int value)
		{
			this.value = value;
		}

		public static s2ContactSectionTypeEnum getEnum(int value)
		{
			for (s2ContactSectionTypeEnum enum1 : s2ContactSectionTypeEnum.values())
				if (enum1.value == value)
					return enum1;
			return unknown;
		}
	}

}
