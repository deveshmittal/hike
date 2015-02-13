package com.bsb.hike.offline;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;


public class ImageExplorer extends Fragment {
	 private Cursor cursor;
	    /*
	     * Column index for the Thumbnails Image IDs.
	     */
	private int columnIndex;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View imageexplorer = inflater.inflate(R.layout.imageexplorer, container, false);
        String[] projection = {MediaStore.Images.Media.DATA};
        cursor = getActivity().getContentResolver().query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, // Which columns to return
                null,       // Return all rows
                null,
                null);
        
        columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        ArrayList<String> al =  new ArrayList<String>();
        
        for(int i=0;i<cursor.getCount();i++)
        {
        	cursor.moveToPosition(i);
        	al.add(cursor.getString(columnIndex));	
        }
                
        return imageexplorer ;
	}
	
	
	
}



class CustomImageGrid extends BaseAdapter{
    private Context mContext;
    private final ArrayList<String> pathInfo;
      public CustomImageGrid(Context c, ArrayList<String> pathInfo) {
          mContext = c;
          this.pathInfo = pathInfo;
      }
    @Override
    public int getCount() {
      // TODO Auto-generated method stub
      return pathInfo.size();
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
          //textView.setText(mContext.getPackageManager().getApplicationLabel(appInfo.get(position)));
          //imageView.setImageDrawable(appInfo.get(position).loadIcon(mContext.getPackageManager()));
          
      return grid;
    }
}

