package com.triviaapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    final String pubTopic = "sestrivia/client";
    Button btnConnect;
    TextView tvTeamName, tvConnStat;
    Spinner spinner;
    ImageView imgBuzzer;
    AlertDialog.Builder alertBuilder;

    public MqttAndroidClient mqttAndroidClient;

    //    final String serverUri = "tcp://m12.cloudmqtt.com:15080";
    final String brokerUri = "ws://broker.mqttdashboard.com:8000";
//    final String brokerUri = "ws://192.168.0.21:5000";

    String clientId;
    final String subscriptionTopic = "sestrivia/admin";

//    final String username = "mwgdyyop";
//    final String password = "1s4lzSl27zSV";
//    final String username = "sestrivia_mqtt";
//    final String password = "hivemq";

    boolean isMqttConnected = false;
    boolean isButtonClicked = false;
    boolean isSessionStarted = false;
    boolean isMessageSent = false;
    String startMessage;

    UserPrefs userPrefs;
    ArrayAdapter<String> spinnerAdapter;
    List<String> teams;
    ProgressDialog pd;
    MediaPlayer buzzPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Calendar calendar = Calendar.getInstance();
        clientId = "TriviaAppClient"+Integer.toString(new Random().nextInt(50))+Long.toString(calendar.getTimeInMillis());
        mqttAndroidClient = new MqttAndroidClient(this, brokerUri, clientId);

        tvTeamName = findViewById(R.id.tv_teamName);
        tvConnStat = findViewById(R.id.tv_connStat);
        spinner  = findViewById(R.id.spinner);
        imgBuzzer = findViewById(R.id.img_buzzer);
        btnConnect = findViewById(R.id.btn_connect);

        userPrefs = new UserPrefs(this);
        isSessionStarted = userPrefs.getIsSessionStarted();
        isMessageSent = userPrefs.getIsMessageSent();
        isButtonClicked = userPrefs.getIsButtonClicked();

        pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Please wait ...");
        alertBuilder = new AlertDialog.Builder(this);

        buzzPlayer = MediaPlayer.create(this, R.raw.buzzersound);

//        spinnerAdapter = ArrayAdapter.createFromResource(this,
//                        R.array.group_array, android.R.layout.simple_spinner_item);
//        spinnerAdapter = new ArrayAdapter<CharSequence>(this, );
//        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(spinnerAdapter);

        String userTeam = userPrefs.getUserTeam();
        if(userTeam != null){
            tvTeamName.setText(userTeam);
            spinner.setVisibility(View.GONE);
        }

        teams = new ArrayList<>();
        teams.add("Select Team");
        String cachedTeams = userPrefs.getTeams();
        if(cachedTeams != null){
            teams.addAll(Arrays.asList(cachedTeams.split(",")));
        }
        spinnerAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, teams);
        spinner.setAdapter(spinnerAdapter);

        //    Handle spinner
        spinner.setOnItemSelectedListener(this);

        imgBuzzer.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isMessageSent){
//            imgBuzzer.setBackgroundResource(R.drawable.button_background_inactive);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.img_buzzer:
                buzzClicked();
                break;
            case R.id.btn_connect:
                if(!internetIsConnected()){
                    Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
                pd.show();
//                if(btnConnect.getText().toString().equalsIgnoreCase("Reconnect")){
//                    disconnectMQTT();
//                }
                connectMqtt();
//                startMqtt();
                break;
        }
    }

    private void connectMqtt(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(false);
        mqttConnectOptions.setCleanSession(false);
//        mqttConnectOptions.setUserName(username);
//        mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    startMqtt();
                    subscribeToTopic();
                    isMqttConnected = true;
                    btnConnect.setVisibility(View.GONE);
                    pd.dismiss();
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    tvConnStat.setText("Status: Connected");
                    Log.e("Mqtt", "connected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    pd.dismiss();
                    Toast.makeText(MainActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();
                    Log.e("Mqtt", "Failed to connect to: " + brokerUri + exception.toString());
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private void startMqtt(){
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {
                tvConnStat.setText("Status: Disconnected");
//                btnConnect.setText("Reconnect");
//                btnConnect.setVisibility(View.VISIBLE);
                isMqttConnected = false;
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                Log.e("Mqtt", "diconnected");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) {
                Log.e("MQTT", mqttMessage.toString());
                String message = mqttMessage.toString();
                if(message.equals("reset")){
                    isButtonClicked = false;
                    isMessageSent = false;
                    Toast.makeText(MainActivity.this, "Session Reset", Toast.LENGTH_LONG).show();
//                    imgBuzzer.setBackgroundResource(R.drawable.button_background);
                }
                if(message.contains("start")){
                    isSessionStarted = true;
                    isButtonClicked = false;
                    isMessageSent = false;
//                    imgBuzzer.setBackgroundResource(R.drawable.button_background);
                    if(spinner.getVisibility() == View.GONE){
                        spinner.setVisibility(View.VISIBLE);
                    }
                    List<String> list = Arrays.asList(message.split(":")[1].split(","));
                    teams.clear();
                    teams.add("Select Team");
                    teams.addAll(list);
                    spinnerAdapter.notifyDataSetChanged();
                    startMessage = message;
                    Toast.makeText(MainActivity.this, "Session Started", Toast.LENGTH_LONG).show();

                }
                if(message.equals("stop")){
                    isSessionStarted = false;
                    Toast.makeText(MainActivity.this, "Session Stopped", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private void buzzClicked(){
        if(!internetIsConnected()){
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!isSessionStarted){
            Toast.makeText(this, "Just a moment\nWe haven't started", Toast.LENGTH_SHORT).show();
            return;
        }
        if(isButtonClicked && isMessageSent){
            Toast.makeText(this, "You have already sent a response\nWait for the reset", Toast.LENGTH_SHORT).show();
            return;
        }
        if(isButtonClicked && !isMessageSent){
            Toast.makeText(this, "Still sending response\nPlease wait", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!isButtonClicked && isSessionStarted){
            String team = tvTeamName.getText().toString();
            if(!team.isEmpty() && !team.equals("Select Group")){
                isButtonClicked = true;
                buzzPlayer.start();
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.UK);
                String currentTime = sdf.format(calendar.getTime());
                sendMessage(team.trim() + "," + currentTime);
            }else{
                Toast.makeText(this, "No Team Selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMessage(String payload){
        if(isMqttConnected){
            try{
                MqttMessage message = new MqttMessage(payload.getBytes());
                mqttAndroidClient.publish(pubTopic, message, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        isMessageSent = true;
//                        btnBuzz.setBackgroundResource(R.drawable.button_background_inactive);
                        Log.e("MQTT", "message sent successfully");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                        Toast.makeText(MainActivity.this,"Could not send message", Toast.LENGTH_SHORT).show();
                        if(e.getMessage() != null){
                            Log.e("Message error", e.getMessage());
                        }else{
                            e.printStackTrace();
                        }
                    }
                });
            }catch(MqttException e){
                Toast.makeText(this,"Something went wrong", Toast.LENGTH_SHORT).show();
                if(e.getMessage() != null){
                    Log.e("Exception subscribing", e.getMessage());
                }else{
                    e.printStackTrace();
                }
            }
        }else{
            Toast.makeText(this, "Could not connect to server.\nTry again shortly", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String teamName = parent.getItemAtPosition(position).toString();
        if(!teamName.equals("Select Team")){
            showDialog(teamName);
        }
    }

    private void showDialog(final String teamName){
        String message = "You have selected Team "+teamName+"\nProceed?";
        alertBuilder.setTitle("Confirm Team").setMessage(message).setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tvTeamName.setText(teamName);
                        spinner.setVisibility(View.GONE);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Toast.makeText(this, "Select a group", Toast.LENGTH_LONG)
                .show();
    }

    private boolean internetIsConnected(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }


    private void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e("Mqtt","Subscribed to topic!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("Mqtt", "Subscribed fail!");
                }
            });

        } catch (MqttException ex) {
            if(ex.getMessage() != null){
                Log.e("Exception subscribing", ex.getMessage());
            }else{
                ex.printStackTrace();
            }
        }
    }



    private void disconnectMQTT(){
        try{
            mqttAndroidClient.disconnectForcibly(2, 2);
            Log.e("force disconn", "success");
        }catch(MqttException ex){
            if(ex.getMessage() != null){
                Log.e("Error disconnecting", ex.getMessage());
            }else{
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        userPrefs.setIsSessionStarted(isSessionStarted);
        userPrefs.setIsButtonClicked(isButtonClicked);
        userPrefs.setIsMessageSent(isMessageSent);
        userPrefs.setUserTeam(tvTeamName.getText().toString());
        if(startMessage != null){
            userPrefs.setTeams(startMessage.split(":")[1]);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO: 4/29/19 handle service memory leak
//        try{
//            mqttAndroidClient.disconnect();
//        }catch(MqttException ex){
//            if(ex.getMessage() != null){
//                Log.e("Exception subscribing", ex.getMessage());
//            }else{
//                ex.printStackTrace();
//            }
//        }
    }
}
