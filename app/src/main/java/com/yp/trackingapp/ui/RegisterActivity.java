package com.yp.trackingapp.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.yp.trackingapp.MyTaskListener;
import com.yp.trackingapp.R;
import com.yp.trackingapp.util.Constants;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RegisterActivity extends BaseAuthentication implements MyTaskListener {

    @BindView(R.id.fullNameSignUp)
    EditText mFullName;
    @BindView(R.id.usernameSignUp)
    EditText mUsername;
    @BindView(R.id.addressSignUp)
    EditText mAddress;
    @BindView(R.id.phonenumberSignUp)
    EditText mPhoneNumber;
    @BindView(R.id.weightSignUp)
    EditText mWeight;
    @BindView(R.id.passwordSignUp)
    EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.signUpBtn)
    public void signUpClickEvent(Button button) {
        String fullname = mFullName.getText().toString().trim();
        String username = mUsername.getText().toString().trim();
        String address = mAddress.getText().toString();
        String phonenumber = mPhoneNumber.getText().toString();
        String weight = mWeight.getText().toString();
        String password = mPassword.getText().toString().trim();

        Toast.makeText(this, fullname + ", " + username + ", " + address + ", " + phonenumber + ", " + weight + ", "
                + password, Toast.LENGTH_SHORT).show();
        HashMap<String, String> params = new HashMap<String, String>();
        RegisterActivity.MyAsyncTask myTask = new RegisterActivity.MyAsyncTask(this, params, false);
        myTask.execute(Constants.BASE_URL + Constants.METHOD_SIGN_UP +
                "username=" + username + "&password=" + password + "&fullname=" + fullname + "&address=" + address +
                "&phonenumber=" + phonenumber + "&weight=" + weight, "", "");
    }

    @OnClick(R.id.cancelBtn)
    public void cancelClickEvent(Button button) {
        finish();
    }

    @Override
    public void onTaskResult(String result) {
        if (Integer.valueOf(result) > 0) {
            com.yp.trackingapp.util.PrefManager.setID(com.yp.trackingapp.util.
                    PrefManager.USER_ID, result);
            startActivity(com.yp.trackingapp.util.Helper.getIntent(this,
                    DispatchActivity.class));
        }
    }

    class MyAsyncTask extends android.os.AsyncTask<String, Void, String> {
        MyTaskListener mListener;
        HashMap<String, String> mParamMap;
        HttpClient mHttpClient;
        boolean mBMultipart = false;

        public MyAsyncTask(MyTaskListener listener, HashMap<String, String> hashMap,
                           boolean isMultipart) {
            this.mListener = listener;
            this.mParamMap = hashMap;
            this.mBMultipart = isMultipart;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
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

                HttpConnectionParams.setConnectionTimeout(mHttpClient.getParams(),
                        30000);
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
            this.mListener.onTaskResult(s);
        }
    }
}
