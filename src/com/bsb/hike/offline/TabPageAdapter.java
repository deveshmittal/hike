package com.bsb.hike.offline;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;


public class TabPageAdapter extends FragmentStatePagerAdapter {

	public TabPageAdapter(FragmentManager fm) {
		super(fm);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Fragment getItem(int i) {
		// TODO Auto-generated method stub
		switch (i) {
        case 0:
            //Fragement for Apks  Tab
            return new ApkExplorer();
        case 1:
           //Fragment for Images Tab
            return new ImageExplorer();
        }
		return null;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 2;
	}

}
