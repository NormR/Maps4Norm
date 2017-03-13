package com.normsstuff.maps4norm;

import com.google.android.gms.maps.model.LatLng;

public class Utils {
	
	// Check distance between two points
	// 1 deg = 6000ft
	final static double FiveFeet = 0.000833D;
	final static double TwoFeet = 0.000333D;
	
	public static boolean areLocationsClose(LatLng pos1, LatLng pos2) {
		return (Math.abs(pos1.latitude - pos2.latitude) < TwoFeet)
				&& (Math.abs(pos1.longitude - pos2.longitude)< TwoFeet);
	}
/*	
	//--------------------------------------------------------------------------------------------------
    // Compute the distance between two points
	//http://stackoverflow.com/questions/4438244/how-to-calculate-shortest-2d-distance-between-a-point-and-a-line-segment-in-all
	//Compute the dot product AB . AC
	private static double DotProduct(double[] pointA, double[] pointB, double[] pointC)
	{
	    double[] AB = new double[2];
	    double[] BC = new double[2];
	    AB[0] = pointB[0] - pointA[0];
	    AB[1] = pointB[1] - pointA[1];
	    BC[0] = pointC[0] - pointB[0];
	    BC[1] = pointC[1] - pointB[1];
	    double dot = AB[0] * BC[0] + AB[1] * BC[1];

	    return dot;
	}

	//Compute the cross product AB x AC
	private static double CrossProduct(double[] pointA, double[] pointB, double[] pointC)
	{
	    double[] AB = new double[2];
	    double[] AC = new double[2];
	    AB[0] = pointB[0] - pointA[0];
	    AB[1] = pointB[1] - pointA[1];
	    AC[0] = pointC[0] - pointA[0];
	    AC[1] = pointC[1] - pointA[1];
	    double cross = AB[0] * AC[1] - AB[1] * AC[0];

	    return cross;
	}

	//Compute the distance from A to B
	private static double Distance(double[] pointA, double[] pointB)
	{
	    double d1 = pointA[0] - pointB[0];
	    double d2 = pointA[1] - pointB[1];

	    return Math.sqrt(d1 * d1 + d2 * d2);
	}

	//Compute the distance from AB to C
	//if isSegment is true, AB is a segment, not a line.
	public static double lineToPointDistance2D(double[] pointA, double[] pointB, double[] pointC, 
	                                     boolean isSegment)
	{
	    double dist = CrossProduct(pointA, pointB, pointC) / Distance(pointA, pointB);
	    if (isSegment)
	    {
	        double dot1 = DotProduct(pointA, pointB, pointC);
	        if (dot1 > 0) 
	            return Distance(pointB, pointC);

	        double dot2 = DotProduct(pointB, pointA, pointC);
	        if (dot2 > 0) 
	            return Distance(pointA, pointC);
	    }
	    return Math.abs(dist);
	} 
*/
	
    //----------------------------------------------------------------------------------------------------
    //x, y is your target point and x1, y1 to x2, y2 is your line segment.
 
   public static double lineToPointDistance(double x, double y, double x1, double y1, double x2,double y2) {
    
       double A = x - x1;
       double B = y - y1;
       double C = x2 - x1;
       double D = y2 - y1;
       
       double dot = A * C + B * D;
       double len_sq = C * C + D * D;
       double param = dot / len_sq;
       
       double xx, yy;
       
       if (param < 0 || (x1 == x2 && y1 == y2)) {
           xx = x1;
           yy = y1;
       }
       else if (param > 1) {
           xx = x2;
           yy = y2;
       }
       else {
           xx = x1 + param * C;
           yy = y1 + param * D;
       }
       
       double dx = x - xx;
       double dy = y - yy;
 
       return Math.sqrt(dx * dx + dy * dy);
       
    }  // end lineToPointDistance()
   
   //----------------------------------
   // Parse String with Thh:mm:ss format to hours
   public static double parseHours(String label){
	   int ix = label.indexOf("T");
	   if(ix < 0 || (label.charAt(ix+3) != ':') || (label.charAt(ix+6) != ':')
			   || (label.length() < ix+8))
		   return -1.0;  // error
	   String hhS = label.substring(ix+1, ix+3);
	   double hh = Double.parseDouble(hhS);
	   String mmS = label.substring(ix+4, ix+6);
	   double mm = Double.parseDouble(mmS);
	   String ssS = label.substring(ix+7, ix+9);
	   double ss = Double.parseDouble(ssS);
	   return hh + mm/60 + ss/3600;  // in hours
   }



}  // end class Utils
