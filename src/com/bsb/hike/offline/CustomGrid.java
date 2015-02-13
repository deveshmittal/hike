package com.bsb.hike.offline;
import java.util.ArrayList;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
public class CustomGrid extends BaseAdapter{
    private Context mContext;
    private final ArrayList<ApplicationInfo> appInfo;
      public CustomGrid(Context c,ArrayList<ApplicationInfo> appInfo) {
          mContext = c;
          this.appInfo = appInfo;
      }
    @Override
    public int getCount() {
      // TODO Auto-generated method stub
      return appInfo.size();
    }
    @Override
    public Object getItem(int position) {
      // TODO Auto-generated method stub
      return null;
    }
    @Override
    public long getItemId(int position) {
      // TODO Auto-generated method stub
      return 0;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // TODO Auto-generated method stub
      View grid;
      LayoutInflater inflater = (LayoutInflater) mContext
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          if (convertView == null) {
            grid = new View(mContext);
            grid = inflater.inflate(R.layout.grid_single, null);
            
          } else {
            grid = (View) convertView;
          }
          TextView textView = (TextView) grid.findViewById(R.id.grid_text);
          ImageView imageView = (ImageView)grid.findViewById(R.id.grid_image);
          textView.setText(mContext.getPackageManager().getApplicationLabel(appInfo.get(position)));
          imageView.setImageDrawable(appInfo.get(position).loadIcon(mContext.getPackageManager()));
          
      return grid;
    }
}