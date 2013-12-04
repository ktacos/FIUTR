package com.example.fiutr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.ListActivity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * ViewAllActivity creates a page which displays a list of networks. This is used for viewing
 * all of the networks in the database or for viewing a list of networks when performing a search.
 */
public class ViewAllActivity extends ListActivity {

	NetworkAdapterItem adapter;
	ArrayList<Network> networkList = new ArrayList<Network>();
	private String filePath;
	private String pageTitle; // title of page depends on whether you are viewing all or searching
	private boolean viewAllData;
	
	/**
	 * Method automatically called with the ViewAllActivity page is created.
	 * It sets the activity_viewall layout containing a list of the networks 
	 * to be displayed
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewall);	
		
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{
			viewAllData = extras.getBoolean("BOOL_VIEW_ALL");
			filePath = extras.getString("FILE_PATH");
			
			if (viewAllData)
				setTitle("View All");
			else
				setTitle("Search Results");
			
		}
		
		checkFileForDuplicates();
		getData();
		adapter = new NetworkAdapterItem(this, R.layout.list_viewall, networkList);
		setListAdapter(adapter);
		
		
		
		// Make sure we're running on Honeycomb or higher to use ActionBar APIs
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	/*
	 * Read in all of the data from the text file and display it
	 */
	public void getData()
	{
		int distance = SearchActivity.getDistance();
		int signalStrength = SearchActivity.getSignalStrength();
		int numResults = SearchActivity.getNumResults();
		int listedResults = 0; // keep track of total results listed
		
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line;
			while((line = reader.readLine()) != null)
			{
				if(line.contains("|"))
				{
					System.out.println("Parsing line: [ "+line+" ]");
					// Format is: NAMEOFNETWORK|INFO_OF_NETWORK|LATITUDE|LONGITUDE
					String[] parsedLine = line.split("\\|");
					
					// Viewing all data
					if (viewAllData)
					{
					  networkList.add(new Network(parsedLine[0], parsedLine[1], Double.parseDouble(parsedLine[2]), Double.parseDouble(parsedLine[3])));
					}
					
					// View only data pertaining to search result
					else
					{
						if (distance > 20 // Get network distance
								&& signalStrength > 4 // get network strength
								&& numResults > listedResults)
						{
							
							networkList.add(new Network(parsedLine[0], parsedLine[1], Double.parseDouble(parsedLine[2]), Double.parseDouble(parsedLine[3])));
							listedResults++; // increment total number of results added to list
						}
					}
				}
				
				else
				{
					System.out.println("Caught blankspace!");
				}
			}
			reader.close();
		}
		catch (Exception e)
		{
			System.err.println("Unable to populate view all!\n");
			e.printStackTrace();
		}
	}

	/**
	 * The user will return to the home page when the back button
	 * at the top of the screen is pressed
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * This can be removed later
	 */
	public void checkFileForDuplicates()
	{
		 Toast.makeText(this,"Path" + filePath,Toast.LENGTH_SHORT).show();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			Set<String> lines = new LinkedHashSet<String>(10000);
			String line;
			while ((line = reader.readLine()) != null)
			{
				lines.add(line);
			}
			reader.close();
			BufferedWriter tempWriter = new BufferedWriter(new FileWriter(filePath));
			for(String uniqueLines : lines)
			{
				tempWriter.write(uniqueLines);
				tempWriter.newLine();
			}
			tempWriter.close();
		}
		catch (FileNotFoundException e)
		{
			try
			{
				System.err.println("Creating a new file!\n");
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "utf-8"));
				writer.close();
				checkFileForDuplicates();
			}
			catch (Exception f)
			{
				f.printStackTrace();
			}
			
		}
		catch (Exception e)
		{
			System.err.println("Unable to check file for duplicates!\n");
			e.printStackTrace();
		}
	}
}
