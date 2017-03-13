package com.normsstuff.maps4norm;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.normsstuff.waypoints.WayPoint;

//---------------------------------------------------------------
// Define class to manage toggling of marker's icon
class MarkerState {
	boolean showingOurIcon = false;  // true if showing our BM / false if showing default
	Marker mrkr;
	BitmapDescriptor defaultIcon;
	BitmapDescriptor ourIcon;    
	BitmapDescriptor currentIcon;
	WayPoint waypoint;
	String[] text;
	
	MarkerState(Marker m, BitmapDescriptor bmd, WayPoint wp, String[] text){
		defaultIcon = bmd;
		currentIcon = bmd;
		waypoint = wp;
		this.text = text;	
		mrkr = m;
	}
	// Toggle the marker's icon
	public void toggleMarker() {
		toggleMarker(mrkr);
	}
	public void toggleMarker(Marker mrkr){
		if(showingOurIcon){
			showingOurIcon = false;
			mrkr.setIcon(defaultIcon);
			currentIcon = defaultIcon;
		}else{
			showingOurIcon = true;
			if(ourIcon == null){
				buildIcon();
			}
			mrkr.setIcon(ourIcon); 
			currentIcon = ourIcon;
		}
	}
	public BitmapDescriptor getCurrentIcon() {
		return currentIcon;
	}
	
	public void resetIcon() {
		if(!showingOurIcon)
			return;  // don't waste our time
		setDefaultIcon();
 	}
	public void setDefaultIcon() {
   		mrkr.setIcon(defaultIcon);   //IllegalArgumentException: Released unknown bitmap reference
		showingOurIcon = false;
	}
	// Show our icon if not being shown
	public void showOurIcon() {
		if(showingOurIcon)
			return;
		toggleMarker();  // otherwise toggle it on
	}
	public LatLng getMarkerLocation() {
		return mrkr.getPosition();
	}
/*	
	public boolean isMarkerAtLocation(LatLng ll){
		return mrkr.getPosition().equals(ll);
	}
*/	
	public void buildIcon() {
		int[] colors = {Color.RED, 0xFFFFFFC8}; 
		Bitmap bmp= MyMapActivity.getCustomMarkerBM(text , colors, true);
		ourIcon = BitmapDescriptorFactory.fromBitmap(bmp);
	}
	
	public String toString() {
		return "Mrkr location="+mrkr.getPosition() + " sOI=" + showingOurIcon;
	}
} // end class
 