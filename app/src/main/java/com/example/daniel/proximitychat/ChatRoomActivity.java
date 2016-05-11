package com.example.daniel.proximitychat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Query;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;


public class ChatRoomActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, ResultCallback<Status>,SwipeRefreshLayout.OnRefreshListener {

    Firebase ref;
    private GoogleApiClient c = null;
    private final Context context = this;
    LocationRequest req;
    ArrayList<Bottle> bottleList = new ArrayList<Bottle>();
    ArrayList<Bottle> proximityList = new ArrayList<Bottle>();
    LinearLayout chatLayout;
    ScrollView scroll;
    private SwipeRefreshLayout swipeRefreshLayout;
    String uid;
    double[] coords;
    TextView onlineView;
    int radius;
    String roomID;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(context);
        if(settings.getString("uid","").equals("")){
            uid = "anonymous";
        }
        else{
            uid = settings.getString("uid","");
        }
        roomID = settings.getString("roomID", ""/*default value*/);
        coords = new double[2];
        radius = 10;
        chatLayout = (LinearLayout)findViewById(R.id.chatLayout);
        scroll = (ScrollView)findViewById(R.id.scrollView);
        Firebase.setAndroidContext(this);
        removePastElements();
        ref = new Firebase("https://suitepolls.firebaseio.com/");

        buildGoogleApiClient();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
       /* Timer myTimer = new Timer();
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
        }, 0, 30000);*/
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        System.out.println("Building Client");
        c = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

    }

    private double[] getData() {
        double[] coords = new double[2];
        try {
            System.out.println("Posting Data");
            Location loc = LocationServices.FusedLocationApi.getLastLocation(c);
            System.out.println("Location: " + loc);
            System.out.println("Loc: " + loc);
            coords[0] = loc.getLatitude();
            coords[1] = loc.getLongitude();
            Log.v("LOC", "" + loc.getLatitude() + ", " + loc.getLongitude());


        } catch (SecurityException ex) {
            ex.printStackTrace();
        }

        return coords;
    }

    @Override
    public void onConnected(Bundle bundle) {
        System.out.println("Connected");
        startLocationUpdates();
     getMessages();

    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("Connection Suspended");

    }

    @Override
    public void onLocationChanged(Location location) {
        coords[0] = location.getLatitude();
        coords[1] = location.getLongitude();
        if(bottleList.size()>0) {

            for (Bottle b : bottleList) {
                b.setDist(location.getLatitude(), location.getLongitude());
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
        req.setInterval(1000); //preferred rate
        req.setFastestInterval(500); //max rate it can handle
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

    public void storeMessage(View view){

        final Firebase ref = new Firebase("https://suitepolls.firebaseio.com/rooms/" + roomID);
        System.out.println("Key: " +ref.getKey());
        EditText msg = (EditText)findViewById(R.id.msgText);
        String text = msg.getText().toString();
        //  double[] coords = getData();
        Message message = new Message(uid,text,coords[0],coords[1],System.currentTimeMillis());
        Firebase pollref = ref.child("messages");
        pollref.push().setValue(message);
        msg.setText("");
    }
    public float calculateDistance(double lat,double lon){
        float[] results = new float[2];
        //  double[] coords = getData();
        Location.distanceBetween(coords[0], coords[1],
                lat, lon, results);
        float distanceToMessage = results[0];
        return distanceToMessage;
    }
    public Bottle createBottle(Message msg){
        float dist = calculateDistance(msg.getLat(), msg.getLon());
        Bottle b = new Bottle(msg.getUid(),dist,msg.getLat(),msg.getLon(),msg.getMessage(),msg.getTimestamp());
        System.out.println("New Bottle: " + b.toString());
        return b;
    }

    public void getMessages(){
        //  swipeRefreshLayout.setRefreshing(true);
        final Firebase ref = new Firebase("https://suitepolls.firebaseio.com/rooms/"+roomID + "/messages");
        // Attach an listener to read the data at our posts reference
        // 5 min = 300000
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                System.out.println("There are " + snapshot.getChildrenCount() + " messages");
                bottleList.clear();
                Query queryRef = ref.orderByChild("timestamp").startAt(System.currentTimeMillis() - 900000);
                queryRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        System.out.println("There are " + snapshot.getChildrenCount() + " messages");
                        bottleList.clear();
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Message message = postSnapshot.getValue(Message.class);
                            Bottle b = createBottle(message);
                            bottleList.add(b);
                        }
                        addViews();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("The read failed: " + firebaseError.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    public void checkRange(int meters){
        // swipeRefreshLayout.setRefreshing(true);
        chatLayout.removeAllViewsInLayout();
        for(Bottle b:bottleList){
            System.out.println("Bottle: " + b.toString());
            if(b.getDist()<=meters){
                System.out.println("Adding TextView");
                proximityList.add(b);
                addTextView(b);
            }
        }
        scroll.fullScroll(View.FOCUS_DOWN);
        // swipeRefreshLayout.setRefreshing(false);
    }
    public void addViews(){
        chatLayout.removeAllViewsInLayout();
        for(Bottle b:bottleList){
            System.out.println("Bottle: " + b.toString());
                addTextView(b);

        }
    }
    public void addTextView(Bottle b){
        LinearLayout view = buildTextView(b);
       // view.setText(b.getUid() + ": " + b.getMessage());
        System.out.println("UIDS are equal: " + b.getUid().equals(uid));
        //view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        //view.setPadding(12, 12, 12, 12);
        //view.setTextColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setElevation(10f);
        }
        chatLayout.addView(view);
    }
    private LinearLayout buildTextView(Bottle b) {
        // the following change is what fixed it
        LinearLayout messageLayout = new LinearLayout(this);
        messageLayout.setOrientation(LinearLayout.VERTICAL);
        TextView userID = new TextView(this);
        TextView message = new TextView(this);
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        message.setPadding(12, 0, 12, 12);
        message.setTextColor(Color.WHITE);
        message.setText(b.getMessage());
        userID.setText(b.getUid() + ":");
        userID.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        userID.setPadding(12, 12, 12,0);
        userID.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,0);
      //  textView.setBackgroundResource(R.color.colorAccent);
        if(b.getUid().equals(uid)) {
            params.gravity = Gravity.RIGHT;
            params.setMargins(100,10,10,10);
            messageLayout.setBackgroundResource(R.drawable.message_background_user);
        }
        else{
            params.gravity = Gravity.LEFT;
            params.setMargins(10,10,100,10);
            messageLayout.setBackgroundResource(R.drawable.message_background_alternate);
        }
        // textView.setPadding(10, 10, 10, 10);
        //textView.setLayoutParams(params);
        messageLayout.setLayoutParams(params);
        messageLayout.addView(userID);
        messageLayout.addView(message);

        return messageLayout;
    }

    public void printQueue(){
        for(Bottle b:bottleList){
            System.out.println(b.toString());
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }
    @Override
    public void onRefresh() {
        // getSwipeMessages();
        if (bottleList.size() > 0) {
            checkRange(radius);
        }
    }
    public void buildAlertDialog(){
        final EditText input = new EditText(this);
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
                        SharedPreferences settings = PreferenceManager
                                .getDefaultSharedPreferences(context);
                        SharedPreferences.Editor editor = settings.edit();
                        if (!input.getText().toString().equals("")) {
                            editor.putString("uid", input.getText().toString());
                            editor.commit();
                            uid = input.getText().toString();
                            getMessages();
                        }
                        else{
                            uid = "anonymous";
                        }
                    }
                }).show();

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
                        //checkRange(radius);
                    }
                }).show();

    }
    public void registerOnline(){
        final Firebase ref = new Firebase("https://suitepolls.firebaseio.com/online");
        ref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData currentData) {
                if (currentData.getValue() == null) {
                    currentData.setValue(1);
                    onlineView.setText("Online: " + 1);
                } else {
                    currentData.setValue((Long) currentData.getValue() + 1);
                    Long value = (Long)currentData.getValue();
                    onlineView.setText("Online: " + value);
                }
                return Transaction.success(currentData);
            }

            public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
                if (firebaseError != null) {
                    registerOnline();
                    System.out.println("Firebase counter increment failed: ");
                } else {
                    System.out.println("Firebase counter increment succeeded.");
                }
            }
        });
    }
    public void unRegisterOnline(){
        final Firebase ref = new Firebase("https://suitepolls.firebaseio.com/online");
        ref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData currentData) {
                if((Long)currentData.getValue()<0){
                    currentData.setValue(0);
                }
                else {
                    currentData.setValue((Long)currentData.getValue() - 1);
                }
                return Transaction.success(currentData);
            }
            public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
                if (firebaseError != null) {
                    unRegisterOnline();
                    System.out.println("Firebase counter decrement failed.");
                } else {
                    System.out.println("Firebase counter decrement succeeded.");
                }
            }
        });
    }
    public void removePastElements(){
        final Firebase ref = new Firebase("https://suitepolls.firebaseio.com/messages");
        Query queryRef = ref.orderByChild("timestamp").endAt(System.currentTimeMillis() - 300000);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                System.out.println("removing values");
                System.out.println("Children: " + snapshot.getChildrenCount());
                //snapshot.getRef().removeValue();
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });

    }
}
