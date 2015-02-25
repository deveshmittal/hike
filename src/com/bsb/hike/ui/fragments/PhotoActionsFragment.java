package com.bsb.hike.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.R;

public class PhotoActionsFragment extends SherlockFragment
{

	private View mFragmentView;

	private ListView mListView;

	private String[] mTitles;

	private String[] mDescription;

	private int itemIcons[] = { R.drawable.set_icon, R.drawable.send_icon };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		mFragmentView = inflater.inflate(R.layout.photos_action_fragment, null);

		mListView = (ListView) mFragmentView.findViewById(R.id.actionsListView);

		loadData();

		mListView.setAdapter(new PhotoActionsListAdapter());

		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Toast.makeText(getActivity(), "Position: " + position + 1, Toast.LENGTH_LONG).show();
			}
		});

		return mFragmentView;
	}

	private void loadData()
	{
		mTitles = getActivity().getResources().getStringArray(R.array.photos_actions_titles);

		mDescription = getActivity().getResources().getStringArray(R.array.photos_actions_description);
	}

	class PhotoActionsListAdapter extends BaseAdapter
	{

		private LayoutInflater inflater;

		public PhotoActionsListAdapter()
		{
			inflater = LayoutInflater.from(getActivity().getApplicationContext());
		}

		@Override
		public int getCount()
		{
			return mTitles.length;
		}

		@Override
		public Object getItem(int position)
		{
			return null;
		}

		@Override
		public long getItemId(int position)
		{
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				convertView = inflater.inflate(R.layout.photos_action_list_item, null);

				PhotosOptionsViewHolder holder = new PhotosOptionsViewHolder();

				holder.titleTv = (TextView) convertView.findViewById(R.id.title);

				holder.descTv = (TextView) convertView.findViewById(R.id.description);

				holder.iconIv = (ImageView) convertView.findViewById(R.id.icon);

				convertView.setTag(holder);
			}

			PhotosOptionsViewHolder holder = (PhotosOptionsViewHolder) convertView.getTag();
			
			holder.titleTv.setText(mTitles[position]);
			
			holder.descTv.setText(mDescription[position]);
			
			holder.iconIv.setImageDrawable(getResources().getDrawable(itemIcons[position]));

			return convertView;
		}

		class PhotosOptionsViewHolder
		{
			TextView titleTv;

			TextView descTv;

			ImageView iconIv;
		}
	}
}
