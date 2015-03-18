package com.bsb.hike.offline;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;

public class ApkExplorer extends Fragment {
	
	ArrayList<ApplicationInfo>  appInfo;
	public static int PORT = 8988;
	public static int APP_PATH = 5757;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View apkexplorer = inflater.inflate(R.layout.apkexplorer, container, false);
        //((TextView)android.findViewById(R.id.textView)).setText("Android");
        List<PackageInfo> pi =  getActivity().getPackageManager().getInstalledPackages(0);
		appInfo = new ArrayList<ApplicationInfo>();
		//ArrayList<String> pnames =  new ArrayList<String>();
		//ArrayList<Drawable> drawables = new ArrayList<Drawable>();
  		for(int i=0;i<pi.size();i++)
		{
			   PackageInfo p1 =  pi.get(i);
			   if(isUserApp(p1.applicationInfo))
			   {
			   		appInfo.add(p1.applicationInfo);	
			      //pnames.add((String) this.getPackageManager().getApplicationLabel(p1.applicationInfo));
			      //drawables.add(p1.applicationInfo.loadIcon(getPackageManager()));
				      
			   }
		}
		
		
		
  		CustomGrid adapter = new CustomGrid(getActivity(), appInfo);
  		
	    GridView grid=(GridView)apkexplorer.findViewById(R.id.grid);
	        grid.setAdapter(adapter);
	        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	               

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						String uri = null;
						uri = appInfo.get(position).sourceDir;
						Intent intent = getActivity().getIntent();
						intent.putExtra(HikeConstants.Extras.EXTRAS_APK_PATH, uri);
						getActivity().setResult(getActivity().RESULT_OK, intent);
						getActivity().finish();
					}
	            });
	        
        return apkexplorer;
	}
	
	
	

	private boolean isUserApp(ApplicationInfo ai) {
		   int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
		   return (ai.flags & mask) == 0;
		}
	

	

}
