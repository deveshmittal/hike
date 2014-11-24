package com.bsb.hike.chatthread;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.R;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;

public class ThemePicker extends PopUpLayout implements BackPressListener,
		OnDismissListener {
	private static final String TAG = "themepicker";

	public static interface ThemePickerListener {
		public void themeClicked(ChatTheme theme);

		public void themeSelected(ChatTheme theme);

		public void themeCancelled();
	}

	private SherlockFragmentActivity sherlockFragmentActivity;
	private View viewToDisplay;
	private ActionMode actionMode;
	private ChatTheme currentSelected;
	private ThemePickerListener listener;

	public ThemePicker(SherlockFragmentActivity sherlockFragmentActivity,
			ThemePickerListener listener) {
		this.sherlockFragmentActivity = sherlockFragmentActivity;
		this.listener = listener;
	}

	public void showThemePicker(int width, int height, View anchor,
			ChatTheme currentTheme) {

	}

	public void showThemePicker(int width, int height, int xoffset,
			int yoffset, View anchor, ChatTheme currentTheme) {
		Logger.i(TAG, "show theme picker");
		// do processing
		if (actionMode == null) {
			sherlockFragmentActivity.startActionMode(actionmodeCallback);
		}
		if (viewToDisplay == null) {
			initView();
		}
		if (popup == null) {
			getPopUpWindow(width, height, viewToDisplay,
					sherlockFragmentActivity);
			

		}
		Logger.i(TAG, "show theme picker " + popup);

		popup.setWidth(width);
		popup.setHeight(height);
		popup.showAsDropDown(anchor, xoffset, yoffset);
		popup.setFocusable(true);
		popup.setTouchInterceptor(this);
		popup.setBackgroundDrawable(sherlockFragmentActivity.getResources()
				.getDrawable(android.R.color.transparent));
		popup.setOnDismissListener(this);

	}

	@Override
	public View getView() {
		return viewToDisplay;
	}

	@Override
	public void initView() {
		View parentView = viewToDisplay = sherlockFragmentActivity
				.getLayoutInflater().inflate(R.layout.chat_backgrounds, null);

		GridView attachmentsGridView = (GridView) parentView
				.findViewById(R.id.attachment_grid);

		TextView chatThemeTip = (TextView) parentView
				.findViewById(R.id.chat_theme_tip);

		// chatThemeTip.setText(mConversation instanceof GroupConversation ?
		// R.string.chat_theme_tip_group : R.string.chat_theme_tip);
		// chatThemeTip.setVisibility(mConversation.isOnhike() ? View.VISIBLE :
		// View.GONE);
		chatThemeTip.setText(R.string.chat_theme_tip_group);

		attachmentsGridView.setNumColumns(getNumColumnsChatThemes());

		final ArrayAdapter<ChatTheme> gridAdapter = new ArrayAdapter<ChatTheme>(
				sherlockFragmentActivity.getApplicationContext(), -1,
				ChatTheme.values()) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = LayoutInflater.from(sherlockFragmentActivity)
							.inflate(R.layout.chat_bg_item, parent, false);
				}
				ChatTheme chatTheme = getItem(position);

				ImageView theme = (ImageView) convertView
						.findViewById(R.id.theme);
				ImageView animatedThemeIndicator = (ImageView) convertView
						.findViewById(R.id.animated_theme_indicator);

				animatedThemeIndicator
						.setVisibility(chatTheme.isAnimated() ? View.VISIBLE
								: View.GONE);
				theme.setBackgroundResource(chatTheme.previewResId());
				theme.setEnabled(currentSelected == chatTheme);

				return convertView;
			}
		};

		attachmentsGridView.setAdapter(gridAdapter);
		if (currentSelected != null) {
			int selection = currentSelected.ordinal();
			attachmentsGridView.setSelection(selection);
		}
		attachmentsGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				currentSelected = ChatTheme.values()[position];
				gridAdapter.notifyDataSetChanged();
				Logger.d("ChatThread",
						"Calling setchattheme from showThemePicker onItemClick");
				listener.themeClicked(currentSelected);
			}
		});

	}

	private ActionMode.Callback actionmodeCallback = new ActionMode.Callback() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			Logger.i(TAG, "on prepare action mode");
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			Logger.i(TAG, "on destroy actionmode");
			actionMode = null;
			popup.dismiss();
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			Logger.i(TAG, "on create action mode");
			actionMode = mode;
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Logger.i(TAG, "onActionItemClicked");
			return false;
		}
	};

	@Override
	public boolean onBackPressed() {
		if (isShowing()) {
			popup.dismiss();
			return true;
		}
		return false;
	}

	private int getNumColumnsChatThemes() {
		Resources resources = sherlockFragmentActivity.getResources();
		int width = resources.getDisplayMetrics().widthPixels;

		int chatThemePaletteMargin = 2 * resources
				.getDimensionPixelSize(R.dimen.chat_theme_palette_margin);

		int chatThemeGridMargin = 2 * resources
				.getDimensionPixelSize(R.dimen.chat_theme_grid_margin);

		int chatThemeGridWidth = width - chatThemeGridMargin
				- chatThemePaletteMargin;

		int chatThemeItemWidth = resources
				.getDimensionPixelSize(R.dimen.chat_bg_item_width);

		return (int) (chatThemeGridWidth / chatThemeItemWidth);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
			return true;
		}
		return false;
	}

	@Override
	public void onDismiss() {
//		actionMode.finish();
	}

}
