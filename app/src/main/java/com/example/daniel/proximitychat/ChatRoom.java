package com.example.daniel.proximitychat;

import android.location.Location;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Daniel on 5/4/2016.
 */
public class ChatRoom {
    String roomid;
    String title;
    double lat;
    double lon;
    float dist;
    Map<String,Message> messages;
    public ChatRoom(){}
    public ChatRoom(String title,float dist, double lat,double lon,Map<String, Message> messages){
        this.title = title;
        this.lat = lat;
        this.lon = lon;
        this.dist = dist;
        this.messages = messages;

    }
    public String getRoomid(){
        return roomid;
    }
    public String getTitle(){
        return title;
    }
    public float getDist(){
        return dist;
    }
    public double getLat(){
        return lat;
    }
    public double getLon(){
        return lon;
    }
    public Map getMessages() {
        return messages;
    }
    public void setRoomid(String id){
        this.roomid = id;
    }
    public void setMessages(Map<String,Message> messages){
        this.messages = messages;
    }
    public void setTitle(String title){
        this.title = title;
    }
    public void setLat(double lat){
        this.lat = lat;
    }
    public void setLon(double lon){
        this.lon = lon;
    }
    public void setDist(float dist){
        this.dist = dist;
    }
    public void setDist(double lonCurrent,double latCurrent){
        float[] results = new float[2];
        Location.distanceBetween(lonCurrent, latCurrent,
                lat, lon, results);
        this.dist = results[0];
    }
    public String toString(){
        return "Distance: " + dist + " Lat: " + lat + " Lon: " + lon + " ID: " + roomid;
    }
}
