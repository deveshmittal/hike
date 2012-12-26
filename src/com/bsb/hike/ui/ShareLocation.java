package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ShareLocation extends MapActivity {

	private MapView myMap;
	private LocationManager locManager;
	private LocationListener locListener;

	private View currentSelection;
	private TextView locationAddress;
	private Button titleBtn;
	private TextView labelView;

	private GeoPoint selectedGeoPoint;
	/*
	 * The max distance the finger can move while placing a pointer
	 */
	private static final int MAX_DISTANCE = 20;

	private boolean gpsDialogShown = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.share_location);

		int zoomLevel = savedInstanceState != null ? savedInstanceState.getInt(
				HikeConstants.Extras.ZOOM_LEVEL,
				HikeConstants.DEFAULT_ZOOM_LEVEL)
				: HikeConstants.DEFAULT_ZOOM_LEVEL;
		initMap(zoomLevel);

		locationAddress = (TextView) findViewById(R.id.address);

		boolean isCustomLocation = savedInstanceState != null
				&& savedInstanceState
						.getBoolean(HikeConstants.Extras.CUSTOM_LOCATION_SELECTED);

		if (isCustomLocation) {
			if (savedInstanceState
					.containsKey(HikeConstants.Extras.CUSTOM_LOCATION_LAT)) {
				int lat = savedInstanceState
						.getInt(HikeConstants.Extras.CUSTOM_LOCATION_LAT);
				int longi = savedInstanceState
						.getInt(HikeConstants.Extras.CUSTOM_LOCATION_LONG);
				selectedGeoPoint = new GeoPoint(lat, longi);
				placeMarker(selectedGeoPoint);
			}
			currentSelection = findViewById(R.id.custom_position);
		} else {
			initMyLocationManager();

			gpsDialogShown = savedInstanceState != null
					&& savedInstanceState
							.getBoolean(HikeConstants.Extras.GPS_DIALOG_SHOWN);

			currentSelection = findViewById(R.id.my_position);

			boolean hasGps = getPackageManager().hasSystemFeature(
					PackageManager.FEATURE_LOCATION_GPS);
			/*
			 * Don't show this on orientation changes
			 */
			if (hasGps
					&& !gpsDialogShown
					&& !locManager
							.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						this);
				alertDialogBuilder
						.setMessage(R.string.gps_disabled)
						.setCancelable(false)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										gpsDialogShown = true;
										Intent callGPSSettingIntent = new Intent(
												android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
										startActivity(callGPSSettingIntent);
									}
								});
				alertDialogBuilder.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
				alertDialogBuilder.setCancelable(true);
				alertDialogBuilder.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						gpsDialogShown = true;
					}
				});
				AlertDialog alert = alertDialogBuilder.create();
				alert.show();
			}
		}

		titleBtn = (Button) findViewById(R.id.title_icon);
		labelView = (TextView) findViewById(R.id.title);

		findViewById(R.id.button_bar_2).setVisibility(View.VISIBLE);

		titleBtn.setText(R.string.send);
		titleBtn.setVisibility(View.VISIBLE);

		labelView.setText(R.string.share_location);

		currentSelection.setSelected(true);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.GPS_DIALOG_SHOWN,
				gpsDialogShown);
		outState.putBoolean(HikeConstants.Extras.CUSTOM_LOCATION_SELECTED,
				currentSelection.getId() == R.id.custom_position);
		if (currentSelection.getId() == R.id.custom_position
				&& selectedGeoPoint != null) {
			outState.putInt(HikeConstants.Extras.CUSTOM_LOCATION_LAT,
					selectedGeoPoint.getLatitudeE6());
			outState.putInt(HikeConstants.Extras.CUSTOM_LOCATION_LONG,
					selectedGeoPoint.getLongitudeE6());
		}
		outState.putInt(HikeConstants.Extras.ZOOM_LEVEL, myMap.getZoomLevel());
		super.onSaveInstanceState(outState);
	}

	public void onTitleIconClick(View v) {
		if (selectedGeoPoint == null) {
			Toast.makeText(getApplicationContext(), R.string.select_location,
					Toast.LENGTH_SHORT).show();
			return;
		}
		Intent result = new Intent();
		result.putExtra(HikeConstants.Extras.ZOOM_LEVEL, myMap.getZoomLevel());
		result.putExtra(HikeConstants.Extras.LATITUDE,
				selectedGeoPoint.getLatitudeE6() / 1E6);
		result.putExtra(HikeConstants.Extras.LONGITUDE,
				selectedGeoPoint.getLongitudeE6() / 1E6);
		setResult(RESULT_OK, result);

		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		removeMyLocationListeners();
		myMap.setBuiltInZoomControls(false);
	}

	/**
	 * Initialize the map.
	 */
	private void initMap(int zoomLevel) {
		myMap = (MapView) findViewById(R.id.map);
		myMap.setBuiltInZoomControls(true);
		myMap.getController().setZoom(zoomLevel);
		/*
		 * Adding this overlay to listen for touch events. Required when placing
		 * custom pointers.
		 */
		myMap.getOverlays().add(new CustomPointerOverlay());
	}

	/**
	 * Initialize the location manager to update us with the current location.
	 */
	private void initMyLocationManager() {
		locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		locListener = new LocationListener() {
			public void onLocationChanged(Location newLocation) {
				createAndShowMyItemizedOverlay(newLocation);
			}

			public void onProviderDisabled(String arg0) {
			}

			public void onProviderEnabled(String arg0) {
			}

			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			}
		};
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
				locListener);
		locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
				0, locListener);

		if (locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
			createAndShowMyItemizedOverlay(locManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER));
		} else if (locManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
			createAndShowMyItemizedOverlay(locManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
		}
	}

	/**
	 * Removes the listener that updates us with the current location.
	 */
	private void removeMyLocationListeners() {
		if (locManager != null) {
			locManager.removeUpdates(locListener);
		}
	}

	/**
	 * This method will be called whenever a change of the current position is
	 * submitted via the GPS.
	 * 
	 * @param newLocation
	 */
	protected void createAndShowMyItemizedOverlay(Location newLocation) {
		// transform the location to a geopoint
		GeoPoint geoPoint = new GeoPoint(
				(int) (newLocation.getLatitude() * 1E6),
				(int) (newLocation.getLongitude() * 1E6));

		placeMarker(geoPoint);
	}

	/**
	 * Remove the older overlay. Ideally there should only be one.
	 */
	private void removeOlderOverlays() {
		selectedGeoPoint = null;
		List<Overlay> overlays = myMap.getOverlays();

		if (!overlays.isEmpty()) {
			for (Iterator<Overlay> iterator = overlays.iterator(); iterator
					.hasNext();) {
				if (!(iterator.next() instanceof CustomPointerOverlay)) {
					iterator.remove();
				}
			}
		}

		myMap.postInvalidate();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private class MyItemizedOverlay extends ItemizedOverlay<OverlayItem> {
		private List<OverlayItem> items;
		private Drawable marker;

		public MyItemizedOverlay(Drawable defaultMarker) {
			super(defaultMarker);
			items = new ArrayList<OverlayItem>();
			marker = defaultMarker;
		}

		@Override
		protected OverlayItem createItem(int index) {
			return items.get(index);
		}

		@Override
		public int size() {
			return items.size();
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			boundCenterBottom(marker);
			super.draw(canvas, mapView, shadow);
		}

		public void addItem(OverlayItem item) {
			items.add(item);
			populate();
		}

	}

	public class CustomPointerOverlay extends Overlay {

		int initialPosY;
		int initialPosX;

		int deltaX;
		int deltaY;

		int actX;
		int actY;

		long firstTapTime;
		long secondTapTime;

		Handler markerHandler = new Handler();
		Runnable placeMarkerRunnable = new Runnable() {

			@Override
			public void run() {
				/*
				 * We are accounting for movement of the finger by a small
				 * amount.
				 */
				boolean showShowMarker = (deltaX < MAX_DISTANCE)
						&& (deltaY < MAX_DISTANCE);
				if (currentSelection.getId() == R.id.custom_position
						&& showShowMarker) {
					final GeoPoint geoPoint = myMap.getProjection().fromPixels(
							actX, actY);

					placeMarker(geoPoint);
				}
			}
		};

		@Override
		public boolean onTouchEvent(MotionEvent e, MapView mapView) {
			switch (e.getAction()) {
			case MotionEvent.ACTION_UP:
				firstTapTime = System.currentTimeMillis();

				actX = (int) e.getX();
				actY = (int) e.getY();

				if (Math.abs(firstTapTime - secondTapTime) > 200L) {
					deltaX = (int) Math.abs(initialPosX - e.getX());

					deltaY = (int) Math.abs(initialPosY - e.getY());

					markerHandler.postDelayed(placeMarkerRunnable, 250);
				} else {
					markerHandler.removeCallbacks(placeMarkerRunnable);
					MapController mapController = myMap.getController();
					mapController.animateTo(myMap.getProjection().fromPixels(
							actX, actY));
					mapController.zoomIn();
				}
				secondTapTime = firstTapTime;
				return false;
			case MotionEvent.ACTION_DOWN:
				initialPosX = (int) e.getX();
				initialPosY = (int) e.getY();
				return false;
			}
			return false;
		}
	}

	public void onChangeMarkerClicked(View v) {
		if (v.getId() != currentSelection.getId()) {
			// first remove old overlay
			removeOlderOverlays();
			if (v.getId() == R.id.my_position) {
				initMyLocationManager();
			} else {
				removeMyLocationListeners();
				Toast.makeText(this, R.string.tap_to_place, Toast.LENGTH_SHORT)
						.show();
			}
			currentSelection.setSelected(false);
			currentSelection = v;
			currentSelection.setSelected(true);
		} else {
			if (selectedGeoPoint != null) {
				myMap.getController().animateTo(selectedGeoPoint);
			}
		}
	}

	/**
	 * Called when we want to place a marker on the map.
	 * 
	 * @param geoPoint
	 *            Where the marker should be placed.
	 */
	private void placeMarker(GeoPoint geoPoint) {
		removeOlderOverlays();

		Drawable icon = getResources().getDrawable(R.drawable.ic_marker);
		icon.setBounds(0, 0, icon.getIntrinsicWidth(),
				icon.getIntrinsicHeight());

		// create my overlay and show it
		MyItemizedOverlay overlay = new MyItemizedOverlay(icon);
		OverlayItem item = new OverlayItem(geoPoint, "My Location", null);
		overlay.addItem(item);
		myMap.getOverlays().add(overlay);

		myMap.invalidate();
		myMap.dispatchTouchEvent(MotionEvent.obtain(1, 1,
				MotionEvent.ACTION_DOWN, 0, 0, 0));

		myMap.getController().animateTo(geoPoint);
		getAddress(geoPoint);

		selectedGeoPoint = geoPoint;
	}

	/**
	 * Get the address of the current pointer
	 * 
	 * @param geoPoint
	 */
	private void getAddress(final GeoPoint geoPoint) {
		/*
		 * Getting the address blocks the UI so we run this code in a background
		 * thread.
		 */
		(new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				return Utils.getAddressFromGeoPoint(geoPoint,
						ShareLocation.this);
			}

			@Override
			protected void onPostExecute(String result) {
				locationAddress.setText(result);
			}
		}).execute();
	}
}
