package com.apurba.simplechatwrtc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.apurba.simplechatwrtc.util.Constants;
import com.pubnub.api.Pubnub;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private String username;
    private String stdByChannel;
    private Pubnub mPubNub;

    private ListView mHistoryList;
    //private HistoryAdapter mHistoryAdapter;
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


    }


    public void makeCall(View view) {
    }
}
