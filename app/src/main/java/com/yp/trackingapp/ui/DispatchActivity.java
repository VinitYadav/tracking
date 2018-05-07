package com.yp.trackingapp.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.yp.trackingapp.util.Helper;
import com.yp.trackingapp.util.PrefManager;

public class DispatchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PrefManager.isAuthorized()) {
            if (PrefManager.isUserWalking()) {
                startActivity(Helper.getIntent(this, WalkActivity.class));
            } else {
                startActivity(Helper.getIntent(this, MainActivity.class));
            }
        } else {
            startActivity(Helper.getIntent(this, LoginActivity.class));
        }
    }
}
