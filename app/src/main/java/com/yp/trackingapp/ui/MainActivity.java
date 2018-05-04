package com.yp.trackingapp.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.yp.trackingapp.MyTaskListener;
import com.yp.trackingapp.R;
import com.yp.trackingapp.model.User;
import com.yp.trackingapp.notification.NotificationBroadcaster;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;

public class MainActivity extends AppCompatActivity implements MyTaskListener {

    @BindView(R.id.messageLabel)
    TextView mMessage;
    @BindView(R.id.dailyDistanceData)
    TextView mTotalDist;
    @BindView(R.id.dailyTimeData)
    TextView mTotalTime;
    @BindView(R.id.dailyPaceData)
    TextView mCurrentPace;
    private ProgressDialog dialog;
    private boolean flag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        dialog = new ProgressDialog(MainActivity.this);

        Realm realm = Realm.getDefaultInstance();
        String id = PrefManager.getID(PrefManager.USER_ID);
        User user = realm.where(User.class).equalTo("id", id).findFirst();
        if (user != null) {
            setDailyStat(user);
            showAchieveMilestone(user.getDistanceCovered());
        }
        scheduleNotification();

        String userId = PrefManager.getID(PrefManager.USER_ID);
        HashMap<String, String> params = new HashMap<String, String>();
        GetWalkDetails myTask = new GetWalkDetails(MainActivity.this, params, false);
        myTask.execute(Constants.BASE_URL + Constants.METHOD_GET_WALK_DETAIL +
                "user_id=" + userId);
    }

    private void setDailyStat(User user) {
        String message = String.format(getString(R.string.message_label), user.getFirstName());
        String dailyDist = String.format(getString(R.string.daily_dist_data), Helper.meterToMileConverter(user
                .getDistanceCovered()));
        String dailyTime = String.format(getString(R.string.daily_time_data), Helper.secondToMinuteConverter(user
                .getTotalTimeWalk()));
        String dailyPace = String.format(getString(R.string.daily_pace_data), user.getPace());

        mMessage.setText(message);
        mTotalDist.setText(dailyDist);
        mTotalTime.setText(dailyTime);
        mCurrentPace.setText(dailyPace);
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
                Intent intent = new Intent(MainActivity.this, UpdateProfileActivity.class);
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

    @OnClick(R.id.walkBtn)
    public void goToWalkEvent(Button button) {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        startActivity(Helper.getIntent(MainActivity.this, WalkActivity.class));
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                                   PermissionToken token) {

                    }
                }).check();
    }

    private void showAchieveMilestone(float distanceCovered) {
        int numberOfMilestones = Helper.getNumberOfMilestones(distanceCovered);
        if (numberOfMilestones > 0) {
            String title = getString(R.string.achievement_title);
            String message = String.format(getString(R.string.achievement_message), numberOfMilestones);
            Helper.displayMessageToUser(this, title, message).show();
        }
    }

    private void goToDispatchScreen() {
        startActivity(Helper.getIntent(this, DispatchActivity.class));
    }

    // Assume user controls the periodic reminder: no reminder at office if user turn off notification
    private void scheduleNotification() {

        Notification notification = createNotification(getString(R.string.app_name), getString(R.string
                .notification_message));
        Intent notificationIntent = getIntent(notification);
        PendingIntent pendingIntent = getBroadcast(notificationIntent);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 20);

        AlarmManager alarmManager = getSystemService();

        // Reminder every 1 hour
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_HOUR,
                pendingIntent);
    }

    private AlarmManager getSystemService() {
        return (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    private PendingIntent getBroadcast(Intent notificationIntent) {
        return PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @NonNull
    private Intent getIntent(Notification notification) {
        Intent notificationIntent = new Intent(this, NotificationBroadcaster.class);
        notificationIntent.putExtra(NotificationBroadcaster.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationBroadcaster.NOTIFICATION_KEY, notification);
        return notificationIntent;
    }

    private Notification createNotification(String title, String message) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        Intent resultIntent = new Intent(this, DispatchActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        builder.setAutoCancel(true);
        return builder.build();
    }

    private void cancelNotification() {
        Intent notificationIntent = new Intent(this, NotificationBroadcaster.class);
        notificationIntent.putExtra(NotificationBroadcaster.NOTIFICATION_ID, 1);
        PendingIntent pendingIntent = getBroadcast(notificationIntent);
        AlarmManager alarmManager = getSystemService();
        alarmManager.cancel(pendingIntent);
    }

    /**
     * Show delete confirmation pop up
     */
    private void showDeleteConfirmationPopUp() {
        final Dialog dialog = new Dialog(MainActivity.this);
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
                DeletProfile myTask = new DeletProfile(MainActivity.this, params, false);
                myTask.execute(Constants.BASE_URL + Constants.METHOD_DELETE_PROFILE +
                        "iduser=" + iduser);
            }
        });
    }

    @Override
    public void onTaskResult(String result) {
        if (flag) {
            if (TextUtils.isBlank(result)) {
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            } else {
                PrefManager.setID(PrefManager.USER_ID, null);
                Toast.makeText(MainActivity.this, "Delete profile successfully", Toast.LENGTH_SHORT).show();
                startActivity(Helper.getIntent(this, LogInActivity.class));
                finish();
            }
        } else {
            if (!TextUtils.isEmpty(result)){
                if (!result.equalsIgnoreCase("User not found")){
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        mTotalDist.setText(jsonObject.getString("distance")+" mi");
                        mTotalTime.setText(jsonObject.getString("time")+" min");
                        mCurrentPace.setText(jsonObject.getString("steps"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class DeletProfile extends AsyncTask<String, Void, String> {
        MyTaskListener mListener;
        HashMap<String, String> mParamMap;
        HttpClient mHttpClient;
        boolean mBMultipart = false;

        DeletProfile(MyTaskListener listener, HashMap<String, String> hashMap,
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
            flag = true;
            this.mListener.onTaskResult(s);
        }
    }

    private class GetWalkDetails extends AsyncTask<String, Void, String> {
        MyTaskListener mListener;
        HashMap<String, String> mParamMap;
        HttpClient mHttpClient;
        boolean mBMultipart = false;

        GetWalkDetails(MyTaskListener listener, HashMap<String, String> hashMap,
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
            flag = false;
            this.mListener.onTaskResult(s);
        }
    }
}
