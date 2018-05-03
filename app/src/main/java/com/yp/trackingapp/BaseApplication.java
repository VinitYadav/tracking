package com.yp.trackingapp;

import android.app.Application;

import com.yp.trackingapp.util.PrefManager;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Marcel on 9/12/2016.
 */
public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);

       // RealmConfiguration realmConfig = new RealmConfiguration.Builder(this).schemaVersion(0)
       ///         .deleteRealmIfMigrationNeeded().build();
        //Realm.setDefaultConfiguration(realmConfig);
        PrefManager.initSharedPref(this);
    }
}
