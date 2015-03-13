package com.bsb.hike.productpopup;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.SparseArray;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.platform.content.PlatformContentModel;
import com.bsb.hike.productpopup.ProductPopupsConstants.PopupStateEnum;
import com.bsb.hike.productpopup.ProductPopupsConstants.PopupTriggerPoints;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author himanshu
 * 
 *         The Manager to ProductPopus.This call handles when the popups need to be shown ,all interactions with the ProductPopup DB.
 */
public class ProductInfoManager
{
	// Handler that is running on the backend thread.

	private final Handler handler = new Handler(HikeHandlerUtil.getInstance().getLooper());

	private static final ProductInfoManager mmProductInfoMgr = new ProductInfoManager();

	// <TRIGGER_POINT,ARRAYLIST<POPUPS>
	private SparseArray<ArrayList<ProductContentModel>> mmSparseArray = new SparseArray<ArrayList<ProductContentModel>>();

	Context context = null;

	// Singleton class
	private ProductInfoManager()
	{
		context = HikeMessengerApp.getInstance().getApplicationContext();

	}

	// To be called once in on create of HikeMessangerApp.
	public void init()
	{
		handler.post(new Runnable()
		{

			@Override
			public void run()
			{
				mmSparseArray = HikeContentDatabase.getInstance().getAllPopup();
			}
		});

	}

	public static ProductInfoManager getInstance()
	{
		return mmProductInfoMgr;
	}

	/**
	 * 
	 * @param val
	 * @param iShowPopup
	 *            -Listener to return the result on the Calling Activity
	 * 
	 *            Logic :this method check taht if there is any popup on a particular trigger point ...
	 * 
	 *            We get the Arraylist containing of all the popups for the corresponding trigger point.We sort it according to the start time.Then we check the popup with the
	 *            highest start time whose endtime is also valid ..And we delete all the popups with the lower start time ..and we do nothing for the popups whose start time is
	 *            greater that the Present start time(Future Popus)
	 * 
	 * 
	 */
	public void isThereAnyPopup(Integer val, IActivityPopup iShowPopup)
	{
		ProductContentModel mmModel = null;
		long presentTime = System.currentTimeMillis();
		ArrayList<ProductContentModel> popUpToBeDeleted=null;
		if (mmSparseArray.get(val) != null && !mmSparseArray.get(val).isEmpty())
		{
			popUpToBeDeleted = new ArrayList<ProductContentModel>();
			ArrayList<ProductContentModel> mmArray = mmSparseArray.get(val);
			Collections.sort(mmArray, ProductContentModel.ProductContentStartTimeComp);
			for (ProductContentModel productContentModel : mmArray)
			{
				if (productContentModel.getStarttime() <= presentTime)
				{
					if (productContentModel.getEndtime() >= presentTime && mmModel == null)
					{
						mmModel = productContentModel;
						Logger.d("ProductPopup", productContentModel.getTriggerpoint() + ">>>>>" + productContentModel.getStarttime() + ">>>>>>" + productContentModel.getEndtime());
					}
					else
					{
						popUpToBeDeleted.add(productContentModel);
					}
				}
			}
		}

		deletePopups(popUpToBeDeleted);

		if (mmModel != null)
		{
			parseAndShowPopup(mmModel, iShowPopup);
		}
		else
		{
			iShowPopup.onFailure();
		}
	}

	/**
	 * 
	 * @param mmArrayList
	 * 
	 *            Deleting all the Popups from the Database and form the memory
	 */
	public void deletePopups(final ArrayList<ProductContentModel> mmArrayList)
	{

		// Deleting all the things on Backend thread;
		handler.post(new Runnable()
		{

			@Override
			public void run()
			{
				if (mmArrayList != null && !mmArrayList.isEmpty())
				{
					
					String[] hashCode = new String[mmArrayList.size()];
					int length = 0;
					for (ProductContentModel a : mmArrayList)
					{
						hashCode[length++] = a.hashCode() + "";

					}
					Logger.d("ProductPopup",hashCode.toString());
					HikeContentDatabase.getInstance().deletePopup(hashCode);
					int triggerPoint = (mmArrayList.get(0).getTriggerpoint());
					Logger.d("ProductPopup", "start deleting" + mmArrayList.get(0).getTriggerpoint() + "<<<<<");

					if (mmSparseArray.get(triggerPoint) != null)
						mmSparseArray.get(triggerPoint).removeAll(mmArrayList);

					Logger.d("ProductPopup", "End deleting" + mmArrayList.toString());
				}
			}
		});

	}

	/**
	 * 
	 * @param productContentModel
	 * @param iShowPopup
	 *            -Listener to return the result to the calling activity
	 * 
	 *            This method is responsible for downloading the zip if not present and then mustaching the template,validating the data
	 */
	private void parseAndShowPopup(final ProductContentModel productContentModel, final IActivityPopup iShowPopup)
	{

		PlatformContent.getContent(productContentModel.toJSONString(), new PopupContentListener(productContentModel, iShowPopup)
		{
			ProductContentModel productContentModel = null;

			@Override
			public void onEventOccured(EventCode event)
			{
				productContentModel = getProductContentModel();

				switch (event)
				{
				case LOW_CONNECTIVITY:
					HikeContentDatabase.getInstance().updatePopupStatus(productContentModel.hashCode(), PopupStateEnum.NOT_DOWNLOADED.ordinal());
					if (getListener() != null)
					{
						getListener().onFailure();
					}
					break;
				case INVALID_DATA:

				case STORAGE_FULL:

					ArrayList<ProductContentModel> mmArrayList = new ArrayList<ProductContentModel>();
					mmArrayList.add(productContentModel);
					deletePopups(mmArrayList);
					if (getListener() != null)
					{
						getListener().onFailure();
					}
					break;
				default:

					break;
				}
			}

			@Override
			public void onComplete(PlatformContentModel content)
			{
				productContentModel = getProductContentModel();

				if (getListener() != null)
				{
					productContentModel.setFormedData(content.getFormedData());
					getListener().onSuccess(productContentModel);
				}
				else
				{
					handlePushScenarios(productContentModel);
					if (mmSparseArray.get(productContentModel.getTriggerpoint()) != null)
					{
						ArrayList<ProductContentModel> mmArrayList = mmSparseArray.get(productContentModel.getTriggerpoint());
						mmArrayList.add(productContentModel);
					}
					else
					{
						ArrayList<ProductContentModel> mmArrayList = new ArrayList<ProductContentModel>();
						mmArrayList.add(productContentModel);
						mmSparseArray.put(productContentModel.getTriggerpoint(), mmArrayList);
					}
					HikeContentDatabase.getInstance().updatePopupStatus(productContentModel.hashCode(), PopupStateEnum.DOWNLOADED.ordinal());
				}
			}

		});

	}

	private void handlePushScenarios(ProductContentModel productContentModel)
	{
		if (productContentModel.isPushReceived())
		{

			Intent intent = new Intent();
			intent.putExtra(ProductPopupsConstants.USER, productContentModel.getUser());
			intent.putExtra(ProductPopupsConstants.NOTIFICATION_TITLE, productContentModel.getNotifTitle());
			intent.putExtra(ProductPopupsConstants.PUSH_SOUND, productContentModel.shouldPlaySound());
			intent.putExtra(ProductPopupsConstants.TRIGGER_POINT, productContentModel.getTriggerpoint());
			if (productContentModel.isPushFuture())
			{
				
				Logger.d("ProductPopup","Future Push Received"+productContentModel.getPushTime()+"......."+ System.currentTimeMillis());
				HikeAlarmManager.setAlarmWithIntent(HikeMessengerApp.getInstance().getApplicationContext(),productContentModel.getPushTime(),
						HikeAlarmManager.REQUESTCODE_PRODUCT_POPUP, true, intent);
			}
			else
			{	
				NotificationContentModel mmNotificationContentModel=new NotificationContentModel(productContentModel.getNotifTitle(), productContentModel.getUser(), productContentModel.shouldPlaySound(), productContentModel.getTriggerpoint());
				ProductInfoManager.getInstance().notifyUser(mmNotificationContentModel);
			}
		}
	}

	/**
	 * 
	 * @param metaData
	 *            of the Json
	 * 
	 *            Saving the popup.
	 */
	public void parsePopupPacket(JSONObject metaData)
	{

		Logger.d("ProductPopup", "Popup received Going to inserti in DB");

		// saving the Popup in DataBase:
		ProductContentModel productContentModel = ProductContentModel.makeProductContentModel(metaData);

		HikeContentDatabase.getInstance().savePopup(productContentModel, PopupStateEnum.NOT_DOWNLOADED.ordinal());

		parseAndShowPopup(productContentModel, null);

	}

	/**
	 * 
	 * @param intent
	 * 
	 *            Send a push to the user
	 */
	public void notifyUser(NotificationContentModel notificationContentModel)
	{
		String title = notificationContentModel.getTitle();
		String user = notificationContentModel.getUser();
		boolean shouldPlaySound = notificationContentModel.isShouldPlaySound();
		int triggerpoint = notificationContentModel.getTriggerpoint();

		if (triggerpoint == PopupTriggerPoints.CHAT_SCR.ordinal())
		{
			HikeNotification.getInstance(context).notifyStringMessage(user, title, !shouldPlaySound);

		}
		else
		{
			HikeNotification.getInstance(context).notifyUserAndOpenHomeActivity(user, title, !shouldPlaySound);
		}
	}

	/**
	 * 
	 * @param mmModel
	 * @return
	 * 
	 *         Creating a Dialog Pojo ...Utility method
	 */
	public DialogPojo getDialogPojo(ProductContentModel mmModel)
	{
		DialogPojo mmPojo = new DialogPojo(mmModel.isFullScreen(), mmModel.getHeight(), mmModel.getFormedData(), mmModel);
		return mmPojo;
	}

	/**
	 * function to get the host from the Url.
	 * @param metaData
	 * @return
	 */
	public String getFormedUrl(String metaData)
	{
		HikeSharedPreferenceUtil mmHikeSharedPreferenceUtil = HikeSharedPreferenceUtil.getInstance();
		try
		{
			JSONObject mmObject = new JSONObject(metaData);
			String url = mmObject.optString(ProductPopupsConstants.URL);
			if (!TextUtils.isEmpty(url))
			{
				url = url.replace("$reward_token", mmHikeSharedPreferenceUtil.getData(HikeMessengerApp.REWARDS_TOKEN, ""));
				url = url.replace("$msisdn", mmHikeSharedPreferenceUtil.getData(HikeMessengerApp.MSISDN_SETTING, ""));
				url = url.replace("$uid", mmHikeSharedPreferenceUtil.getData(HikeMessengerApp.UID_SETTING, ""));
				url = url.replace("$invite_token", mmHikeSharedPreferenceUtil.getData(HikeConstants.INVITE_TOKEN, ""));
			}
			return url;

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * The function calls the Url.Calling from background  Thread .
	 * @param metaData
	 */
	public void callToServer(final String metaData)
	{

		handler.post(new Runnable()
		{
		
			@Override
			public void run()
			{
				try
				{
					String host = getFormedUrl(metaData);
					if (!TextUtils.isEmpty(host))
					{
						URL url = new URL(host);
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						connection.connect();
						Logger.d("ProductPopup",connection.getResponseCode()+"");
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

			}
		});
	}

}
