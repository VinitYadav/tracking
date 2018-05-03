package com.yp.trackingapp.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.yp.trackingapp.R;
import com.yp.trackingapp.model.User;
import com.yp.trackingapp.notification.NotificationBroadcaster;
import com.yp.trackingapp.util.Helper;
import com.yp.trackingapp.util.PrefManager;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.messageLabel)
    TextView mMessage;
    @BindView(R.id.dailyDistanceData)
    TextView mTotalDist;
    @BindView(R.id.dailyTimeData)
    TextView mTotalTime;
    @BindView(R.id.dailyPaceData)
    TextView mCurrentPace;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Realm realm = Realm.getDefaultInstance();
        String id = PrefManager.getID(PrefManager.USER_ID);
        User user = realm.where(User.class).equalTo("id", id).findFirst();
        if (user != null) {
            setDailyStat(user);
            showAchieveMilestone(user.getDistanceCovered());
        }
        scheduleNotification();
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
            case R.id.action_logout:
                PrefManager.setID(PrefManager.USER_ID, null);
                goToDispatchScreen();
                return true;
            case R.id.action_cancel_notification:
                cancelNotification();
                return true;
            case R.id.action_update_profile:
                return true;
            case R.id.action_delete_profile:
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
}
