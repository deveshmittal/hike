package com.bsb.hike.ui;

import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter.ViewBinder;

import com.bsb.hike.R;

public class DropDownViewBinder implements ViewBinder
{

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex)
	{
		if (view.getId() == R.id.onhike)
		{
			view.setBackgroundResource(cursor.getInt(columnIndex) == 0 ? R.drawable.ic_sms_user : R.drawable.ic_hike_user);
			return true;
		}
		return false;
	}

}
