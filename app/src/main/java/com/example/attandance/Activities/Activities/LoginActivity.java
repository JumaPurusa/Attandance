package com.example.attandance.Activities.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.example.attandance.Models.User;
import com.example.attandance.R;
import com.example.attandance.Utils.DBController;
import com.example.attandance.Utils.DisplayDialog;
import com.example.attandance.Utils.JSONParser;
import com.example.attandance.Utils.MySingleton;
import com.example.attandance.Utils.NetworkHelper;
import com.example.attandance.Utils.SharedPrefManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.suprema.BioMiniFactory;
import com.suprema.CaptureResponder;
import com.suprema.IBioMiniDevice;
import com.telpo.tps550.api.fingerprint.FingerPrint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private EditText usernameEditText, passwordEditText;
    private CoordinatorLayout coordinatorLayout;

    RequestQueue requestQueue;

    private AlertDialog progressDialog;
    private SharedPrefManager prefManager;

    private List<User> users = new ArrayList<>();
    private DBController dbController;

    public static final int REQUEST_WRITE_PERMISSION = 786;
    private static BioMiniFactory mBioMiniFactory = null;
    public IBioMiniDevice mCurrentDevice = null;

    private LinearLayout fingerprintLayout;
    private TextView textFingerprintStatus, textDeviceStatus;
    private ImageView fingerprintImage;
    private LinearLayout progressLayout;
    private Button startCaptureButton;

    private IBioMiniDevice.CaptureOption mCaptureOptionDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = SharedPrefManager.getInstance(
                getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        );

        if(prefManager.getUser().getUsername() != null){
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        FingerPrint.fingerPrintPower(1);

        restartBioMini();

        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setTitle("Login");
        actionBar.setHomeButtonEnabled(true);

        dbController = new DBController(LoginActivity.this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                loadUsers();
            }
        }, 100);

        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        fingerprintLayout = findViewById(R.id.fingerprintLayout);
        textFingerprintStatus = findViewById(R.id.textStatus);
        textDeviceStatus = findViewById(R.id.textDevice);
        fingerprintImage = findViewById(R.id.fingerprintImage);
        progressLayout = findViewById(R.id.progressLayout);
        startCaptureButton = findViewById(R.id.startCaptureButton);


        progressDialog = DisplayDialog.progressDialog(
                LoginActivity.this,
                "Please Wait...",
                false
        );

        findViewById(R.id.loginBtn).setOnClickListener(new View.OnClickListener( ) {
            @Override
            public void onClick(View v) {
                // hide the keyboard
                hideKeyboard();

                if(isFormValid())
                    onStartLogin(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());

            }
        });

        findViewById(R.id.startCaptureButton).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onStartLoginUsingFingerprint();
                    }
                }
        );

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        requestQueue = new RequestQueue(cache, network);

        // Start the queue
        requestQueue.start();
    }

    private boolean isFormValid(){

        if (TextUtils.isEmpty(usernameEditText.getText().toString()) && TextUtils.isEmpty(passwordEditText.getText().toString())) {
            showSnackbarMessage("Please provide both your username and password");
            return false;
        } else if (TextUtils.isEmpty(usernameEditText.getText().toString())) {
            showSnackbarMessage("Please enter your username");
            return false;
        } else if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
            showSnackbarMessage("Please enter your password");
            return false;
        }else
            return true;


    }

    private void onStartLogin(String username, String password) {

        if (!progressDialog.isShowing())
            progressDialog.show();


        loginTask(username,
                password);


    }

    private void showSnackbarMessage(String message){

        Snackbar.make(coordinatorLayout,
                message,
                Snackbar.LENGTH_SHORT)
                .show();

    }

    private void loginTask(final String email, final String password){

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                "https://icapaas.000webhostapp.com/login.php",
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onResponse(String response) {

                        if(progressDialog.isShowing())
                            progressDialog.dismiss();

                        if (response.toLowerCase().contains("Username or Password is not valid") || response.toLowerCase().contains("Improper Request Method")) {

                            onClearEditText();
                            Snackbar.make(coordinatorLayout,
                                    response,
                                    5000).show();

                        }else if(response == null || response.contains("<br>") || response.contains("<!DOCTYPE")){

                            onErrorOccurred();

                        }else{

                            User user = JSONParser.parseUser(response);

                            if(user != null){

                                onSuccessLogin(user);
                            }
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        hideKeyboard();

                        if (error instanceof TimeoutError) {
                            showSnackbarMessage("Timeout Error");
                        } else if (error instanceof NoConnectionError) {
                            showSnackbarMessage("No Connection Error");
                        } else if (error instanceof AuthFailureError) {
                            showSnackbarMessage("Authentication Failure Error");
                        } else if (error instanceof NetworkError) {
                            showSnackbarMessage("Network Error");
                        } else if (error instanceof ServerError) {
                            showSnackbarMessage("Server Error");
                        } else if (error instanceof ParseError) {
                            showSnackbarMessage("JSON Parse Error");
                        }

                        if(progressDialog.isShowing())
                            progressDialog.dismiss();

//                        for(User user : users){
//
//                            if(usernameEditText.getText().toString().equals(user.getUsername()) ){
//                                onSuccessLogin(user);
//                                break;
//                            }
//                        }

                    }
                }

        ) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", email);
                params.put("password", password);
                return params;
            }

//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//               / Map<String, String> headers = new HashMap<String, String>();
//                headers.put("User-Agent", "abb");
//                return headers;
//            }
        };

        stringRequest.setTag(TAG);
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finishAffinity();

        requestQueue = MySingleton.getInstance(getApplicationContext())
                .getRequestQueue();

        if(requestQueue != null){
            requestQueue.cancelAll(TAG);

            if(progressDialog.isShowing())
                progressDialog.dismiss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(progressDialog.isShowing())
            progressDialog.dismiss();

        requestQueue = MySingleton.getInstance(getApplicationContext())
                .getRequestQueue();

        if(requestQueue != null)
            requestQueue.cancelAll(TAG);
    }

    private void sendToMainActivity(){
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        //mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void hideKeyboard(){

        View view = getCurrentFocus();
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

    }

    private void onClearEditText(){
        usernameEditText.setText("");
        passwordEditText.setText("");
        usernameEditText.requestFocus();
    }



    private void onErrorOccurred(){

        onClearEditText();

        Snackbar.make(coordinatorLayout,
                "Error Occured. Please try again",
                5000).show();
    }

    private void onSuccessLogin(User user){

        prefManager.saveUser(user);

        if (mBioMiniFactory != null) {
            mBioMiniFactory.close();
            mBioMiniFactory = null;
        }

        FingerPrint.fingerPrintPower(0);
        startActivity(new Intent(LoginActivity.this, MainActivity.class));

        finish();
    }


    private void loadUsers(){

        if(users != null)
            users.clear();

        Cursor cursor = dbController.getUsers();

        if(cursor.moveToFirst()){

            do{
                User user = new User();

                user.setUsername(cursor.getString(cursor.getColumnIndex("username")));
                user.setTemplateData(cursor.getString(cursor.getColumnIndex("templateData")));
                user.setTemplateLength(cursor.getInt(cursor.getColumnIndex("templateLength")));

                users.add(user);
            }while (cursor.moveToNext());

        }

        Toast.makeText(this, String.valueOf(users.size()), Toast.LENGTH_SHORT).show();
    }

    private void onStartLoginUsingFingerprint(){


        showProgressBar();
        textFingerprintStatus.setText("");
        fingerprintImage.setImageBitmap(null);

        if(mCurrentDevice != null){
            mCaptureOptionDefault = new IBioMiniDevice.CaptureOption();
            mCaptureOptionDefault.frameRate = IBioMiniDevice.FrameRate.SHIGH;

            mCaptureOptionDefault.captureImage = true;
            mCaptureOptionDefault.extractParam.captureTemplate = true;
            mCurrentDevice.captureSingle(
                    mCaptureOptionDefault,
                    mCaptureResponseDefault,
                    true);

        }
    }


    private void restartBioMini() {
        if(mBioMiniFactory != null) {
            mBioMiniFactory.close();
        }

        mBioMiniFactory = new BioMiniFactory(getApplicationContext()) {
            @Override
            public void onDeviceChange(DeviceChangeEvent event, Object dev) {

                if(event == DeviceChangeEvent.DEVICE_ATTACHED && mCurrentDevice == null){

                    mCurrentDevice = mBioMiniFactory.getDevice(0);
//
                    mCurrentDevice.
                            setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.
                                    SECURITY_LEVEL, 4));
                    mCurrentDevice.
                            setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.
                                    SENSITIVITY, 7));
                    mCurrentDevice.
                            setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.TIMEOUT, 10000));
                    mCurrentDevice.
                            setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.DETECT_FAKE, 0));
                    mCurrentDevice.
                            setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.FAST_MODE, 1));

                    mCurrentDevice.
                            setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.SCANNING_MODE, 1));

                    mCurrentDevice.
                            setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.EXT_TRIGGER, 1));

                    mCurrentDevice.
                            setParameter(new IBioMiniDevice.Parameter(IBioMiniDevice.ParameterType.ENABLE_AUTOSLEEP, 0));

                    printDeviceStatus("Fingerprint Scanner is Available");

                } else if (mCurrentDevice != null && event == DeviceChangeEvent. DEVICE_DETACHED &&
                        mCurrentDevice.isEqual(dev)){
                    printDeviceStatus("Fingerprint Scanner is not available");
                    mCurrentDevice = null;
                }

            }
        };

    }

    private void showProgressBar(){

        if(progressLayout.getVisibility() == View.GONE) {
            progressLayout.setVisibility(View.VISIBLE);

            startCaptureButton.setVisibility(View.GONE);

        }
    }

    private void hideProgressBar(){

        if(progressLayout.getVisibility() == View.VISIBLE){
            progressLayout.setVisibility(View.GONE);

//            startCaptureButton.setVisibility(View.VISIBLE);
        }

    }

    synchronized public void printState(final CharSequence str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textFingerprintStatus.setText(str);
            }
        });

    }

    private void printDeviceStatus(final String status){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textDeviceStatus.setText(status);
            }
        });


    }

    private CaptureResponder mCaptureResponseDefault = new CaptureResponder() {
        @Override
        public boolean onCaptureEx(Object o, final Bitmap capturedImage, final IBioMiniDevice.TemplateData capturedTemplate, IBioMiniDevice.FingerState fingerState) {
            printState("Captured Fingerprint");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (capturedImage != null) {

                        hideProgressBar();
                        fingerprintImage.setVisibility(View.VISIBLE);
                        if (fingerprintImage != null)
                            fingerprintImage.setImageBitmap(capturedImage);
                    }

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(capturedTemplate != null){

                                User user = new User();
                                boolean isUserExist = false;
                                for(User newUser : users){

                                    byte[] templateData =
                                            Base64.decode(newUser.getTemplateData(), Base64.DEFAULT);
                                    if(mCurrentDevice.verify(capturedTemplate.data, capturedTemplate.data.length,
                                            templateData, newUser.getTemplateLength())){

                                        isUserExist = true;
                                        user = newUser;
                                        break;
                                    }
                                }

                                if(isUserExist){
                                    Toast.makeText(LoginActivity.this,
                                            user.getUsername(), Toast.LENGTH_SHORT).show();

                                    if(NetworkHelper.isConnected(LoginActivity.this)){
                                        loginUsingFingerprint(user.getUsername());
                                    }else
                                        onSuccessLogin(user);

                                }else{
                                    Toast.makeText(LoginActivity.this, "User Does No Exist", Toast.LENGTH_SHORT).show();
                                    startCaptureButton.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }, 800);

                }
            });

            return true;
        }

        @Override
        public void onCaptureError(Object o, int errorCode, String error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgressBar();
                    startCaptureButton.setVisibility(View.VISIBLE);
                    fingerprintImage.setImageResource(R.drawable.ic_error_outline);
                }
            });

            if (errorCode != IBioMiniDevice.ErrorCode.OK.value())
                printState("Capturing Failed");
        }

    };


    @Override
    protected void onDestroy() {
        if (mBioMiniFactory != null) {
            mBioMiniFactory.close();
            mBioMiniFactory = null;
        }

        FingerPrint.fingerPrintPower(0);
        super.onDestroy();
    }

    public void clearState(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textFingerprintStatus.setText("");
                hideProgressBar();
                fingerprintImage.setImageBitmap(null);
            }
        });

    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},  REQUEST_WRITE_PERMISSION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }
    @Override
    public void onPostCreate(Bundle savedInstanceState){
        requestPermission();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.ic_clear){
            onClearEditText();
            if (mCurrentDevice != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentDevice.abortCapturing();
                        int nRetryCount = 0;
                        while (mCurrentDevice != null && mCurrentDevice.isCapturing()) {
                            SystemClock.sleep(10);
                            nRetryCount++;
                        }
                        Log.d("AbortCapturing", String.format(Locale.ENGLISH,
                                "IsCapturing return false.(Abort-lead time: %dms) ",
                                nRetryCount * 10));
                    }
                }).start();
            }
            clearState();
            startCaptureButton.setVisibility(View.VISIBLE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loginUsingFingerprint(String username) {

        if (!progressDialog.isShowing())
            progressDialog.show();

        loginTask(username);
    }

    private void loginTask(final String username){

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                "https://icapaas.000webhostapp.com/login_fingerprint.php",
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onResponse(String response) {

                        if(progressDialog.isShowing())
                            progressDialog.dismiss();

                        if (response.toLowerCase().contains("user does Not exit") || response.toLowerCase().contains("improper request method")
                            || response.toLowerCase().contains("there is no such fingerprint data")) {

                            Snackbar.make(coordinatorLayout,
                                    response,
                                    5000).show();

                            startCaptureButton.setVisibility(View.VISIBLE);

                        }else if(response == null || response.contains("<br>") || response.contains("<!DOCTYPE")){

                            onErrorOccurred();

                            startCaptureButton.setVisibility(View.VISIBLE);

                        }else{

                            User user = JSONParser.parseUser(response);

                            if(user != null){

                                onSuccessLogin(user);
                            }
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        hideKeyboard();

                        if (error instanceof TimeoutError) {
                            showSnackbarMessage("Timeout Error");
                        } else if (error instanceof NoConnectionError) {
                            showSnackbarMessage("No Connection Error");
                        } else if (error instanceof AuthFailureError) {
                            showSnackbarMessage("Authentication Failure Error");
                        } else if (error instanceof NetworkError) {
                            showSnackbarMessage("Network Error");
                        } else if (error instanceof ServerError) {
                            showSnackbarMessage("Server Error");
                            Toast.makeText(LoginActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            Log.d(TAG, "onErrorResponse: " + error.getMessage());
                        } else if (error instanceof ParseError) {
                            showSnackbarMessage("JSON Parse Error");
                        }

                        if(progressDialog.isShowing())
                            progressDialog.dismiss();

                        startCaptureButton.setVisibility(View.VISIBLE);

                    }
                }

        ) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", username);
                return params;
            }

//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//               / Map<String, String> headers = new HashMap<String, String>();
//                headers.put("User-Agent", "abb");
//                return headers;
//            }
        };

        stringRequest.setTag(TAG);
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }
}
