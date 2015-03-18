package com.bsb.hike.offline;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;

public class FileExplorer extends Activity{
	
	ArrayList<ApplicationInfo>  appInfo;
	public static int PORT = 8988;
	public static int APP_PATH = 5757;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filexplorer);
		
		
		List<PackageInfo> pi =  getPackageManager().getInstalledPackages(0);
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
		
		
		
  		CustomGrid adapter = new CustomGrid(FileExplorer.this, appInfo);
  		
	    GridView grid=(GridView)findViewById(R.id.grid);
	        grid.setAdapter(adapter);
	        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	               

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						String uri = null;
						uri = appInfo.get(position).sourceDir;
						Intent intent = getIntent();
						intent.putExtra(HikeConstants.Extras.EXTRAS_APK_PATH, uri);
						setResult(RESULT_OK, intent);
						finish();
					}
	            });
		
	}
	

	private boolean isUserApp(ApplicationInfo ai) {
		   int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
		   return (ai.flags & mask) == 0;
		}
	

}
