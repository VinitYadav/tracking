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

    private EditText mFullName;
    private EditText mUsername;
    private EditText mAddress;
    private EditText mPhoneNumber;
    private EditText mWeight;
    private EditText mPassword;
    private ProgressDialog dialog;
    private Button buttonUpdateProfile;
    private boolean flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        mFullName = findViewById(R.id.fullNameSignUp);
        mUsername = findViewById(R.id.usernameSignUp);
        mAddress = findViewById(R.id.addressSignUp);
        mPhoneNumber = findViewById(R.id.phonenumberSignUp);
        mWeight = findViewById(R.id.weightSignUp);
        mPassword = findViewById(R.id.passwordSignUp);
        buttonUpdateProfile = findViewById(R.id.buttonUpdateProfile);

        dialog = new ProgressDialog(UpdateProfileActivity.this);

        buttonUpdateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickUpdateProfile();
            }
        });

        getUserDetail();
    }

    private void onClickUpdateProfile() {
        String fullname = mFullName.getText().toString().trim();
        String username = mUsername.getText().toString().trim();
        String address = mAddress.getText().toString();
        String phonenumber = mPhoneNumber.getText().toString();
        String weight = mWeight.getText().toString();
        String password = mPassword.getText().toString().trim();
        String iduser = PrefManager.getID(PrefManager.USER_ID);

        if (validation(fullname, username, address, phonenumber, weight, password)) {
            HashMap<String, String> params = new HashMap<String, String>();
            MyAsyncTask myTask = new MyAsyncTask(this, params, false);
            String request = Constants.BASE_URL + Constants.METHOD_UPDATE_PROFILE +
                    "iduser=" + iduser + "&username=" + username +
                    "&fullname=" + fullname + "&address=" + address +
                    "&phonenumber=" + phonenumber + "&weight=" + weight;
            myTask.execute(request);
        }
    }

    @Override
    public void onTaskResult(String result) {
        if (flag){
            if (TextUtils.isBlank(result)) {
                Toast.makeText(UpdateProfileActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UpdateProfileActivity.this, "Update profile successfully", Toast.LENGTH_SHORT).show();
            }
        }else{
            if (TextUtils.isEmpty(result)|| result.equalsIgnoreCase("User not found")) {
                Toast.makeText(UpdateProfileActivity.this, result, Toast.LENGTH_SHORT).show();
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    mFullName.setText(jsonObject.getString("fullname"));
                    mUsername.setText(jsonObject.getString("username"));
                    mAddress.setText(jsonObject.getString("address"));
                    mPhoneNumber.setText(jsonObject.getString("phonenumber"));
                    mWeight.setText(jsonObject.getString("weight"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean validation(String fullName, String username, String address, String phonenumber, String weight,
                               String password) {
        if (fullName.isEmpty()) {
            Toast.makeText(UpdateProfileActivity.this, "Please enter full name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (username.isEmpty()) {
            Toast.makeText(UpdateProfileActivity.this, "Please enter user name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (address.isEmpty()) {
            Toast.makeText(UpdateProfileActivity.this, "Please enter address", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phonenumber.isEmpty()) {
            Toast.makeText(UpdateProfileActivity.this, "Please enter phone number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (weight.isEmpty()) {
            Toast.makeText(UpdateProfileActivity.this, "Please enter weight", Toast.LENGTH_SHORT).show();
            return false;
        }
        /*if (password.isEmpty()) {
            Toast.makeText(UpdateProfileActivity.this, "Please enter password", Toast.LENGTH_SHORT).show();
            return false;
        }*/
        return true;
    }

    /**
     * Get user detail
     */
    private void getUserDetail() {
        String iduser = PrefManager.getID(PrefManager.USER_ID);
        HashMap<String, String> params = new HashMap<String, String>();
        GetUserDetailAsyncTask task = new GetUserDetailAsyncTask(UpdateProfileActivity.this, params, false);
        String request = Constants.BASE_URL + Constants.METHOD_GET_PROFILE +"iduser=" + iduser;
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

    private class MyAsyncTask extends android.os.AsyncTask<String, Void, String> {
        MyTaskListener mListener;
        HashMap<String, String> mParamMap;
        HttpClient mHttpClient;
        boolean mBMultipart = false;

        MyAsyncTask(MyTaskListener listener, HashMap<String, String> hashMap, boolean isMultipart) {
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
                dialog.dismiss();
            }
            flag = true;
            this.mListener.onTaskResult(s);
        }
    }
}
