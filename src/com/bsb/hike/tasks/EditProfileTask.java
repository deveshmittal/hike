package com.bsb.hike.tasks;

import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity.ProfileType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class EditProfileTask implements IHikeHTTPTask
{
	private String currName;

	private String newName;

	private String currEmail;

	private String newEmail;

	private int currGenderType;

	private int newGenderType;

	private String msisdn;

	private String filePath;

	private byte[] avatarBitmapBytes;

	private ProfileType profileType;

	private boolean isBackPressed;

	private Context applicationCtx;

	private SharedPreferences preferences;

	private AtomicInteger editProfileRequestsCount;

	private RequestToken editNameRequestToken;

	private RequestToken editEmailGenderRequestToken;

	private RequestToken editAvatarRequestToken;

	public EditProfileTask(String msisdn, ProfileType profileType, String currName, String currEmail, int currGenderType, boolean isBackPressed)
	{
		this.msisdn = msisdn;
		this.profileType = profileType;
		this.currName = currName;
		this.currEmail = currEmail;
		this.currGenderType = currGenderType;
		this.isBackPressed = isBackPressed;
		this.applicationCtx = HikeMessengerApp.getInstance().getApplicationContext();
		this.preferences = applicationCtx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
		this.editProfileRequestsCount = new AtomicInteger(0);
	}

	public void setNewName(String name)
	{
		this.newName = name;
	}

	public void setEmail(String email)
	{
		this.newEmail = email;
	}

	public void setGenderType(int genderType)
	{
		this.newGenderType = genderType;
	}

	public void setAvatarBitmapBytes(byte[] bitmapBytes)
	{
		this.avatarBitmapBytes = bitmapBytes;
	}

	public void setProfilePicPath(String filePath)
	{
		this.filePath = filePath;
	}

	@Override
	public void execute()
	{
		if (!TextUtils.isEmpty(newName) && !currName.equals(newName))
		{
			editProfileRequestsCount.incrementAndGet();
			editProfileName();
		}
		if (filePath != null)
		{
			editProfileRequestsCount.incrementAndGet();
			editProfileAvatar();
		}
		if (newEmail != null && (!newEmail.equals(currEmail) || newGenderType != currGenderType))
		{
			editProfileRequestsCount.incrementAndGet();
			editProfileEmailGender();
		}
	}

	private void editProfileName()
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put("name", newName);
		}
		catch (JSONException e)
		{
			Logger.e("ProfileActivity", "Could not set name", e);
		}

		if (this.profileType == ProfileType.GROUP_INFO)
		{
			editNameRequestToken = HttpRequests.editGroupProfileNameRequest(json, getEditNameRequestListener(), msisdn);
		}
		else
		{
			editNameRequestToken = HttpRequests.editProfileNameRequest(json, getEditNameRequestListener());
		}
		editNameRequestToken.execute();
	}

	private void editProfileAvatar()
	{
		if (profileType == ProfileType.GROUP_INFO)
		{
			editAvatarRequestToken = HttpRequests.editGroupProfileAvatarRequest(filePath, getEditAvatarRequestListener(), msisdn);
		}
		else
		{
			editAvatarRequestToken = HttpRequests.editProfileAvatarRequest(filePath, getEditAvatarRequestListener());
		}
		editAvatarRequestToken.execute();
	}

	private void editProfileEmailGender()
	{
		JSONObject obj = new JSONObject();
		try
		{
			Logger.d(getClass().getSimpleName(), "Profile details Email: " + newEmail + " Gender: " + newGenderType);
			if (!currEmail.equals(newEmail))
			{
				obj.put(HikeConstants.EMAIL, newEmail);
			}
			if (newGenderType != currGenderType)
			{
				obj.put(HikeConstants.GENDER, newGenderType == 1 ? "m" : newGenderType == 2 ? "f" : "");
			}
			Logger.d(getClass().getSimpleName(), "JSON to be sent is: " + obj.toString());
		}
		catch (JSONException e)
		{
			Logger.e("ProfileActivity", "Could not set email or gender", e);
		}

		editEmailGenderRequestToken = HttpRequests.editProfileEmailGenderRequest(obj, getEditEmailGenderRequestListener());
		editEmailGenderRequestToken.execute();
	}

	@Override
	public void cancel()
	{
		if (editNameRequestToken != null)
		{
			editNameRequestToken.cancel();
		}
		if (editAvatarRequestToken != null)
		{
			editAvatarRequestToken.cancel();
		}
		if (editEmailGenderRequestToken != null)
		{
			editEmailGenderRequestToken.cancel();
		}
	}

	private IRequestListener getEditNameRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				if (profileType != ProfileType.GROUP_INFO)
				{
					/*
					 * if the request was successful, update the shared preferences and the UI
					 */
					String name = newName;
					Editor editor = preferences.edit();
					editor.putString(HikeMessengerApp.NAME_SETTING, name);
					editor.commit();
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_NAME_CHANGED, null);
				}
				if (editProfileRequestsCount.decrementAndGet() == 0)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
				}
				
				if (isBackPressed)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				if (editProfileRequestsCount.decrementAndGet() == 0)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
				}
				showErrorToast(R.string.update_profile_failed, Toast.LENGTH_LONG);
				if (isBackPressed)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
				}
			}
		};
	}

	private IRequestListener getEditEmailGenderRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				Editor editor = preferences.edit();
				if (Utils.isValidEmail(newEmail))
				{
					editor.putString(HikeConstants.Extras.EMAIL, newEmail);
				}
				editor.putInt(HikeConstants.Extras.GENDER, newGenderType);
				editor.commit();
				
				if (editProfileRequestsCount.decrementAndGet() == 0)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
				}
				
				if (isBackPressed)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				if (editProfileRequestsCount.decrementAndGet() == 0)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
				}
				showErrorToast(R.string.update_profile_failed, Toast.LENGTH_LONG);
				if (isBackPressed)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
				}
			}
		};
	}

	private IRequestListener getEditAvatarRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();

				ContactManager.getInstance().setIcon(msisdn, avatarBitmapBytes, false);
				Utils.renameTempProfileImage(msisdn);

				if (profileType == ProfileType.USER_PROFILE || profileType == ProfileType.USER_PROFILE_EDIT)
				{
					/*
					 * Making the profile pic change a status message.
					 */
					JSONObject data = response.optJSONObject("status");

					if (data == null)
					{
						return;
					}

					String mappedId = data.optString(HikeConstants.STATUS_ID);
					String msisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");
					String name = preferences.getString(HikeMessengerApp.NAME_SETTING, "");
					long time = (long) System.currentTimeMillis() / 1000;

					StatusMessage statusMessage = new StatusMessage(0, mappedId, msisdn, name, "", StatusMessageType.PROFILE_PIC, time, -1, 0);
					HikeConversationsDatabase.getInstance().addStatusMessage(statusMessage, true);

					ContactManager.getInstance().setIcon(statusMessage.getMappedId(), avatarBitmapBytes, false);

					String srcFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + msisdn + ".jpg";

					String destFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + mappedId + ".jpg";

					/*
					 * Making a status update file so we don't need to download this file again.
					 */
					Utils.copyFile(srcFilePath, destFilePath, null);

					int unseenUserStatusCount = preferences.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
					Editor editor = preferences.edit();
					editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, ++unseenUserStatusCount);
					editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
					editor.commit();
					/*
					 * This would happen in the case where the user has added a self contact and received an mqtt message before saving this to the db.
					 */

					if (statusMessage.getId() != -1)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
						HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
					}
				}

				if (editProfileRequestsCount.decrementAndGet() == 0)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
				}
				
				HikeMessengerApp.getLruCache().clearIconForMSISDN(msisdn);
				HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);

				if (isBackPressed)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.d("EditProfileTask", "resetting image" + httpException.getMessage());

				if (editProfileRequestsCount.decrementAndGet() == 0)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PROFILE_FAILED, null);
			}
		};
	}

	private void showErrorToast(int stringResId, int duration)
	{
		Toast toast = Toast.makeText(applicationCtx, stringResId, duration);
		toast.show();
	}

}
