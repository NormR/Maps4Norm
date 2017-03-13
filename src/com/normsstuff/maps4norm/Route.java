package com.normsstuff.maps4norm;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;
import com.normsstuff.waypoints.WayPoint;

//  12/17/13 - Added save/restore of waypoints in Parcelable logic

// Define a class to hold info for a route
public class Route implements Parcelable {
	String fileName;                // name of file containing wp
	String routeName = "<None>";
	ArrayList<WayPoint>  waypoints;
	PolylineOptions theLines;
	Marker routeInfoMarker;
	int routeColor;
	int markerColor;
	float hue;                // for default marker
	private BitmapDescriptor bmd;    // image for a marker
	boolean routeUpdated = false;
	
	
	//- - - - - - - - - - - - - - - - - - - - -
	// Define fields and methods for Parcelable
	final String WPL_Key = "waypoints";
	final String PLO_Key = "thelines";

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(fileName);
        Bundle bndl = new Bundle();
        bndl.putParcelableArrayList(WPL_Key, waypoints);
        bndl.putParcelable(PLO_Key , theLines);
        out.writeBundle(bndl);
        out.writeInt(routeColor);
        out.writeInt(markerColor);
        out.writeFloat(hue);
//        out.writeBoolean(routeUpdated);
    }

    public static final Parcelable.Creator<Route> CREATOR
                                            = new Parcelable.Creator<Route>() {
        public Route createFromParcel(Parcel in) {
            return new Route(in);
        }

        public Route[] newArray(int size) {
            return new Route[size];
        }
    };
    
    private Route(Parcel in) {
        fileName = in.readString();
        Bundle bndl = in.readBundle();
        theLines = bndl.getParcelable(PLO_Key);
        waypoints = bndl.getParcelableArrayList(WPL_Key);
        routeColor = in.readInt();
        markerColor = in.readInt();
        hue = in.readFloat();
//        routeUpdated = in.readBoolean();
    }
    
    //--------------------------------------------------
    // Define Route class's working methods
	
	public Route(String fn) {  //  constructor
		fileName = fn;
	    int ix = fileName.lastIndexOf("/");
	    int ix2 = fileName.indexOf(".");
	    routeName = fileName.substring(ix+1, ix2);  //Filename only
	}
	
	public void setWaypoints(ArrayList<WayPoint> wpl){
		waypoints = new ArrayList<WayPoint>(wpl);  // Create a new list locally ?? Same order ???
	}
	public void addWaypoint(WayPoint wp, int idx){
		waypoints.add(idx, wp);
		routeUpdated = true;
	}
	public void setLines(PolylineOptions plo){
		theLines = plo;
	}
	public void setColor(int color) {
		routeColor = color;
	}
	public void setMarkerColor(int color){
		markerColor = color;
	}
	public void setBMD(BitmapDescriptor bmd) {
		this.bmd = bmd;
	}
	public BitmapDescriptor getBMD() {
		if(bmd == null)
			return BitmapDescriptorFactory.defaultMarker(hue);
		// Need code here to build a custom marker if needed???
		return bmd;
	}
	public void setInfoMarker(Marker mkr){
		routeInfoMarker = mkr;
	}
	public Marker getInfoMarker(){
		return routeInfoMarker;
	}
	public String toString() {
		return "Route: fN="+ fileName + " #wps="+ (waypoints == null ? null : waypoints.size() 
				 + ", plo="+theLines);
	}
}
