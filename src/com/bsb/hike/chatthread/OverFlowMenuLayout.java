package com.bsb.hike.chatthread;

import java.util.List;

import com.bsb.hike.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class OverFlowMenuLayout extends PopUpLayout {
	private Context context;
	private List<OverFlowMenuItem> overflowItems;
	private OverflowItemClickListener listener;
	private View viewToShow;

	public OverFlowMenuLayout(List<OverFlowMenuItem> overflowItems,
			OverflowItemClickListener listener, Context context) {
		this.overflowItems = overflowItems;
		this.listener = listener;
		this.context = context;
	}

	public void showPopUpWindow(int width, int height, View anchor) {
		showPopUpWindow(width, height, 0, 0, anchor);
	}

	public void showPopUpWindow(int width, int height, int xOffset,
			int yOffset, View anchor) {
		if (viewToShow == null) {
			initView();
		}
		if (popup == null) {
			getPopUpWindow(width, height, viewToShow, context);
			popup.setBackgroundDrawable(context.getResources().getDrawable(
					android.R.color.transparent));
			popup.setOutsideTouchable(true);
			popup.setFocusable(true);
		}
		popup.showAsDropDown(anchor, xOffset, yOffset);
	}

	@Override
	public View getView() {
		return viewToShow;
	}

	@Override
	public void initView() {
		// TODO : Copypasted code from chat thread, make separate layout file
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
				newGamesIndicator.setVisibility(View.GONE);

				return convertView;
			}
		});
	}
}
