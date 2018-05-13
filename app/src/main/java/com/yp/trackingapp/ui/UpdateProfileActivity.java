package com.yp.trackingapp.ui;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.yp.trackingapp.MyTaskListener;
import com.yp.trackingapp.R;
import com.yp.trackingapp.util.Constants;
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
import java.util.HashMap;
import java.util.List;

public class UpdateProfileActivity extends AppCompatActivity implements MyTaskListener {

    private EditText mFullName; // User full name
    private EditText mUsername;// User name
    private EditText mAddress;// User address
    private EditText mPhoneNumber;// User phone number
    private EditText mWeight;// User weight
    private ProgressDialog dialog;// Progress dialog use for loader
    private Button buttonUpdateProfile;// Update button
    private boolean flag;// This flag use for check which service response.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        // Initialize all fields
        mFullName = findViewById(R.id.fullNameSignUp);
        mUsername = findViewById(R.id.usernameSignUp);
        mAddress = findViewById(R.id.addressSignUp);
        mPhoneNumber = findViewById(R.id.phonenumberSignUp);
        mWeight = findViewById(R.id.weightSignUp);
        buttonUpdateProfile = findViewById(R.id.buttonUpdateProfile);

        // Initialize dialog
        dialog = new ProgressDialog(UpdateProfileActivity.this);

        // Set listener on update button
        buttonUpdateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickUpdateProfile(); // Click on update button
            }
        });

        // Get user details from server
        getUserDetail();
    }

    /**
     * Click on update button
     */
    private void onClickUpdateProfile() {
        // Get data from edit fields
        String fullName = mFullName.getText().toString().trim();
        String userName = mUsername.getText().toString().trim();
        String address = mAddress.getText().toString();
        String phoneNumber = mPhoneNumber.getText().toString();
        String weight = mWeight.getText().toString();
        String userId = PrefManager.getID(PrefManager.USER_ID);

        // Check all fields
        if (validation(fullName, userName, address, phoneNumber, weight)) {
            HashMap<String, String> params = new HashMap<String, String>();
            // Cal service from server for update user profile
            UpdateProfileTask myTask = new UpdateProfileTask(this, params, false);
            // Create request
            String request = Constants.BASE_URL + Constants.METHOD_UPDATE_PROFILE +
                    "iduser=" + userId + "&username=" + userName +
                    "&fullname=" + fullName + "&address=" + address +
                    "&phonenumber=" + phoneNumber + "&weight=" + weight;
            myTask.execute(request);
        }
    }

    @Override
    public void onTaskResult(String result) {
        if (flag) { // If user click on update button
            if (TextUtils.isBlank(result)) {
                Toast.makeText(UpdateProfileActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UpdateProfileActivity.this, "Update profile successfully", Toast.LENGTH_SHORT).show();
            }
        } else { // When screen load first time
            if (TextUtils.isEmpty(result) || result.equalsIgnoreCase("User not found")) {
                Toast.makeText(UpdateProfileActivity.this, result, Toast.LENGTH_SHORT).show();
            } else {
                try {
                    // Parse server response
                    JSONObject jsonObject = new JSONObject(result);
                    // Get full name
                    mFullName.setText(jsonObject.getString("fullname"));
                    // Get user name
                    mUsername.setText(jsonObject.getString("username"));
                    // Get address
                    mAddress.setText(jsonObject.getString("address"));
                    // Get phone number
                    mPhoneNumber.setText(jsonObject.getString("phonenumber"));
                    // Get weight
                    mWeight.setText(jsonObject.getString("weight"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Validation for fields
     *
     * @param fullName    User full name
     * @param username    Username
     * @param address     User address
     * @param phonenumber User phone number
     * @param weight      User weight
     * @return True or False
     */
    private boolean validation(String fullName, String username, String address, String phonenumber, String weight) {
        if (fullName.isEmpty()) { // Check full name isEmpty ot not
            Toast.makeText(UpdateProfileActivity.this, "Please enter full name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (username.isEmpty()) { // Check user name isEmpty ot not
            Toast.makeText(UpdateProfileActivity.this, "Please enter user name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (address.isEmpty()) {// Check address isEmpty ot not
            Toast.makeText(UpdateProfileActivity.this, "Please enter address", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phonenumber.isEmpty()) {// Check phone number isEmpty ot not
            Toast.makeText(UpdateProfileActivity.this, "Please enter phone number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (weight.isEmpty()) {// Check weight isEmpty ot not
            Toast.makeText(UpdateProfileActivity.this, "Please enter weight", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Get user detail
     */
    private void getUserDetail() {
        String userId = PrefManager.getID(PrefManager.USER_ID);
        HashMap<String, String> params = new HashMap<String, String>();
        // Call service from server for get user profile details
        GetUserDetailAsyncTask task = new GetUserDetailAsyncTask(UpdateProfileActivity.this, params, false);
        // Create request
        String request = Constants.BASE_URL + Constants.METHOD_GET_PROFILE + "iduser=" + userId;
        task.execute(request);
    }

    /**
     * Get user detail
     */
    private class GetUserDetailAsyncTask extends AsyncTask<String, Void, String> {
        MyTaskListener mListener;
        HashMap<String, String> mParamMap;
        HttpClient mHttpClient;
        boolean mBMultipart = false;

        GetUserDetailAsyncTask(MyTaskListener listener, HashMap<String, String> hashMap,
                               boolean isMultipart) {
            this.mListener = listener;
            this.mParamMap = hashMap;
            this.mBMultipart = isMultipart;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage("Please wait.....");
            dialog.show(); // Show dialog
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
                dialog.dismiss(); // Cancel dialog
            }
            flag = false;
            this.mListener.onTaskResult(s);
        }
    }

    /**
     * Update user details
     */
    private class UpdateProfileTask extends android.os.AsyncTask<String, Void, String> {
        MyTaskListener mListener;
        HashMap<String, String> mParamMap;
        HttpClient mHttpClient;
        boolean mBMultipart = false;

        UpdateProfileTask(MyTaskListener listener, HashMap<String, String> hashMap, boolean isMultipart) {
            this.mListener = listener;
            this.mParamMap = hashMap;
            this.mBMultipart = isMultipart;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage("Please wait.....");
            dialog.show(); // Show dialog
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
                java.util.List<NameValuePair> nameValuePairs = new java.util.ArrayList<NameValuePair>();
                for (String key : mParamMap.keySet()) {
                    nameValuePairs.add(new BasicNameValuePair(key, mParamMap.get(key)));
                }
                HttpPost httppost = new HttpPost(url);
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse httpResponse = mHttpClient.execute(httppost);
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                return responseBody;
            } catch (Exception e) {
                android.util.Log.e("MAIN", e.getMessage());
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
                dialog.dismiss(); // Cancel dialog
            }
            flag = true;
            this.mListener.onTaskResult(s);
        }
    }
}
