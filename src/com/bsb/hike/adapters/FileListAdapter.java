package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.FileListItem;
import com.bsb.hike.smartImageLoader.FileImageLoader;

public class FileListAdapter extends BaseAdapter
{
	private LayoutInflater inflater;

	private FileImageLoader fileImageLoader;

	private boolean isListFlinging;

	private ArrayList<FileListItem> items;

	private Map<String, FileListItem> selectedFileMap;

	public FileListAdapter(Context context, ArrayList<FileListItem> items)
	{
		inflater = LayoutInflater.from(context);
		int size = context.getResources().getDimensionPixelSize(R.dimen.file_thumbnail_size);
		fileImageLoader = new FileImageLoader(size, size);
		selectedFileMap = new HashMap<String, FileListItem>();
		this.items = items;
	}

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;
		fileImageLoader.setPauseWork(isListFlinging);

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}

	@Override
	public int getCount()
	{
		return items.size();
	}

	@Override
	public Object getItem(int position)
	{
		return items.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	public int getViewTypeCount()
	{
		return 2;
	}

	public int getItemViewType(int pos)
	{
		return items.get(pos).getSubtitle().length() > 0 ? 0 : 1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View v = convertView;
		FileListItem item = items.get(position);
		if (v == null)
		{
			v = inflater.inflate(R.layout.file_item, parent, false);
			if (item.getSubtitle().length() == 0)
			{
				v.findViewById(R.id.file_item_info).setVisibility(View.GONE);
			}
		}
		View selectorView = v.findViewById(R.id.selector_view);

		TextView typeTextView = (TextView) v.findViewById(R.id.file_item_type);
		((TextView) v.findViewById(R.id.file_item_title)).setText(item.getTitle());

		((TextView) v.findViewById(R.id.file_item_info)).setText(item.getSubtitle());
		ImageView imageView = (ImageView) v.findViewById(R.id.file_item_thumb);
		if (item.isShowThumbnail())
		{
			fileImageLoader.loadImage(FileImageLoader.FILE_KEY_PREFIX + item.getFile().getAbsolutePath(), imageView, isListFlinging);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setVisibility(View.VISIBLE);

			typeTextView.setText(item.getExtension().toUpperCase().substring(0, Math.min(item.getExtension().length(), 4)));
			typeTextView.setVisibility(View.VISIBLE);
		}
		else if (item.getIcon() != 0)
		{
			imageView.setImageResource(item.getIcon());
			imageView.setScaleType(ImageView.ScaleType.CENTER);
			imageView.setVisibility(View.VISIBLE);
			typeTextView.setVisibility(View.INVISIBLE);
		}
		else
		{
			typeTextView.setText(item.getExtension().toUpperCase().substring(0, Math.min(item.getExtension().length(), 4)));
			imageView.setVisibility(View.GONE);
			typeTextView.setVisibility(View.VISIBLE);
		}

		selectorView.setSelected(selectedFileMap.containsKey(item.getTitle()));

		return v;
	}
	
	public boolean isSelected(FileListItem item)
	{
		return selectedFileMap.containsKey(item.getTitle());
	}
	
	public void setSelected(FileListItem item, boolean selected)
	{
		if (selected)
		{
			selectedFileMap.put(item.getTitle(), item);
		}
		else
		{
			selectedFileMap.remove(item.getTitle());
		}
	}
	
	public Map<String, FileListItem> getSeletctedFileItems()
	{
		return selectedFileMap;
	}

	public void clearSelection()
	{
		selectedFileMap.clear();
	}
}