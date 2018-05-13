package com.yp.trackingapp.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends BaseAuthentication implements MyTaskListener {

    @BindView(R.id.usernameSignIn)
    EditText mUsername;
    @BindView(R.id.passwordSignIn)
    EditText mPassword;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        ButterKnife.bind(this);
        dialog = new ProgressDialog(LoginActivity.this);
    }

    @OnClick(R.id.loginBtn)
    public void signInClickEvent(Button button) {
        String username = mUsername.getText().toString().trim();
        String password = mPassword.getText().toString().trim();

        if (isEmptyField(username, password)) {
            com.yp.trackingapp.util.Helper.displayMessageToUser(this,
                    getString(R.string.login_error_title),
                    getString(R.string.login_error_message)).show();
        } else {
            HashMap<String, String> params = new HashMap<String, String>();
            MyAsyncTask myTask = new MyAsyncTask(this, params, false);
            myTask.execute(Constants.BASE_URL + Constants.METHOD_LOGIN
                    + "username=" + username + "&password=" + password, "", "");

        }
    }

    @OnClick(R.id.goToSignUpBtn)
    public void goToSignUpClickEvent(Button button) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    @Override
    public void onTaskResult(String result) {
        if (Integer.valueOf(result) == 0) {
            com.yp.trackingapp.util.Helper.displayMessageToUser(this,
                    getString(R.string.login_error_title),
                    getString(R.string.not_found_error_message)).show();
        } else {
            PrefManager.setID(PrefManager.USER_ID, result);
            startActivity(com.yp.trackingapp.util.Helper.getIntent(this,
                    DispatchActivity.class));
        }
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
                HttpConnectionParams.setConnectionTimeout(mHttpClient.getParams(),
                        30000);
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
