package com.example.daniel.proximitychat;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel on 2/26/2016.
 */
public class ChatRoomArrayAdapter extends ArrayAdapter {
    private final Context context;
    ArrayList<ChatRoom> rooms;
    Date date;
    Date endDate;
    boolean delete;
    public ChatRoomArrayAdapter(Context context, Boolean delete, ArrayList<ChatRoom> rooms) {
        super(context, R.layout.chatroom_list,rooms);
        this.context = context;
        this.rooms = rooms;
        this.delete = delete;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        System.out.println("generating views");
        ChatRoom room = rooms.get(position);
        //ypeface font = Typeface.createFromAsset(context.getAssets(), "fontawesome.ttf");
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.chatroom_list, parent, false);

        TextView titleText = (TextView) rowView.findViewById(R.id.roomTitle);
        TextView chatroomDistance = (TextView)rowView.findViewById(R.id.chatroomDistance);
       /* TextView shareButton = (TextView)rowView.findViewById(R.id.shareItem);
        TextView locationButton = (TextView)rowView.findViewById(R.id.markItem);
        shareButton.setTypeface(font);
        locationButton.setTypeface(font);*/
        titleText.setText(room.getTitle());
        chatroomDistance.setText(convertMeterstoFeet(room.getDist()) + " feet");

        // Change icon based on name
//        String s = values[position];



        return rowView;
    }

    public String formatDate(){
        long startTime = date.getTime();
        long endTime = endDate.getTime();
        Calendar calendar = Calendar.getInstance();
        long current = calendar.getTimeInMillis();
        long diff = startTime - current;
        long day = TimeUnit.MILLISECONDS.toDays(diff);
        long hour = TimeUnit.MILLISECONDS.toHours(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if(current>startTime){
            return "Now";
        }
        if(day <1){
            if(hour<12 && hour > 0){
                return hour + " hours";
            }
            else if(hour == 0){
                return minutes + " minutes";
            }
            else{
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                return dateFormat.format(date);
            }
        }
        else if(day>=1){
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return dateFormat.format(date);
        }
        return "Not yet";
    }

    public int convertMeterstoFeet(float meters){
        double toFeet = meters;
        toFeet = meters*3.2808;  // offic// ial conversion rate of Meters to Feet
        return (int)Math.round(toFeet);
    }
}
