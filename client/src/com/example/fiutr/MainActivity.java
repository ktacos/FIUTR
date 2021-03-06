package com.example.fiutr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.net.wifi.ScanResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends Activity implements LocationListener
{
	
	private GoogleMap googleMap;
	private CameraPosition camPos;
	private GPSHandler gpsHandler;
	private WiFiHandler wifiHandler;
	private final ArrayList<LocationNetwork> wifiGPS = new ArrayList<LocationNetwork>();
	private final ArrayList<Marker> markerList = new ArrayList<Marker>();
	private static String filePath;
	private AsyncTimer updateTimer;
	private float zoomLevel = 15f;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		filePath = getFilesDir().toString()+"/gpsmaps.txt";
		System.out.println("The file path is: "+filePath);
		gpsHandler = new GPSHandler(this);
		wifiHandler = new WiFiHandler(this);
		gpsHandler.updateLocation();

		try
		{
			initializeMap();
			startThread();
			parseFile();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		Intent intent;
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_search:
			intent = new Intent(this, SearchActivity.class);
			intent.putExtra("FILE_PATH",filePath);
			startActivity(intent);
			return true;
		case R.id.action_scan:
			intent = new Intent(this, ScanActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_viewAll:
			intent = new Intent(this, ViewAllActivity.class);
			intent.putExtra("FILE_PATH",filePath);
			intent.putExtra("BOOL_VIEW_ALL",true); // Viewing all data
			startActivity(intent);
			return true;
		case R.id.action_about:
			intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		gpsHandler.disable(this);
		stopThread();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		gpsHandler.enable(this);
		updateMap();
		startThread();
	}
	
	private void initializeMap()
	{
		if(googleMap == null)
		{
			googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
			googleMap.setOnCameraChangeListener(getCameraChangeListener());
			updateMap();
		}
		if(googleMap == null)
		{
			Toast.makeText(getApplicationContext(), "Unable to create maps!", Toast.LENGTH_SHORT).show();
		}
	}
	private void updateMap()
	{
		if(gpsHandler.updateLocation())
		{
			removeMarker("Current Location");
			camPos = new CameraPosition.Builder()
				.target(new LatLng(gpsHandler.getLat(),gpsHandler.getLon()))
				.zoom(zoomLevel)
				.build();
			googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
			addMarker("Current Location", new LatLng(gpsHandler.getLat(),gpsHandler.getLon()));
			processWiFiLocations(wifiHandler.getWifiNetworks(),gpsHandler.getLat(), gpsHandler.getLon());
		}
		else
		{
			Toast.makeText(this, "Unable to update GPS Position!", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void processWiFiLocations(List<ScanResult> wifiNetworks, double latitude, double longitude)
	{
		for(ScanResult result : wifiNetworks)
		{
			// Check to see if it is already in there. If so, remove it.
			removeMarker(result.SSID);
			// Add it to our array of LocationNetworks, and to the map.
			wifiGPS.add(new LocationNetwork(result, latitude, longitude));
			Network temp = new Network(result.SSID,result.level,latitude, longitude,"");
			addMarker(temp,true);
		}
	}
	
	public void addMarker(String title, LatLng loc)
	{
		Marker marker = googleMap.addMarker(new MarkerOptions()
								 .position(loc)
								 .title(title)
								 .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
		markerList.add(marker);
	}
	
	public void addMarker(Network network, boolean report)
	{
		Marker marker = googleMap.addMarker(new MarkerOptions()
								 .position(network.getLocation())
								 .title(network.getName())
								 .snippet("Bars: "+network.getBars())
								 .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
		if(!markerList.contains(marker))
			markerList.add(marker);
		if (report)
		{
			ServerHandler handler = new ServerHandler();
			handler.addNetwork(network);
		}
	}
	
	public void removeMarker(String title)
	{
		Iterator<Marker> i = markerList.iterator();
		while(i.hasNext())
		{
			Marker currentItem = i.next();
			if(currentItem.getTitle().equals(title))
			{
				currentItem.remove();
			}
		}
	}
	
	@Override
	public void onLocationChanged(Location location)
	{
		camPos = new CameraPosition.Builder()
			.target(new LatLng(location.getLatitude(),location.getLongitude()))
			.zoom(zoomLevel)
			.build();
		googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
	}
	
	public void parseFile()
	{
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line;
			while((line = reader.readLine()) != null)
			{
				if(line.contains("|"))
				{
					System.out.println("Parsing line: [ "+line+" ]");
					// Format is: NAMEOFNETWORK|SIGNAL STRENGTH|LATITUDE|LONGITUDE
					String[] parsedLine = line.split("\\|");
					Network currentNetwork = new Network(parsedLine[0],Integer.parseInt(parsedLine[1]),Double.parseDouble(parsedLine[2]),Double.parseDouble(parsedLine[3]),"");
					addMarker(currentNetwork, false);
				}
			}
			reader.close();
		}
		catch (FileNotFoundException e)
		{
			try
			{
				System.err.println("Creating a new file!\n");
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "utf-8"));
				writer.close();
				parseFile();
			}
			catch (Exception f)
			{
				f.printStackTrace();
			}
			
		}
		catch (Exception e)
		{
			System.err.println("Unable to parse file for networks!\n");
			e.printStackTrace();
		}
		return;
	}
	
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		
	}
	
	@Override
	public void onProviderEnabled(String provider)
	{
		
	}
	
	@Override
	public void onProviderDisabled(String provider)
	{
		
	}

	public void startThread()
	{
		Toast.makeText(this, "Starting thread", Toast.LENGTH_LONG);
		updateTimer = new AsyncTimer();
		updateTimer.execute();
	}
	
	public void stopThread()
	{
		updateTimer.cancel(true);
	}
	
	public class AsyncTimer extends AsyncTask<Void,Integer,Boolean>{
	    private boolean isRunning;
	    private boolean stop;
	    
	    @Override
	    protected Boolean doInBackground(Void... arg0) {
	        isRunning = true;

	        while(isRunning)
	        {
	            try {
	                Thread.sleep(3000);
	            } catch (InterruptedException e) {
	                Log.e("Thread Interrupted", e.getMessage());
	            }
	            runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	processWiFiLocations(wifiHandler.getWifiNetworks(),gpsHandler.getLat(), gpsHandler.getLon());
                    }
                });
	        }
	        if(stop==false)
	            return true;
	        else
	            return false;
	    }

	    @Override
	    protected void onCancelled() {
	        stop = true;
	        isRunning = false;
	    }
	
	    public boolean getIsRunning()			
	    {			
	        return isRunning;			
	    }
	}
	
	public OnCameraChangeListener getCameraChangeListener()
	{
	    return new OnCameraChangeListener() 
	    {
	        @Override
	        public void onCameraChange(CameraPosition position) 
	        {
	        	if(position.zoom != zoomLevel)
	        	{
	        		zoomLevel = position.zoom;
	        	}
	        }
	    };
	}

}
