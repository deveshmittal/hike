package com.bsb.hike.models;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bsb.hike.utils.Utils;

public class NUXChatReward
{

	private boolean toggleModule;

	private String rewardCardText;

	private String rewardCardSuccessText;

	private String statusText;

	private String chatWaitingText;

	private String pendingChatIcon;

	private String detailsText;

	private String detailsLink;

	private String inviteMoreText;

	private String remindText;

	private String tapToClaimLink;
	
	private String tapToClaimText;
	
	private String selectFriends;

	/**
	 * @return the toggleModule
	 */
	public boolean isToggleModule()
	{
		return toggleModule;
	}

	/**
	 * @return the rewardCardText
	 */
	public String getRewardCardText()
	{
		return rewardCardText;
	}

	/**
	 * @return the rewardCardSuccessText
	 */
	public String getRewardCardSuccessText()
	{
		return rewardCardSuccessText;
	}

	/**
	 * @return the statusText
	 */
	public String getStatusText()
	{
		return statusText;
	}

	/**
	 * @return the chatWaitingText
	 */
	public String getChatWaitingText()
	{
		return chatWaitingText;
	}

	/**
	 * @return the pendingChatIcon
	 */
	public Bitmap getPendingChatIcon()
	{
		if (!TextUtils.isEmpty(pendingChatIcon))
		{
			return Utils.stringToBitmap(pendingChatIcon);
		}

		return null;
	}

	/**
	 * @return the detailsText
	 */
	public String getDetailsText()
	{
		return detailsText;
	}

	/**
	 * @return the detailsLink
	 */
	public String getDetailsLink()
	{
		return detailsLink;
	}

	/**
	 * @return the button1Text
	 */
	public String getInviteMoreButtonText()
	{
		return inviteMoreText;
	}

	/**
	 * @return the button2Text
	 */
	public String getRemindButtonText()
	{
		return remindText;
	}

	/**
	 * @return the tapToClaimLink
	 */
	public String getTapToClaimLink()
	{
		return tapToClaimLink;
	}

	/**
	 * @param toggleModule
	 * @param rewardCardText
	 * @param rewardCardSuccessText
	 * @param statusText
	 * @param chatWaitingText
	 * @param pendingChatIcon
	 * @param detailsText
	 * @param detailsLink
	 * @param button1Text
	 * @param button2Text
	 * @param tapToClaimLink
	 */
	public NUXChatReward(boolean toggleModule, String rewardCardText, String rewardCardSuccessText, String statusText, String chatWaitingText, String pendingChatIcon,
			String detailsText, String detailsLink, String button1Text, String button2Text, String tapToClaimLink,String tapToClaimText,String selectFriends)
	{
		this.toggleModule = toggleModule;
		this.rewardCardText = rewardCardText;
		this.rewardCardSuccessText = rewardCardSuccessText;
		this.statusText = statusText;
		this.chatWaitingText = chatWaitingText;
		this.pendingChatIcon = pendingChatIcon;
		this.detailsText = detailsText;
		this.detailsLink = detailsLink;
		this.inviteMoreText = button1Text;
		this.remindText = button2Text;
		this.tapToClaimLink = tapToClaimLink;
		this.tapToClaimText=tapToClaimText;
		this.selectFriends=selectFriends;
	}

	public String getTapToClaimText()
	{
		// TODO Auto-generated method stub
		return tapToClaimText;
	}

	public String getSelectFriendsText()
	{
		// TODO Auto-generated method stub
		return selectFriends;
	}

}
