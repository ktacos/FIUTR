package com.example.fiutr;

import android.app.AlertDialog;
import android.content.*;
import android.location.*;
import android.widget.*;

/**
 * GPSHandler is used for handling GPS connections.
 */
public class GPSHandler {
	
	private double latitude;
	private double longitude;
	private LocationManager locMan;
	private Context gpsContext;
	private String provider;
	private Criteria gpsCriteria;
	
	/**
	 * Constructor to create the GPSHandler
	 */
	public GPSHandler(Context subContext)
	{
		gpsContext = subContext;
		locMan = (LocationManager) gpsContext.getSystemService(gpsContext.LOCATION_SERVICE);
		
		// If the LocationManager.NETWORK_PROVIDER is not available
		if(!locMan.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			// Display a dialog to allow the user to enable location settings
			AlertDialog.Builder builder = new AlertDialog.Builder(gpsContext);
			builder.setMessage("GPS is disabled. Do you want to enable it?")
				   .setCancelable(false)
				   
				   .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					  /**
				       * Enables the location settings if the user pressed the "yes" button on the popup dialog
					   */
					   public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id)
					   {
						   gpsContext.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					   }
				   })
				   .setNegativeButton("No", new DialogInterface.OnClickListener() {
					   /**
					   * Closes the pop-up dialog if the user pressed the "no" button
					   */
					   public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id)
					   {
						   dialog.cancel();
					   }
				   });
			final AlertDialog alert = builder.create();
			alert.show();
		}
	    gpsCriteria = new Criteria();
		provider = locMan.getBestProvider(gpsCriteria, false);
	}
	
	/**
	 * Update the GPS location (latitude and longitude values are updated)
	 */
	public boolean updateLocation()
	{
		Location location = locMan.getLastKnownLocation(provider);
		if(location != null)
		{
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			return true;
		}
		else
			return false;
	}
	/**
	 * Returns the GPS latitude
	 */
	public double getLat()
	{
		return latitude;
	}
	
	/**
	 * Returns the GPS longitude
	 */
	public double getLon()
	{
		return longitude;
	}
	
	/**
	 * Update the GPS location
	 * @param lat The latitude value
	 * @param lon The longitude value
	 */
	public void updateLocation(double lat, double lon)
	{
		latitude = lat;
		longitude = lon;
	}
	
	/**
	 * Disable a particular listener
	 */
	public void disable(LocationListener listener)
	{
		locMan.removeUpdates(listener);
	}
	
	/**
	 * Enable a particular listener
	 */
	public void enable(LocationListener listener)
	{
		locMan.requestLocationUpdates(provider,400,1,listener);
		updateLocation();
	}
	
}