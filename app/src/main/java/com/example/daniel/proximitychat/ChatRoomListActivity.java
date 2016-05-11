package com.example.daniel.proximitychat;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ChatRoomListActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, ResultCallback<Status> {

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    ArrayList<ChatRoom> rooms = new ArrayList<>();
    ArrayList<ChatRoom> allRooms = new ArrayList<>();
    ArrayAdapter chatroomList;
    public ListView listView;
    Firebase ref;
    private GoogleApiClient c = null;
    LocationRequest req;
    double[] coords;
    int radius;
    Context context;
    Location currentBestLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room_list);
        radius = 30;
        coords = new double[2];
        Firebase.setAndroidContext(this);
        context = this;
        ref = new Firebase("https://suitepolls.firebaseio.com/");
        listView = (ListView) findViewById(R.id.eventList);
        chatroomList = new ChatRoomArrayAdapter(this, false, rooms);
        listView.setAdapter(chatroomList);
        buildGoogleApiClient();
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();

        if(settings.getString("uid","").equals("")){
            buildAlertDialog();
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                // When clicked, show a toast with the TextView text
                                                Intent intent = new Intent(context, ChatRoomActivity.class);
                                                TextView a = (TextView) findViewById(R.id.roomTitle);

                                                intent.putExtra("roomID", rooms.get(position).getRoomid());
                                                SharedPreferences settings = PreferenceManager
                                                        .getDefaultSharedPreferences(context);
                                                SharedPreferences.Editor editor = settings.edit();
                                                editor.putString("roomID", rooms.get(position).getRoomid());
                                                editor.commit();

                                                context.startActivity(intent);
                                            }
                                        }

        );

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Checking Range");
                        checkRange(radius);
                    }
                });
            }
        }, 0, 30000);
}
    protected synchronized void buildGoogleApiClient() {
        System.out.println("Building Client");
        c = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                // About option clicked.
                    createRoomDialog();
                return true;
            case R.id.menu_change_radius:
                changeRadiusDialog();
                return true;
            case R.id.menu_edit:
                buildAlertDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void addRoom(String title){
        //  double[] coords = getData();
        ChatRoom room = new ChatRoom(title,0,coords[0],coords[1],null);
        Firebase pollref = ref.child("rooms");
        pollref.push().setValue(room);
    }

    public void getRooms(){
        final Firebase ref = new Firebase("https://suitepolls.firebaseio.com/rooms");
        // Attach an listener to read the data at our posts referencef
        // 5 min = 300000
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                System.out.println("There are " + snapshot.getChildrenCount() + " messages");
                allRooms.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    ChatRoom room = postSnapshot.getValue(ChatRoom.class);
                    room.setRoomid(postSnapshot.getKey());
                    room.setDist(coords[0], coords[1]);
                    System.out.println("Title: " + room.getTitle());
                    allRooms.add(room);
                }
                checkRange(radius);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
            }


    public void createRoomDialog(){
        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this).setTitle("Set Chat Room Title")
                .setView(input)
                .setMessage("Enter a Chat Room Title")
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addRoom(input.getText().toString());
                    }
                }).show();

    }

    @Override
    public void onConnected(Bundle bundle) {
        System.out.println("Connected");
        startLocationUpdates();
        getRooms();

    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("Connection Suspended");

    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("Accuracy: " + location.getAccuracy());
        if(isBetterLocation(location,currentBestLocation)) {
            System.out.println("New Best Location");
            currentBestLocation = location;
        }
        coords[0] = currentBestLocation.getLatitude();
        coords[1] = currentBestLocation.getLongitude();
        if(rooms.size()>0) {
            System.out.println("Setting Distance");
            for (ChatRoom r : allRooms) {
                r.setDist(coords[0],coords[1]);
            }
        }
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        System.out.println("Connection Failed");
        c.connect();
        System.out.println(connectionResult.toString());

    }

    @Override
    public void onResult(Status status) {
        System.out.println(status.toString());

    }
    protected void startLocationUpdates() {
        System.out.println("Starting Updates");
        req = new LocationRequest();
        req.setInterval(10); //preferred rate
        req.setFastestInterval(5000); //max rate it can handle
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(c, req, this);
    }
    @Override
    protected void onStart() {
        System.out.println("Connecting");
        c.connect();
        getRooms();
        // registerOnline();
        super.onStart();

    }

    @Override
    protected void onStop() {
        System.out.println("Disconnecting");
        //  unRegisterOnline();
        c.disconnect();
        super.onStop();
    }


    public void checkRange(int meters){
        rooms.clear();
        for(ChatRoom r:allRooms){
            System.out.println("Room: " + r.toString());
            if(r.getDist()<=meters){
                System.out.println("Adding Chatroom");
                rooms.add(r);
            }
        }
        System.out.println("Notifying data set changed");
        chatroomList.notifyDataSetChanged();
    }
    public void changeRadiusDialog(){
        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(radius + "");
        new AlertDialog.Builder(context).setTitle("Change Proximity")
                .setView(input)
                .setMessage("Enter a proximity(in meters) that you want to chat with")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        radius = Integer.parseInt(input.getText().toString());
                        checkRange(radius);
                    }
                }).show();

    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            System.out.println("True Current Location is Null");
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            System.out.println("True because it Is Significantly Newer");
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            System.out.println("False because Significantly Older");
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        System.out.println("Accuracy Delta: " + accuracyDelta);
        boolean isLessAccurate = accuracyDelta >= 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 50;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            System.out.println("True because more accurate");
            return true;
        } else if (isNewer && !isLessAccurate) {
            System.out.println("True because is Newer and is not Less Accurate");
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            System.out.println("True because more it Is Newer and Is not Significatly Lses Accurage and Is from the same provider");
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public double convertMeterstoFeet(double meters){
        double toFeet = meters;
        toFeet = meters*3.2808;  // offic// ial conversion rate of Meters to Feet
        return Math.round(toFeet);
    }
    public void buildAlertDialog(){
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(context);
        final EditText input = new EditText(this);
        input.setText(settings.getString("uid",""));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        new AlertDialog.Builder(context).setTitle("Choose Alias")
                .setView(input)
                .setMessage("Choose a chat room alias")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences.Editor editor = settings.edit();
                        if (!input.getText().toString().equals("")) {
                            editor.putString("uid", input.getText().toString());
                            editor.commit();
                        }
                    }
                }).show();

    }
}
