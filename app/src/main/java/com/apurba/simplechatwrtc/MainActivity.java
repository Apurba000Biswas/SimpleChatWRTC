package com.apurba.simplechatwrtc;


import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.apurba.simplechatwrtc.adapters.HistoryAdapter;
import com.apurba.simplechatwrtc.adt.HistoryItem;
import com.apurba.simplechatwrtc.util.Constants;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends ListActivity {

    private SharedPreferences mSharedPreferences;
    private String username;
    private String stdByChannel;
    private Pubnub mPubNub;

    private ListView mHistoryList;
    private HistoryAdapter mHistoryAdapter;
    private EditText mCallNumET;
    private TextView mUsernameTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        if (!this.mSharedPreferences.contains(Constants.USER_NAME)){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        this.username     = this.mSharedPreferences.getString(Constants.USER_NAME, "");
        this.stdByChannel = this.username + Constants.STDBY_SUFFIX;

        this.mHistoryList = getListView();
        this.mCallNumET   = (EditText) findViewById(R.id.call_num);
        this.mUsernameTV  = (TextView) findViewById(R.id.main_username);

        this.mUsernameTV.setText(this.username);
        initPubNub();

        this.mHistoryAdapter = new HistoryAdapter(this, new ArrayList<HistoryItem>(), this.mPubNub);
        this.mHistoryList.setAdapter(this.mHistoryAdapter);
    }

    public void initPubNub(){
        this.mPubNub  = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
        this.mPubNub.setUUID(this.username);
        subscribeStdBy();
    }
    private void subscribeStdBy(){
        try {
            this.mPubNub.subscribe(this.stdByChannel, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("MA-iPN", "MESSAGE: " + message.toString());
                    if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
                    JSONObject jsonMsg = (JSONObject) message;
                    try {
                        if (!jsonMsg.has(Constants.JSON_CALL_USER)) return;     //Ignore Signaling messages.
                        String user = jsonMsg.getString(Constants.JSON_CALL_USER);
                        dispatchIncomingCall(user);
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void connectCallback(String channel, Object message) {
                    Log.d("MA-iPN", "CONNECTED: " + message.toString());
                    setUserStatus(Constants.STATUS_AVAILABLE);
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.d("MA-iPN","ERROR: " + error.toString());
                }
            });
        } catch (PubnubException e){
            Log.d("HERE","HEREEEE");
            e.printStackTrace();
        }
    }


    public void makeCall(View view) {
        String callNum = mCallNumET.getText().toString();
        if (callNum.isEmpty() || callNum.equals(this.username)){
            showToast("Enter a valid user ID to call.");
            return;
        }
        dispatchCall(callNum);
    }


    public void dispatchCall(final String callNum){
        final String callNumStdBy = callNum + Constants.STDBY_SUFFIX;
        this.mPubNub.hereNow(callNumStdBy, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Log.d("MA-dC", "HERE_NOW: " +" CH - " + callNumStdBy + " " + message.toString());
                try {
                    int occupancy = ((JSONObject) message).getInt(Constants.JSON_OCCUPANCY);
                    if (occupancy == 0) {
                        showToast("User is not online!");
                        return;
                    }
                    JSONObject jsonCall = new JSONObject();
                    jsonCall.put(Constants.JSON_CALL_USER, username);
                    jsonCall.put(Constants.JSON_CALL_TIME, System.currentTimeMillis());
                    mPubNub.publish(callNumStdBy, jsonCall, new Callback() {
                        @Override
                        public void successCallback(String channel, Object message) {
                            Log.d("MA-dC", "SUCCESS: " + message.toString());
                            Intent intent = new Intent(MainActivity.this, VideoChatActivity.class);
                            intent.putExtra(Constants.USER_NAME, username);
                            intent.putExtra(Constants.CALL_USER, callNum);  // Only accept from this number?
                            startActivity(intent);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    private void dispatchIncomingCall(String userId){
        showToast("Call from: " + userId);
        Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
        intent.putExtra(Constants.USER_NAME, username);
        intent.putExtra(Constants.CALL_USER, userId);
        startActivity(intent);
    }


    private void setUserStatus(String status){
        try {
            JSONObject state = new JSONObject();
            state.put(Constants.JSON_STATUS, status);
            this.mPubNub.setState(this.stdByChannel, this.username, state, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("MA-sUS","State Set: " + message.toString());
                }
            });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }


    private void showToast(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }



    public void signOut(View view) {
        this.mPubNub.unsubscribeAll();
        SharedPreferences.Editor edit = this.mSharedPreferences.edit();
        edit.remove(Constants.USER_NAME);
        edit.apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("oldUsername", this.username);
        startActivity(intent);
    }
}
