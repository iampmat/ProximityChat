package com.example.daniel.proximitychat;
/**
 * Created by Daniel on 5/3/2016.
 */
public class Message {
    String message;
    double lat;
    double lon;
    long timestamp;
    String uid;
    public Message(){}
    public Message(String uid,String message, double lat, double lon,long timestamp){
        this.message = message;
        this.lat = lat;
        this.lon = lon;
        this.timestamp = timestamp;
        this.uid = uid;
    }
    public String getMessage(){
        return message;
    }
    public double getLat(){
        return lat;
    }
    public double getLon(){
        return lon;
    }
    public long getTimestamp(){
        return timestamp;
    }
    public String getUid(){
        return uid;
    }
    public void setUid(){
        this.uid = uid;
    }
    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }
    public void setTitle(String message){
        this.message = message;
    }
    public void setQuestion(double lat){
        this.lat = lat;
    }
    public void setOptions(double lon){
        this.lon = lon;
    }
}
