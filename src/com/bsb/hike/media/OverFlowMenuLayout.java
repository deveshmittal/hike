package com.bsb.hike.media;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;

public class OverFlowMenuLayout implements OnItemClickListener {
	protected Context context;
	protected List<OverFlowMenuItem> overflowItems;
	protected OverflowItemClickListener listener;
	protected View viewToShow;
	protected PopUpLayout popUpLayout;
	private OnDismissListener mOnDismisslistener;

	/**
	 * This class is made to show overflow menu items, by default it populates
	 * listview of items you want o display, if some other view is required,
	 * extend this class and override initview and getview
	 * 
	 * @param overflowItems
	 * @param listener
	 * @param context
	 */
	public OverFlowMenuLayout(List<OverFlowMenuItem> overflowItems,
			OverflowItemClickListener listener, OnDismissListener onDismissListener, Context context) {
		this.overflowItems = overflowItems;
		this.listener = listener;
		this.context = context;
		this.mOnDismisslistener = onDismissListener;
		popUpLayout = new PopUpLayout(context);
	}

	public View getView() {
		return viewToShow;
	}

	public void initView() {
		// TODO : Copypasted code from chat thread, make separate layout file
		if (viewToShow != null) {
			return;
		}
		viewToShow = LayoutInflater.from(context).inflate(
				R.layout.overflow_menu, null);
		ListView overFlowListView = (ListView) viewToShow
				.findViewById(R.id.overflow_menu_list);
		overFlowListView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(context,
				0, 0, overflowItems) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = LayoutInflater.from(context).inflate(
							R.layout.over_flow_menu_item, parent, false);
				}

				OverFlowMenuItem item = getItem(position);

				TextView itemTextView = (TextView) convertView
						.findViewById(R.id.item_title);
				itemTextView.setText(item.text);

				convertView.findViewById(R.id.profile_image_view)
						.setVisibility(View.GONE);

				TextView freeSmsCount = (TextView) convertView
						.findViewById(R.id.free_sms_count);
				freeSmsCount.setVisibility(View.GONE);
				
				TextView newGamesIndicator = (TextView) convertView
						.findViewById(R.id.new_games_indicator);
				
				if(item.unreadCount <= 0)
				{	
					newGamesIndicator.setVisibility(View.GONE);
				}
				
				else
				{
					newGamesIndicator.setText(setUnreadCounter(item.unreadCount));
					newGamesIndicator.setVisibility(View.VISIBLE);
				}

				return convertView;
			}
		});
		overFlowListView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		listener.itemClicked((OverFlowMenuItem) arg0.getAdapter().getItem(arg2));
		popUpLayout.dismiss();
	}

	public void show(int width, int height, View anchor) {
		show(width, height, 0, 0, anchor);
	}
	
	public void show(int width, int height, View anchor, int inputMethodMode) {
		show(width, height, 0, 0, anchor, inputMethodMode);
	}

	public void show(int width, int height, int xOffset,
			int yOffset, View anchor) {
		initView();
		popUpLayout.showPopUpWindow(width, height, xOffset, yOffset, anchor,
				getView());
		popUpLayout.setOnDismissListener(mOnDismisslistener);
	}
	
	public void show(int width, int height, int xOffset, int yOffset, View anchor, int inputMethodMode)
	{
		initView();
		popUpLayout.showPopUpWindow(width, height, xOffset, yOffset, anchor,
				getView(), inputMethodMode);
		popUpLayout.setOnDismissListener(mOnDismisslistener);
	}
	
	public void appendItem(OverFlowMenuItem item) {
		this.overflowItems.add(item);
	}

	public void appendItem(OverFlowMenuItem item, int position) {
		this.overflowItems.add(position, item);
	}

	public void appendItems(OverFlowMenuItem... items) {
		for (OverFlowMenuItem item : items) {
			this.overflowItems.add(item);
		}
	}

	public void removeItem(int id) {
		Iterator<OverFlowMenuItem> iterator = overflowItems.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().id == id) {
				iterator.remove();
				break;
			}
		}
	}
	
	public List<OverFlowMenuItem> getOverFlowMenuItems()
	{
		return overflowItems;
	}
	

	public void notifyDateSetChanged()
	{
		if(viewToShow != null)
		{
			ListView overFlowListView = (ListView) viewToShow
				.findViewById(R.id.overflow_menu_list);
			
			((ArrayAdapter)overFlowListView.getAdapter()).notifyDataSetChanged();
		}
		
	}
	
	private String setUnreadCounter(int counter)
	{
		if(counter >= HikeConstants.MAX_PIN_CONTENT_LINES_IN_HISTORY)
		{
			return context.getString(R.string.max_pin_unread_counter);
		}
		
		else
		{
			return Integer.toString(counter);
		}
	}

}