package com.bsb.hike.utils;

import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class ImageLoader {

	// --------------------------------------------------------------------------------------------
	// Fields
	// --------------------------------------------------------------------------------------------

	private HashMap<String, SoftReference<Bitmap>> imageMap = new HashMap<String, SoftReference<Bitmap>>();
	private ListItemQueue listItemQueue = new ListItemQueue();
	private Thread imageLoaderThread = new Thread(new ListItemQueueManager());

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void loadImage(String url, ImageView imageView) {
		if (imageMap.containsKey(url) && imageMap.get(url).get() != null) {
			imageView.setImageBitmap(imageMap.get(url).get());

		} else {
			queueImage(url, imageView);
		}
	}

	private void queueImage(String url, ImageView imageView) {
		listItemQueue.clearImageViewInstance(imageView);
		ListItemRef p = new ListItemRef(url, imageView);

		synchronized (listItemQueue.listItemRefs) {
			listItemQueue.listItemRefs.push(p);
			listItemQueue.listItemRefs.notifyAll();
		}

		if (imageLoaderThread.getState() == Thread.State.NEW)
			imageLoaderThread.start();
	}

	private Bitmap fetchBitmap(String url) {
		try {
			Bitmap bitmap = BitmapFactory.decodeStream(new URL(url)
					.openConnection().getInputStream());
			return bitmap;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Inner Classes
	// --------------------------------------------------------------------------------------------

	private class ListItemRef {
		public String url;
		public ImageView imageView;

		public ListItemRef(String url, ImageView imageView) {
			this.url = url;
			this.imageView = imageView;
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
					if (listItemQueue.listItemRefs.size() == 0) {
						synchronized (listItemQueue.listItemRefs) {
							listItemQueue.listItemRefs.wait();
						}
					}

					if (listItemQueue.listItemRefs.size() != 0) {
						ListItemRef listItemToLoad;

						synchronized (listItemQueue.listItemRefs) {
							listItemToLoad = listItemQueue.listItemRefs.pop();
						}

						Bitmap bmp = fetchBitmap(listItemToLoad.url);
						imageMap.put(listItemToLoad.url,
								new SoftReference<Bitmap>(bmp));
						Object tag = listItemToLoad.imageView.getTag();

						if (tag != null
								&& ((String) tag).equals(listItemToLoad.url)) {
							BitmapDisplayer bmpDisplayer = new BitmapDisplayer(
									bmp, listItemToLoad.imageView);

							Activity a = (Activity) listItemToLoad.imageView
									.getContext();

							a.runOnUiThread(bmpDisplayer);
						}
					}

					if (Thread.interrupted())
						break;
				}
			} catch (InterruptedException e) {
			}
		}
	}

	private class BitmapDisplayer implements Runnable {
		Bitmap bitmap;
		ImageView imageView;;

		public BitmapDisplayer(Bitmap bitmap, ImageView imageView) {
			this.bitmap = bitmap;
			this.imageView = imageView;
		}

		public void run() {
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);
			}
		}
	}
}