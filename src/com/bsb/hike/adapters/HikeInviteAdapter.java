package com.bsb.hike.adapters;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.Utils;

public class HikeInviteAdapter extends HikeArrayAdapter implements OnClickListener
{

	public <T> HikeInviteAdapter(Activity activity, int viewItemId, List<T> items)
	{
		super(activity, viewItemId, items);
		this.activity = activity;
	}

	@Override
	protected View getItemView(int position, View convertView, ViewGroup parent)
	{
		{
			ContactInfo contactInfo = (ContactInfo) getItem(position);
			LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = convertView;
			if (v == null)
			{
				v = inflater.inflate(R.layout.invite_item, parent, false);
			}

			TextView textView = (TextView) v.findViewById(R.id.name);
			textView.setText(contactInfo.getName());

			Button button = (Button) v.findViewById(R.id.invite_button);
			button.setTag(contactInfo);
			button.setEnabled(!contactInfo.isOnhike());
			button.setOnClickListener(this);

			boolean no_dividers = ((position == getCount() - 1) ||
									(getItem(position+1) instanceof Section));
			View divider = v.findViewById(R.id.item_divider);
			divider.setVisibility(no_dividers ? View.INVISIBLE : View.VISIBLE);

			return v;
		}
	}

	@Override
	public void onClick(View view)
	{
		ContactInfo info = (ContactInfo) view.getTag();
		activity.setResult(Activity.RESULT_OK, Utils.createIntentFromContactInfo(info));
		activity.finish();
	}

}
