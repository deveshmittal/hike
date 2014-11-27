package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.R;

import android.content.Context;
import android.view.View;

public class AttachmentPicker extends OverFlowMenuLayout {

	public static final int CAMERA = 313;
	public static final int GALLARY = 314;
	public static final int AUDIO = 315;
	public static final int VIDEO = 316;
	public static final int FILE = 317;
	public static final int CONTACT = 318;
	public static final int LOCATOIN = 318;

	private boolean startRespectiveActivities;

	/**
	 * 
	 * @param overflowItems
	 * @param listener
	 * @param context
	 * @param startRespectiveActivities
	 *            - if true, we will start respective activities on activity
	 *            behalf and activity has to handle onActivityResult callback
	 *            where request code is Overflowitem uniqueness
	 */
	public AttachmentPicker(List<OverFlowMenuItem> overflowItems,
			OverflowItemClickListener listener, Context context,
			boolean startRespectiveActivities) {
		super(overflowItems, listener, context);
		this.startRespectiveActivities = startRespectiveActivities;
	}

	/**
	 * By default we show {@link #CAMERA} {@link #GALLARY} {@link #AUDIO}
	 * {@link #VIDEO} {@link #FILE} {@link #CONTACT} {@link #LOCATOIN}
	 * 
	 * @param listener
	 * @param context
	 * @param startRespectiveActivities
	 *            - if true, we will start respective activities on activity
	 *            behalf and activity has to handle onActivityResult callback
	 *            and request code will be constants given above
	 */
	public AttachmentPicker(OverflowItemClickListener listener,
			Context context, boolean startRespectiveActivities) {
		this(null, listener, context, startRespectiveActivities);

	}

	private void initDefaultAttachmentList() {
		List<OverFlowMenuItem> items = new ArrayList<OverFlowMenuItem>(7);
		items.add(new OverFlowMenuItem(getString(R.string.camera_upper_case),
				0, R.drawable.ic_attach_camera, CAMERA));
		items.add(new OverFlowMenuItem(getString(R.string.photo), 0,
				R.drawable.ic_attach_pic, GALLARY));
		items.add(new OverFlowMenuItem(getString(R.string.audio), 0,
				R.drawable.ic_attach_music, AUDIO));
		items.add(new OverFlowMenuItem(getString(R.string.video), 0,
				R.drawable.ic_attach_video, VIDEO));
		items.add(new OverFlowMenuItem(getString(R.string.file), 0,
				R.drawable.ic_attach_file, FILE));
		this.overflowItems = items;
	}

	@Override
	public View getView() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initView() {
		// viewToShow;

	}

	private String getString(int id) {
		return context.getString(id);
	}
}
