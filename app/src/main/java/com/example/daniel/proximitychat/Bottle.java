package com.example.daniel.proximitychat;

import android.location.Location;

/**
 * Created by Daniel on 5/3/2016.
 */
public class Bottle implements Comparable<Bottle> {
    float dist;
    double lat;
    double lon;
    String message;
    long timestamp;
    String uid;

    public Bottle(String uid,float dist, double lat, double lon, String message,long timestamp){
        this.dist = dist;
        this.lat = lat;
        this.lon = lon;
        this.message = message;
        this.timestamp = timestamp;
        this.uid = uid;

    }
    public float getDist(){
        return dist;
    }
    public String getMessage(){
        return message;
    }
    public String getUid(){
        return uid;
    }
    public void setDist(double lonCurrent,double latCurrent){
        float[] results = new float[2];
        Location.distanceBetween(lonCurrent, latCurrent,
                lat, lon, results);
        this.dist = results[0];
    }
    public long getTimestamp(){
        return timestamp;
    }
    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }
    public int compareTo(Bottle b){
        if(this.timestamp<b.getTimestamp()){
            return -1;
        }
        else{
            return 1;
        }
    }
    public void setUid(String uid){
        this.uid = uid;

    }
    public String toString(){
        return "Distance: " + dist + " Lat: " + lat + " Lon: " + lon + " Message: " + message;
    }
}
