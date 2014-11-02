package com.bsb.hike.adapters;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.utils.Utils;

public class StickerShopAdapter extends CursorAdapter
{
	private LayoutInflater layoutInflater;

	private Context context;

	private StickerLoader stickerLoader;
	
	private boolean isListFlinging;

	private int idColoumn;

	private int categoryNameColoumn;

	private int totalStickersCountColoumn;

	private int categorySizeColoumn;

	class ViewHolder
	{
		TextView categoryName;

		TextView totalStickers;

		TextView stickersPackDetails;

		ImageView downloadState;
	}

	public StickerShopAdapter(Context context, Cursor cursor)
	{
		super(context, cursor, false);
		this.context = context;
		this.layoutInflater = LayoutInflater.from(context);
		this.stickerLoader = new StickerLoader(context);
		this.idColoumn = cursor.getColumnIndex(DBConstants._ID);
		this.categoryNameColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_NAME);
		this.totalStickersCountColoumn = cursor.getColumnIndex(DBConstants.TOTAL_NUMBER);
		this.categorySizeColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_SIZE);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		View v = layoutInflater.inflate(R.layout.sticker_shop_list_item, parent, false);
		ViewHolder viewholder = new ViewHolder();
		viewholder.categoryName = (TextView) v.findViewById(R.id.category_name);
		viewholder.stickersPackDetails = (TextView) v.findViewById(R.id.pack_details);
		viewholder.downloadState = (ImageView) v.findViewById(R.id.category_download_btn);
		v.setTag(viewholder);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		ViewHolder viewholder = (ViewHolder) view.getTag();

		String categoryId = cursor.getString(idColoumn);
		int totalStickerCount = cursor.getInt(totalStickersCountColoumn);
		int categorySizeInBytes = cursor.getInt(categorySizeColoumn);
		viewholder.categoryName.setText(cursor.getString(categoryNameColoumn));

		if(totalStickerCount > 0)
		{
			String detailsStirng = context.getResources().getString(R.string.n_stickers,
				totalStickerCount);
			if(categorySizeInBytes>0)
			{
				detailsStirng  += ", " + Utils.getSizeForDisplay(categorySizeInBytes);
			}
			viewholder.stickersPackDetails.setVisibility(View.VISIBLE);
			viewholder.stickersPackDetails.setText(detailsStirng);
		}
		else
		{
			viewholder.stickersPackDetails.setVisibility(View.GONE);
		}
	}
	

	public StickerLoader getStickerLoader()
	{
		return stickerLoader;
	}
	
	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;
		isListFlinging = b;
		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}

}
