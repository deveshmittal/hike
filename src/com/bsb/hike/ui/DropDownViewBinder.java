package com.bsb.hike.ui;

import com.bsb.hike.R;

import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class DropDownViewBinder implements ViewBinder {

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (view.getId() == R.id.onhike) {
            view.setVisibility(cursor.getInt(columnIndex) == 0 ? View.INVISIBLE : View.VISIBLE);
            return true;
        }
        return false;
    }

}
