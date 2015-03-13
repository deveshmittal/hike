package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.adapters.FriendsAdapter.FriendsListFetchedCallback;
import com.bsb.hike.adapters.FriendsAdapter.ViewType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.LastSeenScheduler;
import com.bsb.hike.utils.Utils;

public class FriendsFragment extends SherlockListFragment implements Listener, OnItemLongClickListener, OnScrollListener
{

	private FriendsAdapter friendsAdapter;

	private String[] pubSubListeners = { HikePubSub.ICON_CHANGED, HikePubSub.FAVORITE_TOGGLED, HikePubSub.USER_JOINED, HikePubSub.USER_LEFT, HikePubSub.CONTACT_ADDED,
			HikePubSub.REFRESH_FAVORITES, HikePubSub.FRIEND_REQUEST_ACCEPTED, HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.BLOCK_USER, HikePubSub.UNBLOCK_USER,
			HikePubSub.LAST_SEEN_TIME_UPDATED, HikePubSub.LAST_SEEN_TIME_BULK_UPDATED, HikePubSub.FRIENDS_TAB_QUERY, HikePubSub.FREE_SMS_TOGGLED,
			HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, HikePubSub.INVITE_SENT, HikePubSub.STEALTH_MODE_TOGGLED, HikePubSub.STEALTH_CONVERSATION_MARKED,
			HikePubSub.STEALTH_CONVERSATION_UNMARKED, HikePubSub.STEALTH_MODE_RESET_COMPLETE, HikePubSub.APP_FOREGROUNDED };

	private SharedPreferences preferences;

	private LastSeenScheduler lastSeenScheduler;

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.friends, null);

		ListView friendsList = (ListView) parent.findViewById(android.R.id.list);

		friendsAdapter = new FriendsAdapter(getActivity(), friendsList, friendsListFetchedCallback, ContactInfo.lastSeenTimeComparator);
		friendsAdapter.setLoadingView(parent.findViewById(R.id.spinner));
		friendsAdapter.setEmptyView(parent.findViewById(R.id.noResultView));

		friendsList.setAdapter(friendsAdapter);
		friendsList.setOnScrollListener(this);
		friendsAdapter.executeFetchTask();
		friendsList.setOnItemLongClickListener(this);
		return parent;
	}

	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(friendsAdapter != null)
		{
			friendsAdapter.getIconLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	public void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if(friendsAdapter != null)
		{
			friendsAdapter.getIconLoader().setExitTasksEarly(false);
			friendsAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		preferences = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		/*
		 * Earlier here the issue was because removeListener call was not synchronized. now that we have made it synchronized we can avoid making reference to friendsAdapter
		 * explicitly null and let GC handle this. calling frinedsAdapter destroy method just to clear out all the lists in this adapter.
		 */
		if (friendsAdapter != null)
		{
			friendsAdapter.destroy();
		}

		if (lastSeenScheduler != null)
		{
			lastSeenScheduler.stop(true);
			lastSeenScheduler = null;
		}

		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		ContactInfo contactInfo = friendsAdapter.getItem(position);

		if (FriendsAdapter.SECTION_ID.equals(contactInfo.getId()) || FriendsAdapter.EMPTY_ID.equals(contactInfo.getId()))
		{
			return;
		}

		if (FriendsAdapter.REMOVE_SUGGESTIONS_ID.equals(contactInfo.getId()))
		{
			Editor editor = preferences.edit();
			editor.putBoolean(HikeMessengerApp.HIDE_FTUE_SUGGESTIONS, true);
			editor.commit();

			friendsAdapter.makeCompleteList(true);
		}
		else if (FriendsAdapter.EXTRA_ID.equals(contactInfo.getId()))
		{
			Intent intent;
			if (FriendsAdapter.INVITE_MSISDN.equals(contactInfo.getMsisdn()))
			{
				intent = new Intent(getActivity(), TellAFriend.class);
			}
			else
			{
				intent = new Intent(getActivity(), CreateNewGroupOrBroadcastActivity.class);
			}
			startActivity(intent);
		}
		else
		{
			Utils.startChatThread(getActivity(), contactInfo);
			getActivity().finish();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(final String type, final Object object)
	{
		if (HikePubSub.ICON_CHANGED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					friendsAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.USER_JOINED.equals(type) || HikePubSub.USER_LEFT.equals(type))
		{
			final ContactInfo contactInfo = HikeMessengerApp.getContactManager().getContact((String) object, true, true);

			if (contactInfo == null)
			{
				return;
			}
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (HikePubSub.USER_JOINED.equals(type))
					{
						friendsAdapter.addToGroup(contactInfo, FriendsAdapter.HIKE_INDEX);
					}
					else if (HikePubSub.USER_LEFT.equals(type))
					{
						friendsAdapter.addToGroup(contactInfo, FriendsAdapter.SMS_INDEX);
					}
				}
			});
		}
		else if (HikePubSub.FAVORITE_TOGGLED.equals(type) || HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type) || HikePubSub.REJECT_FRIEND_REQUEST.equals(type))
		{
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					FavoriteType favoriteType = favoriteToggle.second;
					ContactInfo contactInfo = favoriteToggle.first;
					contactInfo.setFavoriteType(favoriteType);
					if ((favoriteType == FavoriteType.FRIEND) || (favoriteType == FavoriteType.REQUEST_SENT_REJECTED) || (favoriteType == FavoriteType.REQUEST_SENT)
							|| (favoriteType == FavoriteType.REQUEST_RECEIVED))
					{
						friendsAdapter.addToGroup(contactInfo, FriendsAdapter.FRIEND_INDEX);
					}
					else if (favoriteType == FavoriteType.NOT_FRIEND || favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED)
					{
						if (contactInfo.isOnhike())
						{
							friendsAdapter.addToGroup(contactInfo, FriendsAdapter.HIKE_INDEX);
						}
						else
						{
							friendsAdapter.addToGroup(contactInfo, FriendsAdapter.SMS_INDEX);
						}
					}
				}
			});
		}
		else if (HikePubSub.CONTACT_ADDED.equals(type))
		{
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo == null)
			{
				return;
			}
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if ((contactInfo.getFavoriteType() != FavoriteType.FRIEND) && (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT)
							&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT_REJECTED) && (contactInfo.getFavoriteType() != FavoriteType.REQUEST_RECEIVED))
					{
						if (contactInfo.isOnhike())
						{
							friendsAdapter.addToGroup(contactInfo, FriendsAdapter.HIKE_INDEX);
						}
						else
						{
							friendsAdapter.addToGroup(contactInfo, FriendsAdapter.SMS_INDEX);
						}
					}
					else
					{
						friendsAdapter.addToGroup(contactInfo, FriendsAdapter.FRIEND_INDEX);
					}
				}
			});
		}
		else if (HikePubSub.REFRESH_FAVORITES.equals(type))
		{
			String myMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

			boolean nativeSMSOn = Utils.getSendSmsPref(getActivity());

			final List<ContactInfo> favoriteList = HikeMessengerApp.getContactManager().getContactsOfFavoriteType(new FavoriteType[] { FavoriteType.FRIEND, FavoriteType.REQUEST_RECEIVED,
					FavoriteType.REQUEST_SENT, FavoriteType.REQUEST_SENT_REJECTED }, HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn, false);
			Collections.sort(favoriteList, ContactInfo.lastSeenTimeComparator);
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					friendsAdapter.refreshGroupList(favoriteList, FriendsAdapter.FRIEND_INDEX);
				}
			});
		}
		else if (HikePubSub.BLOCK_USER.equals(type) || HikePubSub.UNBLOCK_USER.equals(type))
		{
			String msisdn = (String) object;
			final ContactInfo contactInfo = HikeMessengerApp.getContactManager().getContact(msisdn, true, true);
			final boolean blocked = HikePubSub.BLOCK_USER.equals(type);
			if (contactInfo == null)
			{
				return;
			}
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (blocked)
					{
						friendsAdapter.removeContact(contactInfo, true);
					}
					else
					{
						if (contactInfo.isOnhike())
						{
							friendsAdapter.addToGroup(contactInfo, FriendsAdapter.HIKE_INDEX);
						}
						else
						{
							friendsAdapter.addToGroup(contactInfo, FriendsAdapter.SMS_INDEX);
						}
					}
				}
			});
		}
		else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type))
		{
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo.getFavoriteType() != FavoriteType.FRIEND)
			{
				return;
			}

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					friendsAdapter.addToGroup(contactInfo, FriendsAdapter.FRIEND_INDEX);
				}

			});
		}
		else if (HikePubSub.LAST_SEEN_TIME_BULK_UPDATED.equals(type))
		{
			List<ContactInfo> friendsList = friendsAdapter.getFriendsList();
			List<ContactInfo> friendsStealthList = friendsAdapter.getStealthFriendsList();

			Utils.updateLastSeenTimeInBulk(friendsList);
			Utils.updateLastSeenTimeInBulk(friendsStealthList);

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					friendsAdapter.makeCompleteList(false);
				}
			});

		}
		else if (HikePubSub.FRIENDS_TAB_QUERY.equals(type))
		{
			final String query = (String) object;
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					friendsAdapter.onQueryChanged(query);
				}
			});
		}
		else if (HikePubSub.FREE_SMS_TOGGLED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					friendsAdapter.toggleShowSMSContacts(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(HikeConstants.FREE_SMS_PREF, true)
							|| Utils.getSendSmsPref(getActivity()));
				}
			});
		}
		else if (HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					friendsAdapter.makeCompleteList(false);
				}
			});
		}
		else if (HikePubSub.INVITE_SENT.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					friendsAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.STEALTH_MODE_TOGGLED.equals(type))
		{
			boolean shouldChangeItemVisibility = (Boolean) object;

			if (!shouldChangeItemVisibility)
			{
				return;
			}

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					int stealthMode = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

					if (stealthMode == HikeConstants.STEALTH_ON)
					{
						friendsAdapter.addStealthContacts();
					}
					else
					{
						friendsAdapter.removeStealthContacts();
					}
				}
			});
		}
		else if (HikePubSub.STEALTH_CONVERSATION_MARKED.equals(type) || HikePubSub.STEALTH_CONVERSATION_UNMARKED.equals(type))
		{
			String msisdn = (String) object;
			if (HikePubSub.STEALTH_CONVERSATION_UNMARKED.equals(type))
			{
				friendsAdapter.stealthContactRemoved(msisdn);
			}
			else
			{
				friendsAdapter.stealthContactAdded(msisdn);
			}
		}
		else if (HikePubSub.STEALTH_MODE_RESET_COMPLETE.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					friendsAdapter.addStealthContacts();
					friendsAdapter.clearStealthLists();
				}
			});
		}
		else if (HikePubSub.APP_FOREGROUNDED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}

			if (!PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(HikeConstants.LAST_SEEN_PREF, true))
			{
				return;
			}

			getActivity().runOnUiThread(new Runnable()
			{
				
				@Override
				public void run()
				{
					if (lastSeenScheduler == null)
					{
						lastSeenScheduler = LastSeenScheduler.getInstance(getActivity());
					}
					else
					{
						lastSeenScheduler.stop(true);
					}
					lastSeenScheduler.start(true);
				}
			});
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		FriendsAdapter.ViewType viewType = FriendsAdapter.ViewType.values()[friendsAdapter.getItemViewType(position)];
		if (viewType != ViewType.FRIEND)
		{
			return false;
		}
		final ContactInfo contactInfo = friendsAdapter.getItem(position);

		ArrayList<String> optionsList = new ArrayList<String>();

		optionsList.add(getString(R.string.remove_from_favorites));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(getActivity(), R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String option = options[which];
				if (getString(R.string.remove_from_favorites).equals(option))
				{
					Utils.checkAndUnfriendContact(contactInfo);
				}
			}
		});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(getResources().getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	FriendsListFetchedCallback friendsListFetchedCallback = new FriendsListFetchedCallback()
	{

		@Override
		public void listFetched()
		{
			if (!isAdded())
			{
				return;
			}
			if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(HikeConstants.LAST_SEEN_PREF, true))
			{
				lastSeenScheduler = LastSeenScheduler.getInstance(getActivity());
				lastSeenScheduler.start(true);
			}
		}
	};

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}

		if (friendsAdapter == null)
		{
			return;
		}

		friendsAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
	}
}
