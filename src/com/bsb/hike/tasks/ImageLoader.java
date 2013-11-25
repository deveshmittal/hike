package com.bsb.hike.tasks;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.Utils;

public class ImageLoader {

	// --------------------------------------------------------------------------------------------
	// Fields
	// --------------------------------------------------------------------------------------------

	private HashMap<String, SoftReference<Bitmap>> imageMap = new HashMap<String, SoftReference<Bitmap>>();
	private ListItemQueue listItemQueue = new ListItemQueue();
	private Thread imageLoaderThread = new Thread(new ListItemQueueManager());
	private Context context;

	public ImageLoader(Context context) {
		this.context = context;
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void loadImage(String id, ImageView imageView) {
		loadImage(id, imageView, false);
	}

	public void loadImage(String id, ImageView imageView, boolean isScrolling) {
		ImageViewerInfo imageViewerInfo = (ImageViewerInfo) imageView.getTag();
		if (imageViewerInfo.isDefaultImage) {
			id = Utils.getDefaultAvatarServerName(id);
		}

		if (imageMap.containsKey(id) && imageMap.get(id).get() != null) {
			imageView.setImageBitmap(imageMap.get(id).get());
		} else {
			imageView.setImageResource(0);
			queueImage(id, imageView, isScrolling);
		}
	}

	public void interruptThread() {
		imageLoaderThread.interrupt();
	}

	private void queueImage(String id, ImageView imageView, boolean isScrolling) {
		listItemQueue.clearImageViewInstance(imageView);
		ListItemRef p = new ListItemRef(id, imageView, isScrolling);

		synchronized (listItemQueue.listItemRefs) {
			listItemQueue.listItemRefs.push(p);
			listItemQueue.listItemRefs.notifyAll();
		}

		if (imageLoaderThread.getState() == Thread.State.NEW)
			imageLoaderThread.start();
	}

	// --------------------------------------------------------------------------------------------
	// Inner Classes
	// --------------------------------------------------------------------------------------------

	private class ListItemRef {
		public String id;
		public ImageView imageView;
		public boolean isScrolling;

		public ListItemRef(String id, ImageView imageView, boolean isScrolling) {
			this.id = id;
			this.imageView = imageView;
			this.isScrolling = isScrolling;
		}
	}

	private class ListItemQueue {
		private Stack<ListItemRef> listItemRefs = new Stack<ListItemRef>();

		public void clearImageViewInstance(ImageView view) {

			for (int i = 0; i < listItemRefs.size();) {
				if (listItemRefs.get(i).imageView == view)
					listItemRefs.remove(i);
				else
					++i;
			}
		}
	}

	private class ListItemQueueManager implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					if (listItemQueue.listItemRefs.isEmpty()) {
						synchronized (listItemQueue.listItemRefs) {
							listItemQueue.listItemRefs.wait();
						}
					}

					if (!listItemQueue.listItemRefs.isEmpty()) {
						ListItemRef listItemToLoad;

						synchronized (listItemQueue.listItemRefs) {
							listItemToLoad = listItemQueue.listItemRefs.pop();
						}

						String basePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
								+ HikeConstants.PROFILE_ROOT;
						ImageViewerInfo tag = (ImageViewerInfo) listItemToLoad.imageView
								.getTag();

						Drawable drawable = IconCacheManager.getInstance()
								.getIconForMSISDN(tag.mappedId);
						ImageDisplayer imgDisplayer = new ImageDisplayer(
								drawable, listItemToLoad.imageView);

						Activity activity = (Activity) listItemToLoad.imageView
								.getContext();

						activity.runOnUiThread(imgDisplayer);

						if (!listItemToLoad.isScrolling && tag != null) {
							String id;
							if (tag.isDefaultImage) {
								id = Utils
										.getDefaultAvatarServerName(tag.mappedId);
							} else {
								id = tag.mappedId;
							}

							String fileName;
							if (tag.isDefaultImage) {
								fileName = listItemToLoad.id;
							} else {
								fileName = Utils
										.getProfileImageFileName(listItemToLoad.id);
							}

							File orgFile = new File(basePath, fileName);
							if (!orgFile.exists()) {
								continue;
							}

							File outputDir = context.getCacheDir();
							try {
								File tempFile = File.createTempFile(
										listItemToLoad.id, ".jpg", outputDir);

								Bitmap orgBitmap = BitmapFactory
										.decodeFile(orgFile.getPath());
								if (orgBitmap == null) {
									continue;
								}
								Utils.saveBitmapToFile(tempFile, orgBitmap,
										CompressFormat.JPEG, 50);
								Bitmap bmp = BitmapFactory.decodeFile(tempFile
										.getPath());

								tempFile.delete();

								imageMap.put(listItemToLoad.id,
										new SoftReference<Bitmap>(bmp));

								if (id.equals(listItemToLoad.id)) {
									ImageDisplayer bmpDisplayer = new ImageDisplayer(
											bmp, listItemToLoad.imageView);

									Activity a = (Activity) listItemToLoad.imageView
											.getContext();

									a.runOnUiThread(bmpDisplayer);
								}
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}

					if (Thread.interrupted())
						break;
				}
			} catch (InterruptedException e) {
			}
		}
	}

	private class ImageDisplayer implements Runnable {
		Bitmap bitmap;
		Drawable drawable;
		ImageView imageView;

		public ImageDisplayer(Bitmap bitmap, ImageView imageView) {
			this.bitmap = bitmap;
			this.imageView = imageView;
		}

		public ImageDisplayer(Drawable drawable, ImageView imageView) {
			this.drawable = drawable;
			this.imageView = imageView;
		}

		public void run() {
			imageView.setVisibility(View.VISIBLE);
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);
			} else if (drawable != null) {
				imageView.setImageDrawable(drawable);
			}
		}
	}
}