package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;

public class HikeArrayAdapter extends ArrayAdapter<ContactInfo> implements SectionIndexer
{
	private Context context;
	private HashMap<String, Integer> alphaIndexer;
	private String[] sections;

	public HikeArrayAdapter(Context context, int inviteItem, List<ContactInfo> contacts)
	{
		super(context, inviteItem, contacts);
		this.context = context;

        int size = contacts.size();
		alphaIndexer = new HashMap<String, Integer>(size);

		for(int i = 0; i < size; ++i)
		{
			ContactInfo contactInfo = contacts.get(i);
			String c = contactInfo.getName().substring(0,1).toUpperCase();
            alphaIndexer.put(c, i);
		}

        Set<String> sectionLetters = alphaIndexer.keySet();

        ArrayList<String> sectionList = new ArrayList<String>(sectionLetters); 

        Collections.sort(sectionList);

        sections = new String[sectionList.size()];

        sectionList.toArray(sections);
	}

	public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent)
	{
		ContactInfo contactInfo = getItem(position);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(R.layout.invite_item, parent, false);
		}

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());

		Button button = (Button) v.findViewById(R.id.invite_button);
		button.setEnabled(!contactInfo.isOnhike());

		return v;
	};

    public int getPositionForSection(int section) {
    	Log.d("MessagesList", "alphaIndexer " + alphaIndexer);
    	Log.d("MessagesList", "sections " + sections);
        return alphaIndexer.get(sections[section]);
    }

    public int getSectionForPosition(int position) {
        return 1;
    }

    public Object[] getSections() {
         return sections;
    }
}
