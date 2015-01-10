package com.bsb.hike.chatthread;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.R;

public class HikeActionMode implements ActionMode.Callback, OnClickListener
{
	public static interface ActionModeListener
	{
		public void actionModeDestroyed(int id);

		public void doneClicked(int id);

		public void initActionbarActionModeView(int id,View view);
	}

	private static final int DEFAULT_LAYOUT = R.layout.hike_action_mode;

	protected ActionMode actionMode;

	protected SherlockFragmentActivity sherlockFragmentActivity;

	private int title, save, layoutId = DEFAULT_LAYOUT, id;

	private ActionModeListener listener;

	public HikeActionMode(SherlockFragmentActivity sherlockFragmentActivity)
	{
		this(sherlockFragmentActivity, -1, -1, DEFAULT_LAYOUT);
	}

	public HikeActionMode(SherlockFragmentActivity sherlockFragmentActivity, int layoutId)
	{
		this(sherlockFragmentActivity, -1, -1, layoutId);
	}

	public HikeActionMode(SherlockFragmentActivity sherlockFragmentActivity, int title, int save, int layoutId)
	{
		this.sherlockFragmentActivity = sherlockFragmentActivity;
		this.layoutId = layoutId;
		this.title = title;
		this.save = save;
	}

	public void setListener(ActionModeListener listener)
	{
		this.listener = listener;
	}

	protected void startActionMode()
	{
		sherlockFragmentActivity.startActionMode(this);
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu)
	{
		this.actionMode = mode;
		mode.setCustomView(LayoutInflater.from(sherlockFragmentActivity).inflate(layoutId, null));
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu)
	{
		initView();
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		this.actionMode = null;
		if (listener != null)
		{
			listener.actionModeDestroyed(id);
		}
		actionBarDestroyed();
	}

	public void showActionMode(int id)
	{
		showActionMode(id, layoutId);
	}

	public void showActionMode(int id, int layoutId)
	{
		this.layoutId = layoutId;
		showActionMode(id, title, save);
	}

	public void showActionMode(int id, int title, int save)
	{
		this.id = id;
		this.title = title;
		this.save = save;
		sherlockFragmentActivity.startActionMode(this);
	}

	private void initDefaultView()
	{
		setText(R.id.title, title, -1);
		setText(R.id.save, save, R.anim.scale_in);
		actionMode.getCustomView().findViewById(R.id.done_container).setOnClickListener(this);
	}

	/**
	 * Override this function in case you have different custom xml
	 */
	protected void initView()
	{
		if (layoutId == DEFAULT_LAYOUT)
		{
			initDefaultView();
		}
		else
		{
			if (listener != null)
			{
				listener.initActionbarActionModeView(id,actionMode.getCustomView());
			}
		}
	}

	private View setText(int viewId, int textId, int animId)
	{
		if (textId != -1)
		{
			TextView tv = (TextView) actionMode.getCustomView().findViewById(viewId);
			tv.setText(textId);
			if (animId != -1)
			{
				tv.startAnimation(AnimationUtils.loadAnimation(sherlockFragmentActivity, animId));
			}
			return tv;
		}
		return null;
	}

	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.done_container)
		{
			doneClicked();
		}
	}

	protected void actionBarDestroyed()
	{
	}

	protected void doneClicked()
	{
		if (listener != null)
		{
			listener.doneClicked(id);
		}
	}

	public boolean onBackPressed()
	{
		if (actionMode != null)
		{
			actionMode.finish();
			return true;
		}
		return false;
	}

	public void finish()
	{
		if (actionMode != null)
		{
			actionMode.finish();
		}
	}
}
