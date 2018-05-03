package com.yp.trackingapp.ui.meter;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.yp.trackingapp.MyTaskListener;
import com.yp.trackingapp.R;
import com.yp.trackingapp.notification.NotificationBroadcaster;
import com.yp.trackingapp.ui.DispatchActivity;
import com.yp.trackingapp.ui.LogInActivity;
import com.yp.trackingapp.ui.UpdateProfileActivity;
import com.yp.trackingapp.util.Constants;
import com.yp.trackingapp.util.Helper;
import com.yp.trackingapp.util.PrefManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PedoActivity extends AppCompatActivity implements SensorEventListener, StepListener, MyTaskListener {

    private TextView TvSteps;
    private Button BtnStart, BtnStop;
    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String TEXT_NUM_STEPS = "Steps: ";
    private int numSteps;
    private TextView textViewUsername;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedo);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        TvSteps = (TextView) findViewById(R.id.tv_steps);
        BtnStart = (Button) findViewById(R.id.btn_start);
        BtnStop = (Button) findViewById(R.id.btn_stop);
        textViewUsername = findViewById(R.id.textViewUsername);

        dialog = new ProgressDialog(PedoActivity.this);


        BtnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                numSteps = 0;
                sensorManager.registerListener(PedoActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
            }
        });


        BtnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                sensorManager.unregisterListener(PedoActivity.this);
            }
        });
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        TvSteps.setText(TEXT_NUM_STEPS + numSteps);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cancel_notification:
                cancelNotification();
                return true;
            case R.id.action_update_profile:
                Intent intent = new Intent(PedoActivity.this, UpdateProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_delete_profile:
                showDeleteConfirmationPopUp();
                return true;
            case R.id.action_logout:
                PrefManager.setID(PrefManager.USER_ID, null);
                goToDispatchScreen();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTaskResult(String result) {
        if (TextUtils.isBlank(result)) {
            Toast.makeText(PedoActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(PedoActivity.this, "Delete profile successfully", Toast.LENGTH_SHORT).show();
            startActivity(Helper.getIntent(this, LogInActivity.class));
            finish();
        }
    }

    /**
     * Show delete confirmation pop up
     */
    private void showDeleteConfirmationPopUp() {
        final Dialog dialog = new Dialog(PedoActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_profile);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(lp);
        dialog.show();

        TextView textViewOk = dialog.findViewById(R.id.textViewOk);
        TextView textViewCancel = dialog.findViewById(R.id.textViewCancel);

        textViewCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        textViewOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                String iduser = PrefManager.getID(PrefManager.USER_ID);
                HashMap<String, String> params = new HashMap<String, String>();
                MyAsyncTask myTask = new MyAsyncTask(PedoActivity.this, params, false);
                myTask.execute(Constants.BASE_URL + Constants.METHOD_DELETE_PROFILE +
                        "iduser=" + iduser);
            }
        });
    }

    private void goToDispatchScreen() {
        PrefManager.setID(PrefManager.USER_ID, null);
        startActivity(Helper.getIntent(this, DispatchActivity.class));
    }

    private void cancelNotification() {
        Intent notificationIntent = new Intent(this, NotificationBroadcaster.class);
        notificationIntent.putExtra(NotificationBroadcaster.NOTIFICATION_ID, 1);
        PendingIntent pendingIntent = getBroadcast(notificationIntent);
        AlarmManager alarmManager = getSystemService();
        alarmManager.cancel(pendingIntent);
    }

    private AlarmManager getSystemService() {
        return (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    private PendingIntent getBroadcast(Intent notificationIntent) {
        return PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private class MyAsyncTask extends AsyncTask<String, Void, String> {
        MyTaskListener mListener;
        HashMap<String, String> mParamMap;
        HttpClient mHttpClient;
        boolean mBMultipart = false;

        MyAsyncTask(MyTaskListener listener, HashMap<String, String> hashMap,
                    boolean isMultipart) {
            this.mListener = listener;
            this.mParamMap = hashMap;
            this.mBMultipart = isMultipart;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage("Please wait.....");
            dialog.show();
            mHttpClient = new DefaultHttpClient();
        }

        @Override
        protected String doInBackground(String... strings) {
            if (mBMultipart) {
                return multipartMode(strings[0]);
            } else {
                return normalMode(strings[0]);
            }
        }

        private String normalMode(String url) {
            try {
                HttpConnectionParams.setConnectionTimeout(mHttpClient.getParams(), 30000);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                for (String key : mParamMap.keySet()) {
                    nameValuePairs.add(new BasicNameValuePair(key, mParamMap.get(key)));
                }
                HttpPost httppost = new HttpPost(url);
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse httpResponse = mHttpClient.execute(httppost);
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                return responseBody;
            } catch (Exception e) {
                Log.e("MAIN", e.getMessage());
                return null;
            }
        }

        private String multipartMode(String url) {
            return "998787";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            this.mListener.onTaskResult(s);
        }
    }
}
