package com.normsstuff.maps4norm;
/*  Change history:
 *   3/11/15 - added marker on Search for location
 *   3/4/17 - added Add to route
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.SphericalUtil;

import com.normsstuff.waypoints.WayPoint;
import com.normsstuff.waypoints.WayPointFileIO;
import com.normsstuff.waypoints.WayPointFileIO.FileWriterClass;
import com.normstools.SaveStdOutput;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.PorterDuff;


public class MyMapActivity extends FragmentActivity {
	final String Version = "Version date: March 13, 2017 @ 1340\n";
	
	final int[] SomeClrs = {Color.RED, Color.BLUE, Color.CYAN, Color.DKGRAY, 
			                Color.MAGENTA, Color.GREEN, Color.YELLOW, Color.GRAY,
			                Color.rgb(255, 123, 2), Color.rgb(218, 106, 218), Color.rgb(255, 164, 222),
			                Color.rgb(255, 153, 153), Color.BLACK, Color.WHITE
			                };
	int clrIdx = 0;  // index to above array
	
	// Retrieve the next color / wrap at end
	private int getSomeColor() {
		int color = SomeClrs[clrIdx++];
		if(clrIdx >= SomeClrs.length) clrIdx = 0; // wrap
		return color;
	}
	
	// Used to give markers in a route different colors from those in another route
	final float[] MarkerColors = {BitmapDescriptorFactory.HUE_AZURE, BitmapDescriptorFactory.HUE_CYAN,
								  BitmapDescriptorFactory.HUE_BLUE, BitmapDescriptorFactory.HUE_GREEN,
			                      BitmapDescriptorFactory.HUE_ORANGE, BitmapDescriptorFactory.HUE_VIOLET,
			                      BitmapDescriptorFactory.HUE_ROSE, BitmapDescriptorFactory.HUE_MAGENTA};
									// RED not used
	int hueIdx = 0;  // index to above
	
	float privateHue = 0;  // Used for User markers
	
	// Request codes for startActivity
    final int ShowRoute = 123;              //  Id for file choosing activity
    final int ShowWaypoints = 124;
    final int ShowRouteAndWaypoints = 125;
    final int FileChosenForSave = 2345;
    final int RESULT_SETTINGS = 31;

    
    // Some display values
    float initialZoom = 15.0F;     // How to set this to get most WPs in view???
    int routeWidth = 5;
    final int LineWidth = 2;   // for connecting movable windows
    
	ArrayList<WayPoint> wplist = null;
	LatLng startPt;

	GoogleMap gMap;
//	MenuItem mapTypeMI;     // Last MenuItem selected for MapType
	int gmMapType = -1; //GoogleMap.MAP_TYPE_NORMAL;
	
	// These are saved to allow restore These are Parcelable items to save in Bundle 
	ArrayList<PolylineOptions> routeLines = new ArrayList<PolylineOptions>();
	ArrayList<Route> routes = new ArrayList<Route>();  // for waypoints that were read in as a route
	ArrayList<Route> waypoints = new ArrayList<Route>(); // for waypoints that were read in as waypoints not in a route
	ArrayList<MarkerOptions> waypointsMOs = new ArrayList<MarkerOptions>();
	ArrayList<WayPoint> userWPs = new ArrayList<WayPoint>();
	CameraPosition aCameraPos;
	
	// Define keys for bundle used with restore
	final String RouteLines_K = "routeLines";
	final String Routes_K = "routes";
	final String WaypointsMOs_K = "waypointsmos";
	final String UserWPs_K = "userwps";
	final String ColorIdx_K = "coloridx";
	final String HueIdx_K = "hueidx";
	final String CameraPos_K = "camerapos";
	final String MapType_K = "maptype";
	final static String WPList_K = "wplist";
	final String SetMyLocS_K = "set_mylocation";  // See preferences.xml (NOTE: l vs L ???)
	final String CreateWPAtMyLoc_K = "createWPAtMyLoc";
	final String MeasuringDistance_K = "measuringDist";
	final String ShowFeetIfShort_K = "showFeetIfShort";
	
	Map<Marker, MarkerState> markerStates = new HashMap<Marker, MarkerState>();
	
	boolean doingTwoTouch = false;   // flag for TwoTouch mode
	boolean measuringSpeed = false;  // for measuring speed between two wps
	Marker firstTouch = null;
	BitmapDescriptor firstTouchIcon = null;
	DecimalFormat df = new DecimalFormat("#.#");  // One decimal place
	DecimalFormat twoDP_df = new DecimalFormat("#.##"); // two decimal places
	BitmapDescriptor startCandDIcon; 
	
	// Define map for the dragable markers that show course and distance
	Map<Marker, MarkerLineState> markerLines = new HashMap<Marker, MarkerLineState>();
	
	final String rootDir = Environment.getExternalStorageDirectory().getPath();

	// Define variables for setting the starting location
	final String StartLocationFN = "StartLocation.txt";
	boolean place_wp_forStart = false;
	boolean create_wp_forStart = false;
	final String UserWayPointsFN = "UserWaypointsFN.txt";
	
	// Where we'll write waypoints when user saves them
	final String UserWP_Folder = rootDir + "/MapTests/";
	final String WPFile_Ext = WayPointFileIO.WPS_Ext;
	String userWP_fn = "userwaypoints.wps";
	
	final String LastWPFileReadFN = "LastWPFIelReadFN.txt"; // name of last waypoint file that was read
	String lastWPFile_fn = "";     // name of last waypoint file that was read
	
	private int customMarkerSize = 20;
	private boolean useCustomMarker =  false;
	
	// For updating a route
	private Marker selectedMarker = null;  // this is the Marker being worked on
	private BitmapDescriptor selectedMarkerIcon = null;
	private boolean routeHasBeenUpdated = false;  // so write routine knows
	private Route routeBeingUpdated = null;

	enum UpdateMode {AddMarker, DeleteMarker, MoveMarker, RenameMarker, Nothing};
	UpdateMode updateMode = UpdateMode.Nothing;   // What kind of update are we doing
	
	// Logging stuff
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US);
    Thread.UncaughtExceptionHandler lastUEH = null;
	final String LogFilePathPfx = rootDir + "/Maps4Norm/logs/log_";
	
	private SharedPreferences prefs;

	// Stuff for measuring distances
	// Code copied from: de.j4velin.mapsmeasure by Thomas Hoffmann
	private TextView valueTv;
	private View topCenterOverlay;
	private boolean measuringDistance = false;
	private static BitmapDescriptor marker;
	// the stacks - everytime the user touches the map, an entry is pushed
	private Stack<LatLng> trace = new Stack<LatLng>();
	private Stack<Polyline> lines = new Stack<Polyline>();
	private Stack<Marker> points = new Stack<Marker>();
	final static NumberFormat formatter_two_dec = NumberFormat.getInstance(Locale.getDefault());
	private float distance; 
	private final static int COLOR_LINE = Color.argb(128, 0, 0, 0), 
			                 COLOR_POINT = Color.argb(128, 255, 0, 0);
	private boolean setMyLocation = true;  // set the Maps marker
	private boolean showFeetIfShort = false; // use ft vs mile on C&D ,markers
	
	//--------------------------------
    public GoogleMap getMap() {
        return gMap;
    }
    public Stack<LatLng> getTrace() {
    	return trace;
    }
	
    private boolean createWPAtMyLoc = false;  // with MyLocation from GPS
    private double closeEnough = 2.0;     // 2 meters is close enough to set a WP ???
    
	boolean Testing = false;  // control some debug stuff
	
	//-----------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setMyLocation = prefs.getBoolean(SetMyLocS_K, false);
        showFeetIfShort = prefs.getBoolean(ShowFeetIfShort_K, false);
        customMarkerSize = Integer.parseInt(prefs.getString("customMarkerSize", ""+customMarkerSize));
        useCustomMarker = prefs.getBoolean("useCustomMarker", useCustomMarker);

        setExceptionHandler();
        
         // Following for java.lang.NullPointerException: IBitmapDescriptorFactory is not initialized
         try {
	         MapsInitializer.initialize(getApplicationContext());
	     } catch (GooglePlayServicesNotAvailableException e) {
	         e.printStackTrace();
	     }

        
        //  Try to draw a line between SK and CF
        SupportMapFragment smf = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        gMap = smf.getMap();
        System.out.println("M4N gMap="+gMap);
        
    	Intent intent = getIntent();
		System.out.println("onCreate() intent="+intent + "\n >>data="+intent.getData());
    	
    	// Test what intent started us. If "geo" then get the waypoint and show it.
		// onCreate() intent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10000000
		//  >>data=null
		// onCreate() intent=Intent{act=android.intent.action.VIEW dat=geo:0,0?q=36.8671,-93.4398(Cox Ford)&z=17 
		//       flg=0x3000000 cmp=com.normsstuff.maps4norm/.MyMapActivity }
		// old  >>data=geo:0,0?q=36.8671,-93.4398(Cox Ford)&z=17
		// new  >>data=geo:51.0508,13.7284?z=17

		Uri data = intent.getData();
		LatLng goToLocLL = null;
		String goToLocName = "<No Name>";
		int goToLocZoom = 11;
		if(data != null) {
			String query = data.getEncodedQuery();
			System.out.println("M4N onCreate() data query="+query + "<");
			// old  query=q=36.8671,-93.4398(Cox Ford)&z=17<
			// new onCreate() data query=z=17<
			int ix0 = 2;          // skip over q=
			int ix1 = query.indexOf(","); // set separator 1
			int ix2 = query.indexOf("(");
			int ix3 = query.indexOf(")");
			if(ix3 < 0){
				// wrong format, use data vs query
				// data=geo:51.0508,13.7284?z=17
				query = intent.getData().toString();
				if(!query.startsWith("geo:")) {
					showMsg("M4N Unknown intent data="+query);
				}
				ix0 = 4;  // skip over geo:
				ix1 = query.indexOf(",");
				ix2 = query.indexOf("?");
			}else{
				goToLocName = query.substring(ix2+1, ix3);
			}
			String latS = query.substring(ix0,ix1);
			String longS = query.substring(ix1+1,ix2);
			//name=Cox Ford  lat=36.8671, long=-93.4398
			double latD = Double.parseDouble(latS);
			double longD = Double.parseDouble(longS);
			goToLocLL = new LatLng(latD, longD);
			int ix4 = query.indexOf("z=");
			if(ix4 > ix3) {
				String zoomS = query.substring(ix4+2);  // Can there be more ??? +2 for "z="
				goToLocZoom = Integer.parseInt(zoomS);
			}
			System.out.println("M4N name="+goToLocName + "  lat="+latS+", long="+longS + "  zoom="+goToLocZoom);
		}  // end handling intent data
        
		//----------------------------------------------------
        // Did we save something from the last time?
		if(savedInstanceState != null) {
			//  Retrieve saved data
			clrIdx = savedInstanceState.getInt(ColorIdx_K);
			hueIdx = savedInstanceState.getInt(HueIdx_K);
			gmMapType = savedInstanceState.getInt(MapType_K);
			createWPAtMyLoc = savedInstanceState.getBoolean(CreateWPAtMyLoc_K);
			measuringDistance = savedInstanceState.getBoolean(MeasuringDistance_K);
			// Only need to do something if not Normal
			if(gmMapType != GoogleMap.MAP_TYPE_NORMAL) {
				//  unclick Normal and click for this type

				gMap.setMapType(gmMapType);
			}
			aCameraPos = savedInstanceState.getParcelable(CameraPos_K);
			
			routeLines = savedInstanceState.getParcelableArrayList(RouteLines_K);
			loadRoutes();
			waypointsMOs = savedInstanceState.getParcelableArrayList(WaypointsMOs_K);
			loadWayPoints();
			userWPs = savedInstanceState.getParcelableArrayList(UserWPs_K);
			routes = savedInstanceState.getParcelableArrayList(Routes_K);
		
			//  Position to where we were
			if(aCameraPos != null)
				gMap.moveCamera(CameraUpdateFactory.newCameraPosition(aCameraPos));
		}  // end restoring data from bundle
		
		// Did we come here because of an action intent???
		else if(goToLocLL != null) {
	 		MarkerOptions mo = new MarkerOptions()
	   	     .position(goToLocLL)
	   	     .title(goToLocName)
	   	     .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
	   	    );
	 		Marker aMrkr = gMap.addMarker(mo);
	 		waypointsMOs.add(mo);  // save for next time around  ????W
	 		WayPoint wp = new WayPoint(goToLocName, goToLocLL);
		    MarkerState ms = new MarkerState(aMrkr, mo.getIcon(), wp,
                                  new String[]{goToLocName, wp.latLongText()});
		    markerStates.put(aMrkr, ms);    // save to allow toggling
		    ms.toggleMarker(aMrkr);   // show the label

//	 		System.out.println("intent action #waypointsMOs="+waypointsMOs.size()); //<<<<<<<<
	        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(goToLocLL, goToLocZoom));

		}
		else if(Testing)
		{   //- - - - - - - - - - - - - - - - - - -  
			// For Testing:  First start will execute here - show some local waypoints
	        LatLng coxFord = new LatLng(36.8671, -93.43983);
	        LatLng shellKnob = new LatLng(36.61009, -93.607);
	        startPt = shellKnob;
	        PolylineOptions plo = new PolylineOptions().color(Color.YELLOW).width(routeWidth);
	        plo.add(shellKnob, coxFord);
	        
	        routeLines = new ArrayList<PolylineOptions>();  // for testing
	        routeLines.add(plo);
        
	        Polyline pl = gMap.addPolyline(plo);
	        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPt, 11));
	        
	        // add some Markers also
    		MarkerOptions mo = new MarkerOptions()
		   	     .position(shellKnob)
		   	     .title("Shell Knob")
		   	     .snippet("Oak Ridge Drive")
		   	     .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
    		Marker skMrkr = gMap.addMarker(mo);
    		
    		mo = new MarkerOptions()
	   	     .position(coxFord)
	   	     .title("Cox Ford land")
	   	     .snippet("Near Indian Tree GC")
	   	     .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
		    Marker cfMrkr = gMap.addMarker(mo);	
		    	    
		    // Try a mid point customized marker
			String CandD = getCourseAndDistance(skMrkr, cfMrkr);
		    add_Special_Marker(skMrkr, cfMrkr, CandD);
		    
		}  // end Testing - loading local waypoints
		
		
		// Finally see if there has been a Starting location saved
		else {
			WayPoint wp = null;
			try{
				FileInputStream fis = openFileInput(StartLocationFN);
				byte[] bfr = new byte[600]; // should never be this long
				int nbrRd = fis.read(bfr);
				String theText = new String(bfr, 0, nbrRd);
				System.out.println("read start loc=" + theText);
				wp = new WayPoint(theText);
				fis.close();
				// now get next one
			}catch(Exception x){
				x.printStackTrace();
			}
			if(wp != null) {
		 		MarkerOptions mo = new MarkerOptions()
		   	     .position(wp.getLocation())
		   	     .title(wp.getName())
		   	     .snippet(wp.latLongText())
		   	     .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
		   	    );
		 		Marker aMrkr = gMap.addMarker(mo);
		 		waypointsMOs.add(mo);  // save for next time around  ????W
			    MarkerState ms = new MarkerState(aMrkr, mo.getIcon(), wp,
	                                 new String[]{wp.getName(), wp.latLongText()});
			    markerStates.put(aMrkr, ms);    // save to allow toggling
			    ms.toggleMarker(aMrkr);         // show the label
	
			    System.out.println("startlocation name=" +wp.getName() + ", zoom="+wp.getElevation());
		        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wp.getLocation(), wp.getElevation()));
			}
		}
		
		//------------------------------------------
		// Make sure have our folder
		File testUserWPFolder = new File(UserWP_Folder);
		if(!testUserWPFolder.exists()){
			testUserWPFolder.mkdir();
			System.out.println("M4N created folder="+testUserWPFolder);
		}
		
		// Get the previous User WP filename
		try{
			FileInputStream fis = openFileInput(UserWayPointsFN);
			byte[] bfr = new byte[600]; // should never be this long
			int nbrRd = fis.read(bfr);
			String theText = new String(bfr, 0, nbrRd);
			System.out.println("M4N read userWP_fn=" + theText);
			fis.close();
			userWP_fn = theText;  // set the user WP fn from the previous session
		}catch(Exception x){
			x.printStackTrace();
		}
		
		// Read in the previous WP file
		// Does this need to be done on rotation???
		try{
			FileInputStream fis = openFileInput(LastWPFileReadFN);
			byte[] bfr = new byte[600]; // should never be this long
			int nbrRd = fis.read(bfr);
			String theText = new String(bfr, 0, nbrRd);
			System.out.println("M4N read lastWPfile fn=" + theText);
			fis.close();
			lastWPFile_fn = theText;  // set the fn from the previous session
			System.out.println("M4N lastWPFile_fn="+lastWPFile_fn);
			//lastWPFile_fn=/storage/emulated/0/Norms/Waypoints/userwaypoints.wps

			// Check that file exists
			File lastWPFile = new File(lastWPFile_fn);
			if(lastWPFile.exists()){
				// Read waypoints and save in tables etc
				ArrayList<WayPoint> wplistL = readWaypoints(lastWPFile_fn);
				System.out.println("M4N init read "+ wplistL.size() +" waypoints from " + lastWPFile_fn);
			    Toast.makeText(this, "Read " + wplistL.size() +" waypoints from " + lastWPFile_fn,
		 		       Toast.LENGTH_SHORT).show();
			    if(wplist == null || wplist.size() == 0)
			    	wplist = wplistL;
			    else {
			    	wplist.addAll(wplistL);
			    }
				
			}else{
				System.out.println("M4N lastWP file not found " + lastWPFile_fn);
			}
		}catch(Exception x){
			x.printStackTrace();
		}

		
		//----------------------------------------------------
		// Trap some events with Marker clicks
		gMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
			public boolean onMarkerClick(Marker mrkr){
				if(measuringDistance) {
					// If measuring distances, put one on this marker
					LatLng mrkrLoc = mrkr.getPosition();
					addPoint(mrkrLoc);
					return true;
				}
				
				MarkerState ms = markerStates.get(mrkr);
				System.out.println("onMarkerClick() ms="+ms + " for mrkr="+mrkr.getTitle());
				if(ms == null)
					return true; //false;     // default for a marker that we are not interested in
				//>>>>>>>>> PROBLEM HERE>>> Special markers will have info window shown!!!!
				
				// Are we selecting a Marker to add to a route?
				if(updateMode == UpdateMode.AddMarker) {
					System.out.println("oMC mrkr="+mrkr +", selectedMarker="+selectedMarker);
					if(selectedMarker != null) {  // check if second press
						// If press is on same marker, restore it
						boolean sameMarker = selectedMarker.equals(mrkr); // remember if same marker
						selectedMarker.setIcon(selectedMarkerIcon); // restore
						selectedMarker = null;  // turn off selection
						if(sameMarker) {
							System.out.println("onMarkerClick same marker restored");
							return true;   // done if second click on same marker
						}
						System.out.println("oMC markers not same");
					}
					selectedMarker = mrkr;
					selectedMarkerIcon = ms.getCurrentIcon();  // save for restore
					selectedMarker.setIcon(startCandDIcon);    // show it was selected
					showMsg("oMC addMarker Selected marker=" + mrkr.getTitle());
					return true;
				}  // end adding marker to a route
				
				// Are we going to delete a marker?
				if(updateMode == UpdateMode.DeleteMarker) {
					showMsg("oMC deleteMarker Selected marker=" + mrkr.getTitle()
							+ "\n >> Not implemented yet<<");
					return true;
				}

				// Are we going to rename a marker?
				if(updateMode == UpdateMode.RenameMarker) {
					String wpName = ms.waypoint.getName();
					showMsg("oMC renameMarker Selected marker=" + mrkr.getTitle() +"("+wpName+")"
							+ "\n >> Not implemented yet<<");
					return true;
				}

				// Are we going to move a marker?
				if(updateMode == UpdateMode.MoveMarker) {
					String wpLoc = ms.waypoint.getLocation().toString();
					showMsg("oMC moveMarker Selected marker=" + mrkr.getTitle() +" loc="+wpLoc
							+ "\n >> Not implemented yet<<");
					return true;
				}
				
				// Are we trapping two marker clicks to show the course and distance
				if(doingTwoTouch || measuringSpeed) {

					if(firstTouch == null){
						firstTouch = mrkr;     // save the first marker
						firstTouchIcon = ms.getCurrentIcon();    // Save current icon
						firstTouch.setIcon(startCandDIcon);  // This works
						
					}else{
						// Check for second click on same as first 
						if(firstTouch.equals(mrkr))
							return true;  // ignore it
						
						//System.out.println("firstTouch="+ firstTouch + ", mrkr="+mrkr);  //<<<<
						//firstTouch=com.google.android.gms.maps.model.Marker@420d0088, 
						//      mrkr=com.google.android.gms.maps.model.Marker@420d0088
						if(doingTwoTouch){
							// Build a midpoint marker and reset the firstTouch marker
							String CandD = getCourseAndDistance(firstTouch, mrkr);
							add_Special_Marker(firstTouch, mrkr, CandD);
							System.out.println/*showMsg*/(firstTouch.getTitle() + " to " + mrkr.getTitle()
								                 +"\n " + CandD);
						}else if(measuringSpeed){
							//  Compute distance and divide by time
							// Need to get the WayPoints for these Markers
							double firstTouchHours = Utils.parseHours(firstTouch.getTitle());
							double mrkrHours = Utils.parseHours(mrkr.getTitle());
							if(firstTouchHours < 0 || mrkrHours < 0) {
								showMsg("Markers don't have valid titles: "+firstTouch.getTitle() + " and " 
								         + mrkr.getTitle());
							
							}else {
								double duration = Math.abs(firstTouchHours - mrkrHours);
								// First get the text of the message
								String SandD = getSpeedAndDistance(firstTouch, mrkr, duration);
								// Then add it in a special marker
								add_Special_Marker(firstTouch, mrkr, SandD);
							}
						}
						
						firstTouch.setIcon(firstTouchIcon);    // restore the previous
						firstTouch = null;  // clear
						
						//???? Show this be reset now - only do one per menu selection???
					}
					return true;    // don't do default 
				}  // end doingTwoTouch
				
				// Are we setting a starting location
				if(place_wp_forStart){
					place_wp_forStart = false;  // turn off
					setStartLocation(mrkr);
					return true;
				}

				// Otherwise toggle the Marker's icon
				if(ms != null){
				    // Toggle the marker's icon
					ms.toggleMarker(mrkr);
					return true;  // don't do default
				}
				
				return true;     // don't default ????
			}
		});
		
		// ------------------------------------------
		// Define listener for the draggable markers
		gMap.setOnMarkerDragListener(new OnMarkerDragListener () {
			@Override
			public void onMarkerDrag(Marker mrkr){
				MarkerLineState mls = markerLines.get(mrkr);
				if(mls != null) {
					mls.drawLine();
				}
			}
			public void onMarkerDragStart(Marker mrkr){
				// Change the color here???
			}
			public void onMarkerDragEnd(Marker mrkr){
				MarkerLineState mls = markerLines.get(mrkr);
				if(mls != null) {
					mls.drawLine();
				}
			}
			
		});
		
		//----------------------------------------------------
		//  Trap user presses and see if on a route
		gMap.setOnMapClickListener(new OnMapClickListener() {
			public void onMapClick(LatLng clickLoc){
				if(measuringDistance) {
					addPoint(clickLoc);
					return;
				}
				
//				showMsg("map clicked at LL="+clickLoc);
				if(routes == null || routes.size() < 1)
					return;      // no routes available
				
				if((updateMode == UpdateMode.AddMarker) && (selectedMarker == null)) {
//					showMsg("onMC selectedMarker == null");  //???? why show this
					System.out.println("oMC addMarker ToRoute with selectedMarker == null");
					return;    // we're done???
				}
				
				LatLng closeLL = null;
				String foundDist = "";  // debug message built here
				String distMsg = null;
				// Compute closeEnough based on current view
				VisibleRegion vr = gMap.getProjection().getVisibleRegion();
	    		LatLngBounds llBnds = vr.latLngBounds;
	    		double spanLat = Math.abs(llBnds.northeast.latitude - llBnds.southwest.latitude);
	    		double spanLong = Math.abs(llBnds.southwest.longitude - llBnds.northeast.longitude);
	    		int factor = 30;   // should this change with orientation ???
				double closeEnough = spanLong/factor;  // 
				float totalDistanceSvd = 0.0f;
				int wpCountSvd = 0;
				Route closestRoute = null;
				int nextLLidx = 0; // save index to access waypoints

				
				// Go through the routes one by one
				for(Route route : routes) {
					float totalDistance = 0.0f;             // reset for this route
					int wpCount = route.waypoints.size();   // number of WPs for this route
					boolean foundCloser = false;    // set true if closer route found
					
					PolylineOptions plo = route.theLines;
					if(plo ==  null){
						System.out.println("*** onMapClick() plo=null for route="+route);
						continue; // skip
					}
					List<LatLng> listPts = plo.getPoints();
					// check the connecting lines between the points
					LatLng lastLL = listPts.get(0);  // get first one
		
					for(int i = 1; i < listPts.size(); i++) { 
						LatLng nextLL = listPts.get(i);
						totalDistance += distanceBetweenTwoPoints(lastLL, nextLL);
						// Approximate 2D on flat earth
						double dist2Line = Utils.lineToPointDistance(clickLoc.latitude, clickLoc.longitude,
										   lastLL.latitude, lastLL.longitude, nextLL.latitude, nextLL.longitude);
						
//						System.out.println("dist2Line="+dist2Line + ", closeEnough="+closeEnough);
						
						if(dist2Line <= closeEnough) {
							closeEnough = dist2Line; // save closest so far
							foundDist += "\n" + i + " = " + dist2Line;
							closeLL = nextLL;
							nextLLidx = i;            //save index to access waypoints
							distMsg = "" + dist2Line;
							closestRoute = route;
							System.out.println("dist2Line="+dist2Line + ", closeEnough="+closeEnough);
							foundCloser = true;
						}
						lastLL = nextLL;        // the LL also
					}  // end for(i) through points in this PLO
					
					if(foundCloser){
						//  save total distance for the route and # wps
						totalDistanceSvd = totalDistance;
						wpCountSvd = wpCount;
					}
				}  // end for() through PLO in routes
				
       
				// Have found closest route to touched point 
				if(closeLL != null) {
					// Are we adding a WP to a route?
					if((updateMode == UpdateMode.AddMarker) && (selectedMarker != null)) {  // NPE when selectedMarker is null
						WayPoint wp1 = closestRoute.waypoints.get(nextLLidx-1);
						WayPoint wp2 = closestRoute.waypoints.get(nextLLidx);
						showMsg("oMC route="+closestRoute.fileName + "\nwp1="+wp1.getName()
								+ ", wp2="+wp2.getName());
						// Have a marker to add to a route
						// Need to:
						//  add waypoint to route
						LatLng pos = selectedMarker.getPosition();
						String name = selectedMarker.getTitle();
						WayPoint wp = new WayPoint(name, pos);
						closestRoute.addWaypoint(wp, nextLLidx);
						routeHasBeenUpdated = true;
						routeBeingUpdated = closestRoute; // save
						System.out.println("M4N added wp="+ name + " at index="+nextLLidx
								           + ", #waypoints="+closestRoute.waypoints.size()
								           + ", name="+closestRoute.routeName);
						System.out.println("M4N waypoints="+closestRoute.waypoints);
						
						//  clear current line and waypoints
						clearRouteLinesAndWaypoints();
						// Draw the markers
						drawRouteMarkers(closestRoute);

						// draw the new lines
						drawRouteLines(closestRoute);
						
						// restore markers for any waypoints not on a route
						if(waypoints.size() > 0){
							for(int i=0; i < waypoints.size(); i++)
								drawRouteMarkers(waypoints.get(i));  // show markers in this group
						}
						
						// Unselect the marker
						selectedMarker = null;      // turn off selection
						System.out.println("M4N after new route and waypoints drawn");		
						
					}else {
// 					    showMsg("Distances: "+foundDist + "\n closeLL="+ closeLL);
						LatLng nearPtLL = clickLoc; //<<<<<<<<<<<
					    String routeInfo = "Length: "+ twoDP_df.format(totalDistanceSvd)
					    		            + " miles, " + wpCountSvd + " waypoints.";
//					    String routeName = closestRoute.fileName;
//					    int ix = routeName.lastIndexOf("/");
//					    int ix2 = routeName.indexOf(".");
//					    String rtNm = routeName.substring(ix+1, ix2);  //Filename only
					    String title = "Route: "+ closestRoute.routeName;
					    int[] colors = {Color.RED, 0XFFE4FFFF};
					    Bitmap bmp = getCustomMarkerBM(new String[]{title, routeInfo}, colors, true);
	
						MarkerOptions mo = new MarkerOptions()
				   	     .position(nearPtLL)
				   	     .title(title)
				   	     .snippet(routeInfo)
				   	     .icon(BitmapDescriptorFactory.fromBitmap(bmp));
		   		        Marker rtMrkr = gMap.addMarker(mo);  //<<< Should this be saved so it can be cleared???
		   		        // Hide current marker if going to show a new one
		   		        Marker mkr = closestRoute.getInfoMarker();
		   		        if(mkr != null) {
		   		        	mkr.setVisible(false);
		   		        }
		   		        closestRoute.setInfoMarker(rtMrkr);  // save new marker
					}
				}
			}
		});
		
		//-----------------------------------------------------------------------
		// Trap user's long presses and put a marker there
		gMap.setOnMapLongClickListener(new OnMapLongClickListener() {
			public void onMapLongClick(final LatLng clickLoc){
//				showMsg("map long clicked at LL="+clickLoc);
				
				// build a custom marker here
				if(privateHue == 0) {
					privateHue = MarkerColors[hueIdx++];
					if(hueIdx >= MarkerColors.length)
						hueIdx = 0; // reset
				}
				// Get the name from the user
	        	AlertDialog.Builder alert = new AlertDialog.Builder(MyMapActivity.this);
	        	alert.setTitle("Enter Marker name");
	        	alert.setMessage("Enter a name for this Marker");

	        	// Set an EditText view to get user input 
	        	final EditText input = new EditText(MyMapActivity.this);
	        	alert.setView(input);

	        	alert.setPositiveButton("Set Marker name", new DialogInterface.OnClickListener() {
		        	public void onClick(DialogInterface dialog, int whichButton) {
		        	  String wpName = input.getText().toString().trim();
		        	  if(wpName == null || wpName.length() == 0)
		        		  wpName = "<No name given>";
						WayPoint wpx=  new WayPoint(wpName, clickLoc);
						userWPs.add(wpx);
			    		MarkerOptions mo = new MarkerOptions()
			    	     .position(wpx.getLocation())
			    	     .title(wpx.getName())
			    	     .snippet(wpx.latLongText())
			    	     .icon(BitmapDescriptorFactory.defaultMarker(privateHue));
			    		Marker aMarker = gMap.addMarker(mo);
					    MarkerState ms = new MarkerState(aMarker, mo.getIcon(), wpx,
					    		                         new String[]{wpx.getName(), wpx.latLongText()});
					    markerStates.put(aMarker, ms);    // save to allow toggling
					    
					    // Also save user's marker for restart
					    waypointsMOs.add(mo);
					    
					    // Check if adding a starting location
					    if(create_wp_forStart){
					    	create_wp_forStart = false;   // turn off
					    	setStartLocation(aMarker);
					    }
		        	}  
		        });

	        	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        	  public void onClick(DialogInterface dialog, int whichButton) {
	        	    // Canceled.
	        	  }
	        	});

	        	alert.show();
			}  // end onMapLongClick()
		});
		
		startCandDIcon = BitmapDescriptorFactory.fromResource(R.drawable.crawler);  // Get the icon
		
		//---------------------------------------------------------
		// Setup for measuring distance
		topCenterOverlay = findViewById(R.id.topCenterOverlay);
		valueTv = (TextView) findViewById(R.id.distance);
		topCenterOverlay.setVisibility(View.GONE);  //????INVISIBLE doesn't work with FrameLayout
		marker = BitmapDescriptorFactory.fromResource(R.drawable.marker);
		formatter_two_dec.setMaximumFractionDigits(2);
		
		View deleteImg = (View)findViewById(R.id.delete); 
		deleteImg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				removeLast();
			}
		});
		
		deleteImg.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(final View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MyMapActivity.this);
				builder.setMessage(getString(R.string.delete_all, trace.size()));
				builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						clearDM();
						dialog.dismiss();
					}
				});
				builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.create().show();
				return true;
			}
		});
		
				
		if(setMyLocation) {
			turnOnMyLocation();
		} 

    }  // end onCreate()
    
    //=======================================================================================
    @Override
    public void onPause() {
    	super.onPause();
    	System.out.println("M4N onPause()");
  	
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	System.out.println("M4N onResume");
    }
    //---------------------------------------------------------------
    private void setExceptionHandler() {
    	if(lastUEH != null) {
    		return;  // already setup - exit
    	}
        // Define inner class to handle exceptions
        class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
            public void uncaughtException(Thread t, Throwable e){
               java.util.Date dt =  new java.util.Date();
               String fn = LogFilePathPfx + "exception_" + sdf.format(dt) + ".txt";
               try{ 
                  PrintStream ps = new PrintStream( fn );
                  e.printStackTrace(ps);
                  ps.close();
                  System.out.println("wrote trace to " + fn);
                  e.printStackTrace(); // capture here also???
                  SaveStdOutput.stop(); // close here vs calling flush() in class 
               }catch(Exception x){
                  x.printStackTrace();
               }
               lastUEH.uncaughtException(t, e); // call last one  Gives: "Unfortunately ... stopped" message
               return;    //???? what to do here
            }
         }
        
         lastUEH = Thread.getDefaultUncaughtExceptionHandler(); // save previous one
         Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler());
    }
    
    //--------------------------------------------------
    private void turnOnMyLocation() {
		gMap.setMyLocationEnabled(true);
        gMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (gMap.getMyLocation() != null) {
                    LatLng myLocation = new LatLng(gMap.getMyLocation().getLatitude(),
                                                   gMap.getMyLocation().getLongitude());
                    double distance = SphericalUtil.computeDistanceBetween(myLocation, 
                    		                                   gMap.getCameraPosition().target);
                    System.out.println("M4N onMyLocBtnClck distance="+distance
                    		+ ", measuringDist="+measuringDistance 
                    		+", createWPAtMyLoc=" + createWPAtMyLoc);
                    // Only if the distance is less than 50cm??? we are on our location, add the marker
                    if (distance < closeEnough) {
                        Toast.makeText(MyMapActivity.this, R.string.marker_on_current_location,
                                Toast.LENGTH_SHORT).show();
                        // Save this point somewhere
                        if(measuringDistance)
                        	addPoint(myLocation);   // Save distance marker
                        if(createWPAtMyLoc){
                        	createWPAtLoc(myLocation, null);  
                        }
                    }
                }
                return false;
            }
        });
	
    }
    
    // -------------------------------------------------------------------
    // Build a new marker
    private void createWPAtLoc(final LatLng clickLoc, final String wpName) {
		if(privateHue == 0) {
			privateHue = MarkerColors[hueIdx++];
			if(hueIdx >= MarkerColors.length)
				hueIdx = 0;  // reset by wrapping around to beginning
		}
		// Get the name from the user
    	AlertDialog.Builder alert = new AlertDialog.Builder(MyMapActivity.this);
    	alert.setTitle("Enter Marker name");
    	alert.setMessage("Enter a name for this Marker");

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(MyMapActivity.this);
    	if(wpName != null)
    		input.setText(wpName);   //  insert name if given
    	alert.setView(input);

    	alert.setPositiveButton("Set Marker name", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        	  String wpName = input.getText().toString().trim();
        	  if(wpName == null || wpName.length() == 0)
        		  wpName = "<No name given>";
				WayPoint wpx=  new WayPoint(wpName, clickLoc);
				userWPs.add(wpx);
	    		MarkerOptions mo = new MarkerOptions()
	    	     .position(wpx.getLocation())
	    	     .title(wpx.getName())
	    	     .snippet(wpx.latLongText())
	    	     .icon(BitmapDescriptorFactory.defaultMarker(privateHue));
	    		Marker aMarker = gMap.addMarker(mo);
			    MarkerState ms = new MarkerState(aMarker, mo.getIcon(), wpx,
			    		                         new String[]{wpx.getName(), wpx.latLongText()});
			    markerStates.put(aMarker, ms);    // save to allow toggling
			    
			    // Also save user's marker for restart
			    waypointsMOs.add(mo);
        	}
    	});
    	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      	  public void onClick(DialogInterface dialog, int whichButton) {
      	    // Canceled.
      	  }
      	});

      	alert.show();

    }  // end createWPAtLoc()
    
    //---------------------------------------------------------------------
    // Compute and format the course and distance between two markers
    private String getCourseAndDistance(Marker firstM, Marker secondM){
		LatLng firstLL = firstM.getPosition();
		LatLng lastLL = secondM.getPosition();
		float[] results = new float[3];
		Location.distanceBetween(firstLL.latitude, firstLL.longitude, 
				                 lastLL.latitude, lastLL.longitude, results);  
		//The computed distance is stored in results[0]. 
		//If results has length 2 or greater, the initial bearing is stored in results[1]. 
		//If results has length 3 or greater, the final bearing is stored in results[2].
		float distInMiles = results[0] * 0.000621371F;  // convert to miles
		float distInFeet = results[0] * 3.28084F;
		int course = (int)(results[2] >= 0 ? results[2] : (360 + results[2]));
		return "course="+course + "deg distance=" 
				+ (showFeetIfShort && (distInFeet < 5280) ? twoDP_df.format(distInFeet) + "ft" 
						           : twoDP_df.format(distInMiles) +"mi");
    }
    
    //---------------------------------------------------------------
    //  Compute the speed in miles per hour between two points
    // First we need to find what route these markers are in
    // Then scan the route between the markers and accumulate the distance
    // then compute the mph
    private String getSpeedAndDistance(Marker firstM, Marker secondM, double duration){
    	LatLng firstLoc = firstM.getPosition();
    	LatLng secondLoc = secondM.getPosition();
    	LatLng endLoc = null;
    	Marker endM = null;
		double totalDistance = 0.0;
    	
		for(Route route : routes) {
			boolean foundLoc = false;
			PolylineOptions plo = route.theLines;
			List<LatLng> listPts = plo.getPoints();
			int nextLL_i = 0;

			// Find the first one starting at the beginning of the list
			// When found, set endLoc to be the next one to search for
			for(int i = 0; i < listPts.size() && !foundLoc; i++) { 
				LatLng nextLL = listPts.get(i);
			    if(Utils.areLocationsClose(nextLL, firstLoc)){
			    	foundLoc = true;
			    	endLoc = secondLoc;
			    	endM = secondM;
			    	nextLL_i = i;   // Save starting index
				}else if(Utils.areLocationsClose(nextLL, secondLoc)){
					foundLoc = true;
					endLoc = firstLoc;
					endM = firstM;
					nextLL_i = i;
				}
			} // end for(i) through points on this line
			
			// If found one end, now search for the other end
			if(foundLoc){
				LatLng lastLL = listPts.get(nextLL_i);
				double theDistance = 0.0;
				
				for(int i = nextLL_i; i < listPts.size(); i++){
					LatLng nextLL = listPts.get(i); 
					theDistance += distanceBetweenTwoPoints(lastLL, nextLL);
					if(Utils.areLocationsClose(endLoc, nextLL)) {
						totalDistance = theDistance; 
						break;   // this is the last one
					}
					lastLL = nextLL;  // copy for next leg
				}  // end for(i)
				
				break; // exit outer for()
				
			}  // have found start location
			
		} // end for() through routes
		
		if(totalDistance == 0.0){
			showMsg("End point not found:" + endM.getTitle());
		}
		
		double mph = totalDistance / duration;
		
    	return "Speed=" + df.format(mph) + "MPH in " + twoDP_df.format(duration) +" hours.";
    }  // end getSpeedAndDistance()
    
    //----------------------------------------------------------------
    private float distanceBetweenTwoPoints(LatLng pt1, LatLng pt2){
		float[] results = new float[3];
		Location.distanceBetween(pt1.latitude, pt1.longitude, 
				                 pt2.latitude, pt2.longitude, results);  
		//The computed distance is stored in results[0]. 
		//If results has length 2 or greater, the initial bearing is stored in results[1]. 
		//If results has length 3 or greater, the final bearing is stored in results[2].
		return results[0] * 0.000621371F;  // convert to miles
    }
    
    
    //------------------------------------------------
    // Define values for spacing the text in our BM image
    final static int BottomArrowHt = 15;
    final static int TB_Space = 5;
    final static int LR_Space = 5;
    
    //------------------------------------------------------------
    public static Bitmap getCustomMarkerBM(String[] strs, int[] colors, boolean withArrow) {
    	// colors[0] is foreground, colors[1] is background
    	// First line is a header to do in bold
    	// following line(s) are done in normal
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; 
        Paint paint = new Paint();
        // Top line of text will be bold
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        paint.setTextSize(20F);
        Rect firstBnds = new Rect();
        paint.getTextBounds(strs[0], 0, strs[0].length(), firstBnds);
//       System.out.println("firstBnds.right="+firstBnds.right +", btm="+firstBnds.bottom 
//        		              +", top="+firstBnds.top); //firstBnds.right=48, btm=3, top=-10

        //  Rest of text will be normal
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        Rect[] scndBnds = new Rect[strs.length-1];
        for(int i=0; i < scndBnds.length; i++) {
        	scndBnds[i] = new Rect();  // populate array
        	paint.getTextBounds(strs[i+1], 0, strs[i+1].length(), scndBnds[i]);
//        	System.out.println("scndBnds.right="+scndBnds[i].right+", btm="+scndBnds[i].bottom); 
        	//scndBnds=Rect(1, -10 - 152, 3)
        }
        //  Q&D here assuming only two lines for now <<<<<<<<<<<<<<
        int maxStrLng = firstBnds.right > scndBnds[0].right ? firstBnds.right : scndBnds[0].right;
        int totalHeight = firstBnds.bottom - firstBnds.top + scndBnds[0].bottom - scndBnds[0].top;

        int width = maxStrLng + 2*LR_Space;
        //  ??? should height include BottomArrowHt if withArrow=false???
        int height = totalHeight + BottomArrowHt + (strs.length+1)*TB_Space;
//        System.out.println("totalHt="+ totalHeight +", height="+ height); //totalHt=36, height=66

        Bitmap bmp = Bitmap.createBitmap(width, height, conf); 
        Canvas canvas = new Canvas(bmp);
        canvas.save();  // allow restore() and reset clip
        canvas.clipRect(0f, 0f, width, height-BottomArrowHt);   //????  how to undo this and set new clip
        System.out.println("clip1="+canvas.getClipBounds());
        canvas.drawColor(colors[1]);  // Fill with background
        paint.setColor(colors[0]);    // Set foreground
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));

        //  How to position the text ???  Center or left adjusted
        // paint defines the text color, stroke width, size
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        int startX = LR_Space + (maxStrLng - firstBnds.right)/2;
        int drawY = firstBnds.bottom-firstBnds.top + TB_Space;
        canvas.drawText(strs[0], startX, drawY, paint); 
        
        for(int i=0; i < scndBnds.length; i++) {
	        int nextY = drawY + TB_Space + scndBnds[i].bottom-scndBnds[i].top;
	        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
	        paint.setColor(Color.GRAY);
	        canvas.drawText(strs[i+1], LR_Space, nextY, paint); 
//	        System.out.println("drawY="+drawY +", nextY="+nextY);
	    }
        if(withArrow) {
	        float yVal = (float)(height - BottomArrowHt);
	        float xDownPt = (width/2 - 20);
	        float xUpPt = xDownPt + 40;
	        // Draw border
	        paint.setColor(Color.GRAY);
	        paint.setStrokeWidth(3);
	        paint.setStyle(Paint.Style.STROKE);
	        canvas.drawRect(0, 0, width, yVal, paint);
	        
	        canvas.drawLine(0, 0, 0, yVal, paint);
	         
	        Path path = new Path();  //  make a pointer at bottom
	        //  Just fill in the down pointer triangle
	        path.moveTo(xDownPt, yVal);
	        path.lineTo(xUpPt, yVal);
	        path.lineTo(width/2, yVal+BottomArrowHt);
	        path.lineTo(xDownPt, yVal);
	        Paint transparent = new Paint();
	        transparent.setColor(Color.BLUE);  
	        transparent.setStyle(Paint.Style.FILL);
	        canvas.restore();  // need to do a save() first
	        canvas.clipRect(0, yVal, width, height);
//	        System.out.println("clip2="+canvas.getClipBounds());
	        canvas.drawPath(path, transparent);
        } // end withArrow
        return bmp;
    }
    
    //--------------------------
    class MarkerLineState {
    	Marker startMrkr;    // fixed marker the line is drawn from
    	Marker moveableMrkr;
    	Polyline theLine;    // the line from above to the moveable Marker
    	
    	public MarkerLineState(Marker sm, Marker mm){
    		startMrkr = sm;
    		moveableMrkr = mm;
    	}
    	
    	public void drawLine(){
    		if(theLine != null){
    			theLine.setVisible(false); // get rid of old line
    		}
    		
	        PolylineOptions plo = new PolylineOptions().color(Color.BLACK).width(LineWidth);
	        plo.add(startMrkr.getPosition(), moveableMrkr.getPosition());
       
	        theLine = gMap.addPolyline(plo);   // draw the new line
    	}
    	
    	public void hideMarkers() {
    		startMrkr.setVisible(false);
    		moveableMrkr.setVisible(false);
       		if(theLine != null){
    			theLine.setVisible(false); // get rid of line also
    		}
    	}
    	// Check if the start marker is at this location
    	public boolean checkLocationOfStart(LatLng ll){
    		return startMrkr.getPosition().equals(ll);
    	}
    	
    } // end class MarkerLineState

	//----------------------------------------------------------
	// Add course and distance markers
	// 1 very small marker mid point on the line
	// 1 draggable marker with the course and distance
	
    private void add_Special_Marker(Marker startMkrP, Marker endMkrP, String text) {   
    	
    	LatLng startLL = startMkrP.getPosition();
        LatLng endLL = endMkrP.getPosition();
	    //------------------------------------------------------
	    // Try a mid point customized marker
	    double midLat = (startLL.latitude + endLL.latitude)/2;
	    double midLong = (startLL.longitude + endLL.longitude)/2;
	    LatLng midPt = new LatLng(midLat, midLong);
	    
	    // Check that there isn't already a marker here
	    // go through list and call checkLocationOfStart(midPt) <<<<<<<<<<<<<<
	    
	    // First marker is small and on the line
   		MarkerOptions mo1 = new MarkerOptions()
   	     .position(midPt)
   	     .title("Mid point")
   	     .anchor(0.5f, 0.5F)
   	     .snippet("bearing and distance connects here")
   	     .icon(BitmapDescriptorFactory.fromResource(R.drawable.small_square)
   	    );
//	    Marker startMrkr = gMap.addMarker(mo1);	  // Put midPt marker on map  <<<< PROBLEM THIS IS ON TOP !!!
	    
	    // Now build the draggable marker
//	    String CandD = getCourseAndDistance(startMkrP, endMkrP);
	    String title = startMkrP.getTitle() + " to "+endMkrP.getTitle();
	    int[] colors = {Color.RED, 0XFFE4FFFF};
	    Bitmap bmp = getCustomMarkerBM(new String[]{title, text}, colors, false);
  		MarkerOptions mo2 = new MarkerOptions()
  	     .position(midPt)
  	     .title(title)
  	     .draggable(true)        //<<<<<<<  will need to draw a connecting line
  	     .anchor(0.5F, 0.7F)
  	     .snippet(text)
  	     .icon(BitmapDescriptorFactory.fromBitmap(bmp)
  	    );
  		// Does the order of adding determine which is on top??? 
  		//  First one added is on top!!!
  		Marker endMrkr = gMap.addMarker(mo2);
	    Marker startMrkr = gMap.addMarker(mo1);	  // Put midPt marker on map  
  		
	    MarkerLineState mls = new MarkerLineState(startMrkr, endMrkr);
	    markerLines.put(endMrkr, mls);
	
    }  // end add_C_and_D_Marker()
   
	//--------------------------------------------------------------
	//  Save values to allow us to restore state when rotated
	@Override
	public void onSaveInstanceState(Bundle bndl) {
		super.onSaveInstanceState(bndl);
		bndl.putInt(ColorIdx_K, clrIdx);
		bndl.putInt(HueIdx_K,  hueIdx);
		bndl.putInt(MapType_K, gMap.getMapType());
		bndl.putBoolean(CreateWPAtMyLoc_K, createWPAtMyLoc);
		bndl.putBoolean(MeasuringDistance_K, measuringDistance);
		bndl.putParcelable(CameraPos_K, aCameraPos);
		bndl.putParcelableArrayList(RouteLines_K, routeLines);
		bndl.putParcelableArrayList(WaypointsMOs_K, waypointsMOs);
		bndl.putParcelableArrayList(UserWPs_K, userWPs);
		bndl.putParcelableArrayList(Routes_K,  routes);
	}

	//----------------------------------------------------------------
	@Override
	public boolean onPrepareOptionsMenu(Menu m) {
		super.onPrepareOptionsMenu(m);
		// Handle the MapType settings: some radio buttons
		if(gmMapType < 0 || true){  // Hardcode always using current setting
			gmMapType = gMap.getMapType();
		}
		MenuItem mi = (MenuItem)m.findItem(R.id.normal);
		if(mi != null) {                 //<<<<<<<<<< null???
			// Have we been restored?  Initially value is -1
			if(gmMapType == GoogleMap.MAP_TYPE_NORMAL) {
		        mi.setChecked(true);
		        gmMapType = GoogleMap.MAP_TYPE_NORMAL;
			}
			else {
				mi.setChecked(false);  // turn off Normal
				// Find and check on the selected one
				if(gmMapType == GoogleMap.MAP_TYPE_SATELLITE){
					mi = (MenuItem)m.findItem(R.id.satellite);
					if(mi != null) {
						mi.setChecked(true);
					}
				}else if(gmMapType == GoogleMap.MAP_TYPE_TERRAIN) {
					mi = (MenuItem)m.findItem(R.id.terrain);
					if(mi != null) {
						mi.setChecked(true);
					}
				}else{
					System.out.println("**ERROR** unknown gmMapType="+gmMapType);
				}
			}
		}
		else {
			Log.d("onPrepOptsMenu()", "mi=null  menu="+m);
		}
		// Disable/enable Write User WPs item if WPs any in list
		mi = (MenuItem)m.findItem(R.id.writeuserwps);
		mi.setEnabled(userWPs.size() > 0); 
		
		// Only if we have waypoints to go to
		mi = (MenuItem)m.findItem(R.id.gotowaypoint);
		mi.setEnabled(wplist !=  null && wplist.size() > 0);
		
		// Set Start location buttons
		mi = (MenuItem)m.findItem(R.id.create_wp);
		mi.setChecked(create_wp_forStart);
		mi = (MenuItem)m.findItem(R.id.place_wp);
		mi.setEnabled(wplist !=  null && wplist.size() > 0);  // only if showing markers
		mi.setChecked(place_wp_forStart);
		
		// Set hidden box item
		mi = (MenuItem)m.findItem(R.id.measureDistance);
		if(mi != null) {                  //<<<<<<<<<< null???
			mi.setChecked(topCenterOverlay.isShown()); // show check  
	    }
		mi = (MenuItem)m.findItem(R.id.saveMarks);
		mi.setEnabled(trace.size() > 0); // only if there are some to save
		mi = (MenuItem)m.findItem(R.id.createWPAtMyLoc);
		mi.setChecked(createWPAtMyLoc);
		
		
		// ========= For updating route ==================
		mi = (MenuItem)m.findItem(R.id.read_wp_and_route);
		mi.setEnabled(updateMode == UpdateMode.Nothing);  // Don't allow reading of route when set
		boolean haveOneRoute = (routes != null) && (routes.size() == 1);
		
		mi = (MenuItem)m.findItem(R.id.addMarker);
		mi.setEnabled(haveOneRoute);
		mi.setChecked(updateMode == UpdateMode.AddMarker);
		
		mi = (MenuItem)m.findItem(R.id.deleteMarker);
		mi.setEnabled(haveOneRoute);
		mi.setChecked(updateMode == UpdateMode.DeleteMarker);
		
		mi = (MenuItem)m.findItem(R.id.moveMarker);
		mi.setEnabled(haveOneRoute);
		mi.setChecked(updateMode == UpdateMode.MoveMarker);
		
		mi = (MenuItem)m.findItem(R.id.renameMarker);
		mi.setEnabled(haveOneRoute);
		mi.setChecked(updateMode == UpdateMode.RenameMarker);
		
		mi = (MenuItem)m.findItem(R.id.noUpdates);
		mi.setChecked(updateMode == UpdateMode.Nothing);
		
		mi = (MenuItem)m.findItem(R.id.undoUpdate);
		mi.setEnabled(selectedMarker != null);     //???? when should this be enabled
		
		mi = (MenuItem)m.findItem(R.id.writeRoute);
		mi.setEnabled(routeHasBeenUpdated);
		
		return true;
	} // end onPrepareOptionsMenu()

	//--------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }
    
	//----------------------------------------------------------
	// Handle menu item selection
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("oOIS", "item="+item.getItemId());
		
	    // Handle item selection
		int itemId = item.getItemId();  // to use in common cases with route/waypoint files
	    switch (item.getItemId()) {
	    	case R.id.normal:
//	    		swapSelection(item);
	    		item.setChecked(!item.isChecked());   // toggle the setting
	    		gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	    		return true;
	    	case R.id.satellite:
	    		item.setChecked(!item.isChecked());   // toggle the setting
//	    		swapSelection(item);
	    		gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
	    		return true;
	    	case R.id.terrain:
	    		item.setChecked(!item.isChecked());   // toggle the setting
//        		swapSelection(item);
	    		gMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
	    		return true;
	    		
	    	case R.id.twotouch:
	        	doingTwoTouch = !item.isChecked();  // toggle setting
	        	if(item.isChecked())
	        		item.setChecked(false);
	        	else
	        		item.setChecked(true);

	    		return true;
	    		
	    	case R.id.measurespeed:
	    		measuringSpeed = !item.isChecked();  // toggle setting
	    		item.setChecked(!item.isChecked());  // toggle menu item
	    		
	    		return true;
	    		
	    	case R.id.set_uwp_filename:
	    		// Set the User waypoint filename
	        	AlertDialog.Builder alert = new AlertDialog.Builder(this);
	        	alert.setTitle("Set user waypoint filename");
	        	alert.setMessage("Enter filename (without extension) for user waypoint file."
	        			+"\nFile is written to "+ UserWP_Folder);

	        	// Set an EditText view to get user input 
	        	final EditText input = new EditText(this);
	        	int ix = userWP_fn.indexOf(".");
	        	input.setText(userWP_fn.substring(0, ix));  // show current value
	        	alert.setView(input);

	        	alert.setPositiveButton("Set filename", new DialogInterface.OnClickListener() {
		        	public void onClick(DialogInterface dialog, int whichButton) {
		        	  String newFN = input.getText().toString();
		        	  if(newFN == null || newFN.length() == 0)
		        		  return;		// exit if user quit
		        	  	
		        	  userWP_fn = newFN + WayPointFileIO.WPS_Ext;
		        	  String wpFileMsg = "New File will be created";
		        	  File testFile = new File(UserWP_Folder + userWP_fn);
		        	  if(testFile.exists()){
		        		  wpFileMsg = "File exists";
		        	  }
		    		  Toast.makeText(MyMapActivity.this, "User waypoint filename set to: "+ userWP_fn
		    				  							 +"\n" + wpFileMsg,
	    		    		         Toast.LENGTH_LONG).show();
		    		  // Save name for next time???
		    		  try{
	    				FileOutputStream fos = openFileOutput(UserWayPointsFN, Context.MODE_PRIVATE);
	    				fos.write(userWP_fn.getBytes());
	    				fos.close();
		    		  }catch(Exception x){
		    			  x.printStackTrace();
		    		  }
		        	}
	        	});
	        	alert.setNeutralButton("Choose file", new DialogInterface.OnClickListener() {
	          	  	public void onClick(DialogInterface dialog, int whichButton) {
	          	  		// Choose the file to write to
	    	            Intent intent2 = new Intent(Intent.ACTION_GET_CONTENT);
	    	            intent2.setType("file/*");

	    	            //  How to get String ids below???
	    	    		intent2.putExtra("START_PATH", new File(UserWP_Folder));
	    	    		intent2.putExtra("TYPE_FILTER", 
	    	    				         new String[]{".wps"});  // These are only ones on one line
	    	    		intent2.putExtra(Intent.EXTRA_TITLE, "Choose file to save to");  //????? Where does this go
//	    	    		System.out.println("selectBtn clicked intent="+intent2);
	    	    		System.out.println("M4N selectBtnClicked to choose file to copy to, intent="+intent2);
	    	    		startActivityForResult(intent2, FileChosenForSave);
	    	    		// Should the copy be done immediately after the file is chosen?
	          	  	}
	        	});


	        	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        	  public void onClick(DialogInterface dialog, int whichButton) {
	        	    // Canceled.
	        	  }
	        	});

	        	alert.show();

	    		return true;
	    		
	    	case R.id.writeuserwps:
	    		if(userWPs.size() == 0)
	    			return true;             // exit if  none
	    		
	    		// Write the user's waypoints to a file
	    		String wpFN = UserWP_Folder + userWP_fn;  //<<<<<<<< WHERE TO GET FileName???
	    		// Make a list of those not already copied
	    		ArrayList<WayPoint> newWPs = new ArrayList<WayPoint>();
    		    for(WayPoint wp : userWPs){
    		    	if(wp.getUserObject() == null) { // check if written before
    		    		newWPs.add(wp);    // only copy new ones
 		    		    wp.setUserObject(wpFN);  // mark as copied
    		    	}
    		    }
	    		try{
	    			FileWriterClass fwc = WayPointFileIO.getFileWriter(wpFN);
	    			fwc.writeWPs(newWPs);
	    			fwc.closeFile();
	    		    Toast.makeText(this, "Wrote " + newWPs.size() +" waypoints to " + wpFN,
	    		    		       Toast.LENGTH_SHORT).show();
	    		}catch(Exception x){
	    			showMsg("error writing waypoint file e="+x);
	    			x.printStackTrace();
	    		}
	    		// Change the color for the next set
				privateHue = MarkerColors[hueIdx++];
				if(hueIdx >= MarkerColors.length)
					hueIdx = 0; // reset
	    		return true;
	    		
	    	case R.id.gotowaypoint:
	    		if(wplist == null)   // catch error that shouldn't happen
	    			return true;
	    		// Show user a list of waypoints and allow him to chose one
	            // Create and show the dialog.
	    	    FragmentTransaction ft = getFragmentManager().beginTransaction();
 	            DialogFragment newFragment = new MyDialogFragment();
	            Bundle args = new Bundle();
	            args.putParcelableArrayList(WPList_K, wplist);
	            newFragment.setArguments(args);
	            newFragment.show(ft, "dialog");
	    		return true;
	    		
	    	case R.id.create_wp:
	    		item.setChecked(!item.isChecked());   // toggle the setting
	    		create_wp_forStart = item.isChecked();
	    		return true;
	    		
	    	case R.id.place_wp:
	    		item.setChecked(!item.isChecked());   // toggle the setting
	    		place_wp_forStart = item.isChecked();
	    		System.out.println("place_wp_forStart = " + place_wp_forStart);
	    		return true;
	    		
	    	case R.id.action_settings:
	    		// Do some testing here
//	    		showMsg("Settings not implemented yet");
/*	    		
	    		VisibleRegion vr = gMap.getProjection().getVisibleRegion();
	    		LatLngBounds llBnds = vr.latLngBounds;
	    		double spanLat = Math.abs(llBnds.northeast.latitude - llBnds.southwest.latitude);
	    		double spanLong = Math.abs(llBnds.southwest.longitude - llBnds.northeast.longitude);
	    		int factor = 30;
	    		String msg = "ne="+llBnds.northeast +"\nsw="+llBnds.southwest
	    				+ "\n spanLat="+spanLat + " factor="+(spanLat/factor)
	    				+ "\n spanLong="+spanLong + " factor="+(spanLong/factor); 
	    		showMsg(msg);
*/	    		
				// Starts the Settings activity on top of the current activity
				Intent intent0 = new Intent(this, SettingsActivity.class);
				startActivityForResult(intent0, RESULT_SETTINGS);
				System.out.println("M4N starting settings activity");
	    		return true;
	    		
	        // file for route and waypoints both contain waypoints
	    	// after the waypoints are read, how they are treated is different	
	    	case R.id.read_a_route:
	    	case R.id.read_waypoints:
	    	case R.id.read_wp_and_route:
	    		// Set the reqCode depending on the request
	    		int reqCode = ShowRouteAndWaypoints;  
	    		if(itemId == R.id.read_a_route)
	    			reqCode = ShowRoute;
	    		else if(itemId == R.id.read_waypoints)
	    			reqCode = ShowWaypoints;
	    		
	        	// READ waypoints file and load Spinner
	            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
	            intent.setType("file/*");

	            //  Set path and filter incase FileChooser is used
	            String startPath = rootDir + "/Norms/Waypoints/";
	            if(lastWPFile_fn.length() > 5){
	            	int ix1 = lastWPFile_fn.lastIndexOf("/");
	            	if(ix1 > 0) {
	            		startPath = lastWPFile_fn.substring(0,ix1);
	            		System.out.println("startPath="+startPath +"<");
	            	}
	            }
	    		intent.putExtra("START_PATH", new File(startPath)); 
	    		intent.putExtra("TYPE_FILTER", 
	    				         new String[]{".wps", ".nmea", ".kml"});
	    		System.out.println("selectBtn clicked intent="+intent);
	    		Log.d("Maps4Norms selectBtnClicked", "intent="+intent);
	    		startActivityForResult(intent, reqCode);  // Go chose a file and return below
	    		return true;
	    		
	    		
	    	case R.id.clear_all:
	    		clearRouteLinesAndWaypoints();
	    		if(routes != null) routes.clear();
	    		userWPs.clear();
	    		if(wplist != null)      // can be null if nothing read yet
	    			wplist.clear();
	    		return true;
	    	
	    	case R.id.clear_routes:
	    		gMap.clear();
	    		routeLines.clear();
	    		loadWayPoints();
	    		return true;
	    		
	    	case R.id.clear_wps:
	    		gMap.clear();
	    		waypointsMOs.clear();
	    		loadRoutes();
	    		return true;
	    		
	    	case R.id.hide_c_d:
	    		// Hide all the course and distance markers saved in markerLines
	    		Collection<MarkerLineState> colOfMLS = markerLines.values();
	    		for(MarkerLineState mls : colOfMLS)
	    			mls.hideMarkers();
	    		return true;
	    		
	    	case R.id.reset_marker_titles:
	    		// reset marker titles to the default image for markers in markerStates
	    		Collection<MarkerState> colOfML = markerStates.values();
	    		for(MarkerState ms : colOfML)
	    			ms.resetIcon();
	    		return true;
	    		
	    		//  Following for setting marks to measure distance
		    case R.id.measureDistance:
		    	valueTv.setText("0 mi");   // Hard code for testing <<<<<<<<,
		    	// Toggle the switch
		    	if(topCenterOverlay.isShown()) {
		    		topCenterOverlay.setVisibility(View.INVISIBLE);	
		    		clearDM();   // get rid of the distance measuring markers
		    	}else {
		    		topCenterOverlay.setVisibility(View.VISIBLE);
		    	}
	    		measuringDistance = topCenterOverlay.isShown();  // follow the setting
		    	return true;
		    	
		    case R.id.saveMarks:
		    	Dialogs.saveMarks(this, trace);
		    	return true;
		    	
		    case R.id.loadMarks:
		    	 topCenterOverlay.setVisibility(View.VISIBLE);
		    	 measuringDistance = topCenterOverlay.isShown();
		    	 Dialogs.loadMarks(this, trace);
		    	 return true;
		    	 
		    // --- Following for Updating a route ---
		    // These should be radio buttons - only one at a time
		    case R.id.addMarker:
		    	updateMode = UpdateMode.AddMarker; 
		    	System.out.println("M4N addMarker selected #routes="+routes.size()
		    			           +" #routeLines="+routeLines.size() + ", #markerStates="+markerStates.size()
		    			           + " #waypointMOs="+waypointsMOs.size() +", #wplist=" + wplist.size());
		    	return true;
		    	
		    case R.id.undoUpdate:
		    	System.out.println("M4N no code yet");
		    	return true;
		    	
		    case R.id.moveMarker:
		    	updateMode = UpdateMode.MoveMarker;
		    	System.out.println("M4N no code yet");
		    	return true;
		    	
		    case R.id.deleteMarker:
		    	updateMode = UpdateMode.DeleteMarker;
		    	System.out.println("M4N no code yet");
		    	return true;
		    	
		    case R.id.renameMarker:
		    	updateMode = UpdateMode.RenameMarker;
		    	System.out.println("M4N no code yet");
		    	return true;
		    	
		    case R.id.noUpdates:
		    	updateMode = UpdateMode.Nothing;
		    	return true;
		    	
		    	
		    case R.id.writeRoute:
		    	System.out.println("M4N writeRoute #routes="+routes.size()
 			           +" #routeLines="+routeLines.size() + ", #markerStates="+markerStates.size()
 			           + " #waypointMOs="+waypointsMOs.size() +", #wplist=" + wplist.size());
		    	// Rename the current version and write data out to current name
		    	if(routes.size() != 1) {
		    		showMsg("'Write route' only allowed with single route");
		    		return true;  
		    	}
		    	Route route = routes.get(0); // get one an only route
		    	File currentFile = new File(route.fileName);
		    	if(!currentFile.exists()){
		    		showMsg("Route file:"+currentFile +" not found?");
		    		return true;
		    	}
		    	int ix1 = route.fileName.lastIndexOf(".");
		    	String pathPrefix = route.fileName.substring(0, ix1) + "_";
		    	String ext = route.fileName.substring(ix1);  // .wps
		    	// Rename current version out of the way
	 	        File file = currentFile;
	 	        int sfx = 0;
	 	        // Find unused name
	 	        while(file.exists()) {
	 	           file = new File(pathPrefix + sfx++ + ext);
	 	        }
	 	        if(sfx > 0) {
	 	           currentFile.renameTo(file);  // rename current version to new slot
	 	           showMsg("Current file renamed to "+file);
	 	        }
	 	        // Now write out new contents
	    		try{
	    			FileWriterClass fwc = WayPointFileIO.getFileWriter(route.fileName);
	    			fwc.writeWPs(route.waypoints);
	    			fwc.closeFile();
	    		    Toast.makeText(this, "Wrote " + route.waypoints.size() +" waypoints to " 
	    			               + route.fileName,
	    		    		       Toast.LENGTH_SHORT).show();
	    		}catch(Exception x){
	    			showMsg("error writing waypoint file e="+x);
	    			x.printStackTrace();
	    		}
	    		// reset some flags
	    		route.routeUpdated = false; 
	    		routeHasBeenUpdated = false;         // not needed after save
	    		updateMode = UpdateMode.Nothing;    //  Reset update mode
	    	    return true;
		    	 
	    	//---------------------------------------------------
		    case R.id.createWPAtMyLoc:
		    	createWPAtMyLoc = !createWPAtMyLoc;  // toggle the setting
		    	return true;
		    	
		    case R.id.searchForLocation:
		    	doSearchForLocation();
		    	return true;
	    		
		    //  Following for debugging
		    case R.id.savestdout:
		    	System.out.println("savestdout checked="+item.isChecked());
		    	// toggle saving
		    	if(item.isChecked()){
		    		SaveStdOutput.stop();
		    		item.setChecked(false);
		    		showMsg("Ended SaveSTD of output");
		    	}else{
		            java.util.Date dt =  new java.util.Date();
		            String fn = LogFilePathPfx + sdf.format(dt) + ".txt";   // 2014-02-02T193504
		            /*System.out.println*/showMsg("starting SaveSTD fn="+fn);
		           try {
		   			SaveStdOutput.start(fn);
		   		} catch (IOException e) {
		   			e.printStackTrace();
		   		}
		    		item.setChecked(true);
		    	}
		    	return true;

		    case R.id.queryStatus:
		    	// Build a display about the waypoints and routes
		    	String msg = "Routes:\n number routes="+ (routes == null ? 0 : routes.size());
		    	if(routes != null){
		    		for(int i=0; i < routes.size(); i++)
		    			msg += "\n " +routes.get(i).routeName +" nbr wps=" + routes.get(i).waypoints.size();
		    	}
		    	msg = msg + "\nWaypoints:\n number of waypoints=" + wplist.size()
		    	             + "\n number waypoint lists="+ (waypoints == null ? 0 : waypoints.size());
		    	if(waypoints != null) {
		    		for(int i=0; i < waypoints.size(); i++)
		    			msg += "\n   " + waypoints.get(i).routeName + " nbr wps="+waypoints.get(i).waypoints.size();
		    	}
		    	msg += "\n User waypoints="+userWPs.size();
		    	msg += "\nUser waypoint file=" + UserWP_Folder + userWP_fn;
		    	showMsg(msg);
		    	return true;
	    		
	        case R.id.about:
	            showMsg("Norm's Map program\n"
	            		+ Version
	            		+ "email: radder@hotmail.com");
	            return true;
	            
	        case R.id.exit:
	        	finish();
	        	return true;

	    		
	    	default:
	    		return super.onOptionsItemSelected(item);
	    }
	}
	
	//-----------------------------------------------------
	private void doSearchForLocation() {
		// Get the name from the user
    	AlertDialog.Builder alert = new AlertDialog.Builder(MyMapActivity.this);
    	alert.setTitle("Search for location");
    	alert.setMessage("Enter location");

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(MyMapActivity.this);
    	alert.setView(input);

    	alert.setPositiveButton("Search", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        		new GeocoderTask().execute(input.getText().toString());	
        	}  
        });

    	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    	  public void onClick(DialogInterface dialog, int whichButton) {
    	    // Canceled.
    	  }
    	});

    	alert.show();
	
	}
	/**
	 *   From Thomas Hoffman package de.j4velin.mapsmeasure
	 * Based on
	 * http://wptrafficanalyzer.in/blog/android-geocoding-showing-user-input
	 * -location-on-google-map-android-api-v2/
	 * 
	 * @author George Mathew
	 * 
	 */
	private class GeocoderTask extends AsyncTask<String, Void, Address> {

		String locName = null;  // To save location name
		@Override
		protected Address doInBackground(final String... locationName) {
			// Creating an instance of Geocoder class
			Geocoder geocoder = new Geocoder(getBaseContext());
			try {
				// Get only the best result that matches the input text
				locName = locationName[0];  // save to pass to onPostExecute()
				List<Address> addresses = geocoder.getFromLocationName(locationName[0], 1);
				return addresses != null && !addresses.isEmpty() ? addresses.get(0) : null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Address address) {
			if (address == null) {
				String toastMsg = getString(R.string.no_location_found);
	    		WifiManager wifiManager = (WifiManager) MyMapActivity.this.getSystemService(Context.WIFI_SERVICE); 
	    		if(!wifiManager.isWifiEnabled())  // Append msg if no wifi
	    			toastMsg += " Check wifi is on";
				Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_LONG).show();
			} else {
				LatLng theLoc = new LatLng(address.getLatitude(), address.getLongitude());
				// Move to that location
				gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(theLoc,
						Math.max(10, gMap.getCameraPosition().zoom)));
				// Put marker on the location
				createWPAtLoc(theLoc, locName);
			}
		}
	}

		
	//----------------------------------------------------------
	//  Handle what selected activity found
	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		System.out.println("Maps4Norm1 onActRes reqCode="+reqCode 
				            + ", resCode="+resCode+", intent="+data);
		
		// Intercept misc intents before waypoint handling
		if(reqCode == RESULT_SETTINGS) {
	        setMyLocation = prefs.getBoolean(SetMyLocS_K, false);
	        showFeetIfShort = prefs.getBoolean(ShowFeetIfShort_K, false);
	        customMarkerSize = Integer.parseInt(prefs.getString("customMarkerSize", ""+customMarkerSize));
	        useCustomMarker = prefs.getBoolean("useCustomMarker", useCustomMarker);
            System.out.println("M4N onActRes setMyLocation="+setMyLocation 
            		          + " markerSize="+customMarkerSize);
	        if(setMyLocation) {
	        	turnOnMyLocation();
	        } 	
			return;  // done
		}

		
	   	String wpFN = selectWP_file(); 
	   	
		if(resCode == RESULT_OK) {
			String filePath = data.getStringExtra("FileName");
            wpFN = data.getData().getPath();         // Here is the standard place for response
            System.out.println("Maps4Norm2 onActRes filePath="+filePath+"< wpFN="+wpFN+"<");
            // MapTests2 onActRes filePath=/storage/emulated/0/Norms/Waypoints/Europe.wps< 
            //                        wpFN=/storage/emulated/0/Norms/Waypoints/Europe.wps<
 
		}else {
			System.out.println("onActRes ??? resCode="+resCode);
			return;       //????  what to do here
		}
		
		if(reqCode == FileChosenForSave) 
		{
			// User chose a file to write to 
//			  showMsg("Chose this as output file:"+wpFN);
			File wpFile = new File(wpFN);
			String theFN = wpFile.getName();
			if(theFN.endsWith(WPFile_Ext)){
				userWP_fn = theFN;     // save the user's choice
				System.out.println("M4N set userWP_fn="+userWP_fn);
			}else{
				System.out.println("M4N unknown file chosen wpFN="+wpFN);
			}
			return;
		}
		
		// Intercept misc intents before waypoint handling
		if(reqCode == RESULT_SETTINGS) {
	        setMyLocation = prefs.getBoolean(SetMyLocS_K, false);
	        showFeetIfShort = prefs.getBoolean(ShowFeetIfShort_K, false);
	        customMarkerSize = Integer.parseInt(prefs.getString("customMarkerSize", ""+customMarkerSize));
	        useCustomMarker = prefs.getBoolean("useCustomMarker", useCustomMarker);
	        System.out.println("M4N onActRes setMyLocation="+setMyLocation 
  		          + " markerSize="+customMarkerSize);
	        if(setMyLocation) {
	        	turnOnMyLocation();
	        } 	
			return;  // done
		}
		
        // Read the waypoints into a list
     	wplist = readWaypoints(wpFN);
		Log.d("MapsTest onActRes2","read "+ wplist.size() +" waypoints from " + wpFN);
	    Toast.makeText(this, "Read " + wplist.size() +" waypoints from " + wpFN,
 		       Toast.LENGTH_SHORT).show();

		
    	if(wplist.size() == 0) {
    		return;  // exit if nothing read
    	}
    	
    	// Create a Route object to hold the info for this route/list of waypoints
    	Route route = new Route(wpFN);
    	System.out.println("M4N created route fn="+wpFN);
    	route.setWaypoints(wplist);
    	
    	// Save this filename for next time
    	lastWPFile_fn = wpFN;  // save for current use
        try{
			FileOutputStream fos = openFileOutput(LastWPFileReadFN, Context.MODE_PRIVATE);
			fos.write(wpFN.getBytes());
			fos.close();
	    }catch(Exception x){
		  x.printStackTrace();
	    }
 
    	//  HERE need to know if route or waypoints (Markers)
    	if(reqCode == ShowWaypoints || reqCode == ShowRouteAndWaypoints){
    		if(waypointsMOs == null)
    			waypointsMOs = new ArrayList<MarkerOptions>();
    		float hue = MarkerColors[hueIdx++];  // set the hue for this set of markers
    		if(hueIdx >= MarkerColors.length)
    			hueIdx = 0;  // reset
    		route.hue = hue;    // save
    		BitmapDescriptor bmd = BitmapDescriptorFactory.defaultMarker(hue);
    		if(useCustomMarker){
    			int color = getSomeColor();
     			bmd = getCustomMarkerImage(color, customMarkerSize);
     			route.setMarkerColor(color);
    		}
    		route.setBMD(bmd);
    		
    		// Build a list of waypoints as markers
    		for(WayPoint wpx : wplist) {
	    		MarkerOptions mo = new MarkerOptions()
	    	     .position(wpx.getLocation())
	    	     .title(wpx.getName())
	    	     .snippet(wpx.latLongText())
	    	     .icon(bmd); // BitmapDescriptorFactory.defaultMarker(hue));
	    	  try{ // DEBUG to trap problem in kml file
	    		Marker aMarker = gMap.addMarker(mo);
			    MarkerState ms = new MarkerState(aMarker, mo.getIcon(), wpx,
			    		                         new String[]{wpx.getName(), wpx.latLongText()});
			    markerStates.put(aMarker, ms);
//			    showMsg("wp loc="+wpx.getLocation() +", mrkr loc="+aMarker.getPosition());

	    		waypointsMOs.add(mo);    // add to this list ???? what if >1 list???
	    	  }catch(Exception x){
	    			x.printStackTrace();
	    			System.out.println("Exception with wp="+wpx);
	    			if(wpx.getLocation() == null){
	    				wpx.setLocation(0, 0);
	    				wpx.setName("<No Location>");
	    			}
	    	  }
    		}  // end for() through wplist
    		
    		// Save the list of waypoints that are not in a route
    		if(reqCode == ShowWaypoints) {
    	       	if(waypoints == null)
    	    		waypoints = new ArrayList<Route>();  // make sure no NPE
    	    	waypoints.add(route);
    		}
    		startPt = null;   // don't go to a point when loading waypoints only
    	}  // end waypoints section
    	
    	// Following section for routes
    	if(reqCode == ShowRoute || reqCode == ShowRouteAndWaypoints)  
    	{
	    	// Build the route and show it
    		if(routeLines == null)
    			routeLines = new ArrayList<PolylineOptions>();

    		int routeColor = getSomeColor();
    		route.setColor(routeColor);                 // save
    		drawRouteLines(route);
/*    		
	    	PolylineOptions plo = new PolylineOptions().width(routeWidth).color(routeColor);
 	    	
	    	for(WayPoint wpx : wplist) {
	    		LatLng llx = wpx.getLocation();
	    		plo.add(llx);
	    	}
	    	routeLines.add(plo);    // save for restart
	        Polyline pl = gMap.addPolyline(plo);
	        route.setLines(plo);
*/
	        WayPoint wp1 = wplist.get(0);
	    	startPt = wp1.getLocation();   // use first wp as the start point

	       	if(routes == null)
	    		routes = new ArrayList<Route>();  // make sure no NPE
	    	routes.add(route);
   	}  // end routes section
    	
     		
    	if(startPt != null)  // go to a starting point if have one
    		gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPt, initialZoom));
	}	// end onActivityResult() - waypoint file was chosen

	//------------------------------------------------
	private void clearRouteLinesAndWaypoints() {
		gMap.clear(); 
		routeLines.clear();   // get rid of lists
		waypointsMOs.clear();
		markerStates.clear();
	}
	// Draw the markers for the waypoints on this route
	private void drawRouteMarkers(Route route){
		// Build a list of waypoints as markers
		for(WayPoint wpx : route.waypoints) {
    		MarkerOptions mo = new MarkerOptions()
    	     .position(wpx.getLocation())
    	     .title(wpx.getName())
    	     .snippet(wpx.latLongText())
    	     .icon(route.getBMD());   // BitmapDescriptorFactory.defaultMarker(hue));
    	  try{ // DEBUG to trap problem in kml file
    		Marker aMarker = gMap.addMarker(mo);
		    MarkerState ms = new MarkerState(aMarker, mo.getIcon(), wpx,
		    		                         new String[]{wpx.getName(), wpx.latLongText()});
		    markerStates.put(aMarker, ms);
//		    showMsg("wp loc="+wpx.getLocation() +", mrkr loc="+aMarker.getPosition());

    		waypointsMOs.add(mo);    // add to this list ???? what if >1 list???
    	  }catch(Exception x){
    			x.printStackTrace();
    			System.out.println("Exception with wp="+wpx);
    			if(wpx.getLocation() == null){
    				wpx.setLocation(0, 0);
    				wpx.setName("<No Location>");
    			}
    	  }
		}  // end for() through wplist	
	}
	
	// draw the lines between the waypoint markers
	private void drawRouteLines(Route route) {
    	PolylineOptions plo = new PolylineOptions().width(routeWidth).color(route.routeColor);
	    	
    	for(WayPoint wpx : route.waypoints) {
    		LatLng llx = wpx.getLocation();
    		plo.add(llx);
    	}
    	routeLines.add(plo);    // save for restart
        Polyline pl = gMap.addPolyline(plo);
        route.setLines(plo);
	}
	
	// Load the lines between the waypoints on a route
	private void loadRoutes() {
		if(routeLines  != null && routeLines.size() > 0) {
			for(PolylineOptions plo : routeLines) {
				gMap.addPolyline(plo);
			}
		}
	}
	// Load the markers and save the MarkerState
	private void loadWayPoints() {
		if(waypointsMOs != null && waypointsMOs.size() > 0){
			markerStates = new HashMap<Marker, MarkerState>();
			try{
			 for(MarkerOptions mo : waypointsMOs){
				Marker aMarker = gMap.addMarker(mo);  //<<< Getting following error here
				//The concrete class implementing IObjectWrapper must have exactly *one* 
				// declared private field for the wrapped object.  
				//Preferably, this is an instance of the ObjectWrapper<T> class.

				// Also create new icons for toggling
			    MarkerState ms = new MarkerState(aMarker, mo.getIcon(), null,
                                           new String[]{mo.getTitle(), mo.getSnippet()});
			    markerStates.put(aMarker, ms);
			 }  // end for() 
			}catch(Exception x) {
				String msg = x.getMessage();
				showMsg("Error in loadWayPoints:\n"+msg +"\n #waypointsMOs="+waypointsMOs.size());
			}
		}else{
			System.out.println("M4N loadWayPoints without empty waypointsMOs");
		}
	}
	
	//----------------------------------------------------------
	//  Select waypoint file
	String selectWP_file() {
		// Hardcode now for testing
		return "/sdcard/Norms/Waypoints/BahamaViaBimini.wps";
	}
	
	final String WP_Comment = "#";   // .wps comment records start with this
	
	//------------------------------------------------------------------
	//  Read waypoint file, parse it and save WayPoints in list
	private ArrayList<WayPoint> readWaypoints(String wpfn) {
		ArrayList<WayPoint> wpList = new ArrayList<WayPoint>();
		try{
			WayPointFileIO.FileReaderClass wpFRC = WayPointFileIO.getFileReader(wpfn);
			wpList = wpFRC.getWaypoints();
			if(wpList == null) {  // Catch null returned here
				showMsg("getWaypoints() returned null for " + wpfn);
			}
		}catch(Exception x){
			showMsg("Error with waypoints file: "+wpfn
					+ "\n"+x.getMessage());
			x.printStackTrace();
		}
		return wpList;
	}
	
	//--------------------------------------
	// save the location and title of the marker for the start location
	private void setStartLocation(Marker mrkr){
		LatLng pos = mrkr.getPosition();
		String name = mrkr.getTitle();
		String desc = mrkr.getSnippet();
		WayPoint wp = new WayPoint(name, pos);
		float zoomLvl = gMap.getCameraPosition().zoom;  //  How to get current zoom ???
		try{
			// Build a String with the waypoint data and write it to internal storage
			String data = WayPoint.WPEqual + wp.toString()+ WayPoint.SepStr + zoomLvl;
			System.out.println("setStartLocation data="+data);
			FileOutputStream fos = openFileOutput(StartLocationFN, Context.MODE_PRIVATE);
			fos.write(data.getBytes());
			fos.close();

		}catch(Exception x){
			x.printStackTrace();
			return;
		}
		showMsg("Set Start location to:\n" + name + "\n" + wp.latLongText() 
				 + "\nAt zoom level=" + (int)zoomLvl);
	}
	
	//---------------------------------------------
	// Build bitmap for custom marker
	private BitmapDescriptor getCustomMarkerImage(int color, int dotSize) {
		Bitmap mDotMarkerBitmap = Bitmap.createBitmap(dotSize, dotSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mDotMarkerBitmap);
		Drawable shape = getResources().getDrawable(R.drawable.map_dot_red);
		shape.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
		shape.setColorFilter(getSomeColor(), PorterDuff.Mode.SRC);     // SRC gives nice color
		shape.draw(canvas);
        BitmapDescriptor bd = BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap);
		return bd;
	}
	
	//==================================================================================
	// Define class to display allow user to select a waypoint from a list of wps
    public static class MyDialogFragment extends DialogFragment implements OnItemSelectedListener {
    	ArrayList<WayPoint> wpl;
    	
    	public MyDialogFragment() {} // empty constructor
    	
       	
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            // Get the list of waypoints
            wpl = getArguments().getParcelableArrayList(WPList_K);
            System.out.println("MDF onCreate() wpl size=" + (wpl == null? "null" : wpl.size()));

        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                				 Bundle savedInstanceState) {

            View v = inflater.inflate(R.layout.show_list, container, false);
            
//            View tv = v.findViewById(R.id.text);
//            ((TextView)tv).setText("Dialog #" + mNum + ": using style "
 //                   + getNameForNum(mNum));
            
    		Spinner spinner = (Spinner)v.findViewById(R.id.the_list);
    		ArrayList<WP_Wrapper> wpwA = new ArrayList<WP_Wrapper>();
    		for(WayPoint wp : wpl)
    			wpwA.add(new WP_Wrapper(wp));
    		Collections.sort(wpwA);
    			
    		ArrayAdapter<WP_Wrapper> aa = new ArrayAdapter<WP_Wrapper>(v.getContext(), 
    				                              android.R.layout.simple_spinner_item, wpwA);
    		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		spinner.setAdapter(aa);
    		spinner.setOnItemSelectedListener(this);

            // Watch for button clicks.
            Button button = (Button)v.findViewById(R.id.donebtn);
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                     //  Return the choice
           		    ((MyMapActivity)getActivity()).setChoice(choice);
           		 
                 	dismiss();   // end this dialog
                }
            });
            Button button2 = (Button)v.findViewById(R.id.cancelbtn);
            button2.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                     //  Return the choice as null
           		    ((MyMapActivity)getActivity()).setChoice(null);
           		 
                 	dismiss();   // end this dialog
                }
            });
            

            return v;
        } // end onCreateView()
        
        //------------------------------------------------------
        // Need class for sorting and showing  
        class WP_Wrapper  implements Comparable<WP_Wrapper>{
        	WayPoint wp;
        	WP_Wrapper(WayPoint w) {
        		wp = w;
        	}
        	
    	   @Override
    	    public int compareTo(WP_Wrapper wpw) {
     	        return getName().compareTo(wpw.getName()); 
       	    }

    	    public String getName() {
    	    	return wp.getName();
     	    }
    	    public WayPoint getWP() {
    	    	return wp;
    	    }
        	public String toString() {
        		return wp.getName();
         	}
        } // end class WP_Wrapper
 
    	
        WayPoint choice = null;   // User's choice is saved here and return
        
       	//-----------------------------------------------------------
       	// Define OnItemSelectedListener methods
       	@Override
       	public void onItemSelected(AdapterView<?> parent, View arg1, int pos, long id) {
       		Log.d("onItemSelected", "arg1="+arg1+", pos="+pos +", is="+id);
       		WP_Wrapper wpw = (WP_Wrapper) parent.getItemAtPosition(pos);  // Save
       		choice = wpw.getWP();
      	}
       	@Override
       	public void onNothingSelected(AdapterView<?> arg0) {
       		Log.d("onNothingSelected", "arg0="+arg0);
       		
       	}

    }  // end class MyDialogFragment
    
    // The above class will call here to set what waypoint was chosen
    public void setChoice(WayPoint wp){
    	if(wp == null){
    		showMsg("User cancelled choosing a waypoint");
    		return;  // done
    	}
    	LatLng wpPos = wp.getLocation();   // get lat/long for lookup and move
    	// Move to the marker
        gMap.moveCamera(CameraUpdateFactory.newLatLng(wpPos));
    	
    	// Toggle the marker
        setMarkerAt(wpPos);
          
    }
    // Search the markerStates map for a Marker at this location
    void setMarkerAt(LatLng thisLoc) {
      	Collection<MarkerState> markerStatesC = markerStates.values();
      	System.out.println("setMA ll="+thisLoc.toString() +" nbrMS="+markerStatesC.size());
    	for(MarkerState ms : markerStatesC) {
    		System.out.println("ms="+ms);
    		if(Utils.areLocationsClose(thisLoc, ms.getMarkerLocation())) {
    			System.out.println("setChoice markerState="+ms); //  NOT SHOWN???
    			ms.showOurIcon();
    			break;          // exit the loop when done
    		}
    	}  // end for()
    }
    //------------------------------------------------------------
    // Measuring distance methods:
	/**
	 * Adds a new point, calculates the new distance and draws the point and a
	 * line to it
	 * 
	 * @param p
	 *            the new point
	 */
	void addPoint(final LatLng p) {
		if (!trace.isEmpty()) {
			lines.push(gMap.addPolyline(new PolylineOptions().color(COLOR_LINE).add(trace.peek()).add(p)));
			distance += SphericalUtil.computeDistanceBetween(p, trace.peek());
		}
		points.push(drawCircle(p));
		trace.push(p);
		updateValueText();
	}
	/**
	 * Draws a circle at the given point.
	 * 
	 * Should be called when the users touches the map and adds an entry to the
	 * stacks
	 * 
	 * @param center
	 *            the point where the user clicked
	 * @return the drawn Polygon
	 */
	private Marker drawCircle(final LatLng center) {
		return gMap.addMarker(new MarkerOptions().position(center).flat(true).anchor(0.5f, 0.5f).icon(marker));
	}

	/**
	 * Resets the map by removing all points, lines and setting the text to 0
	 */
	void clearDM() {
//		gMap.clear();  // There could be other markers 
		// Get rid of our markers
		while(!trace.isEmpty()){
			removeLast();
		}
		trace.clear();
		lines.clear();
		points.clear();
		distance = 0;
		updateValueText();
	}
	/**
	 * Updates the valueTextView at the top of the screen
	 */
	void updateValueText() {
		valueTv.setText(getFormattedString());
	}

	/**
	 * Removes the last added point, the line to it and updates the distance
	 */
	private void removeLast() {
		if (trace.isEmpty())
			return;
		points.pop().remove();
		LatLng remove = trace.pop();
		if (!trace.isEmpty())
			distance -= SphericalUtil.computeDistanceBetween(remove, trace.peek());
		if (!lines.isEmpty())
			lines.pop().remove();
		updateValueText();
	}
	private String getFormattedString() {
		if (distance > 1609)
			return formatter_two_dec.format(distance / 1609.344f) + " mi";
		else
			return formatter_two_dec.format(Math.max(0, distance / 0.3048f)) + " ft";
			
		
	}
    
	@Override
	public void onStop() {
		super.onStop();
    	System.out.println("M4N onStop");
	}
	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	System.out.println("M4N onDestroy");
    	SaveStdOutput.stop();
    }

    
	//----------------------------------------------------	
	//  Show a message in an Alert box
	private void showMsg(String msg) {

		AlertDialog ad = new AlertDialog.Builder(this).create();
		ad.setCancelable(false); // This blocks the 'BACK' button
		ad.setMessage(msg);
		ad.setButton(DialogInterface.BUTTON_POSITIVE, "Clear message", 
		  new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.dismiss();                    
		    }
		});
		ad.show();
	} // end showMsg()

}  // end class MyMapActivity
