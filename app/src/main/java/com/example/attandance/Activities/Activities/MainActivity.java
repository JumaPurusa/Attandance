package com.example.attandance.Activities.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.attandance.Adapters.DashboardAdapter;
import com.example.attandance.Models.DashboardItem;
import com.example.attandance.Models.User;
import com.example.attandance.R;
import com.example.attandance.Utils.DBController;
import com.example.attandance.Utils.DisplayDialog;
import com.example.attandance.Utils.JSONParser;
import com.example.attandance.Utils.MySingleton;
import com.example.attandance.Utils.NetworkHelper;
import com.example.attandance.Utils.SharedPrefManager;
import com.google.android.material.snackbar.Snackbar;
import com.suprema.BioMiniFactory;
import com.suprema.CaptureResponder;
import com.suprema.IBioMiniDevice;
import com.telpo.tps550.api.fingerprint.FingerPrint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActionBar actionBar;

    private LinearLayout fingerprintRegLayout;
    private boolean isFingerprintLayoutVisible = false;

    private RelativeLayout dashboardLayout;
    private boolean isDashboardLayoutVisible = false;

    private SharedPrefManager prefManager;

    private AlertDialog confirmLogoutDialog;
    private AlertDialog progressDialog;


    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int REQUEST_WRITE_PERMISSION = 786;
    private static BioMiniFactory mBioMiniFactory = null;
    public IBioMiniDevice mCurrentDevice = null;
    private TextView textFingerprintStatus, textDeviceStatus, textConfirm, confirmTextStatus;
    private ImageView fingerprintImage, confirmedFingerprint;
    private LinearLayout progressLayout, confirmProgressLayout;
    private Button startCaptureButton;
    private AlertDialog confirmFingerprintDialog;

    private IBioMiniDevice.TemplateData templateData1;

    private IBioMiniDevice.CaptureOption mCaptureOptionDefault;

    private DBController dbController;
//    private List<User> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = SharedPrefManager.getInstance(
                getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        );

        setContentView(R.layout.activity_main);

        dbController = new DBController(MainActivity.this);

//        loadUsers();

        initViews();

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        actionBar = getSupportActionBar();
        assert actionBar != null;

        buildConfirmLogoutDialog();

        if(prefManager.getUser().getTemplateLength() != 0){
            Toast.makeText(this, prefManager.getUser().getTemplateData(),
                    Toast.LENGTH_SHORT).show();
            showDashboard();
        }else{
            showFingerprintRegistration();
        }

    }

    private void initViews(){
        fingerprintRegLayout = findViewById(R.id.fingerprintLayout);
        dashboardLayout = findViewById(R.id.dashboardLayout);
    }

    private void showFingerprintRegistration(){

        FingerPrint.fingerPrintPower(1);

        restartBioMini();

        isFingerprintLayoutVisible = true;
        isFingerprintLayoutVisible = false;

        actionBar.setTitle(getString(R.string.reg_complete));

        fingerprintRegLayout.setVisibility(View.VISIBLE);

        textFingerprintStatus = findViewById(R.id.textStatus);
        textDeviceStatus = findViewById(R.id.textDevice);
        fingerprintImage = findViewById(R.id.fingerprintImage);
        progressLayout = findViewById(R.id.progressLayout);
        startCaptureButton = findViewById(R.id.startCaptureButton);

        mCaptureOptionDefault = new IBioMiniDevice.CaptureOption();
        mCaptureOptionDefault.frameRate = IBioMiniDevice.FrameRate.SHIGH;

        findViewById(R.id.startCaptureButton).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        showProgressBar();
                        textFingerprintStatus.setText("");
                        fingerprintImage.setImageBitmap(null);

                        if(mCurrentDevice != null){

                            mCaptureOptionDefault.captureImage = true;
                            mCaptureOptionDefault.extractParam.captureTemplate = true;
                            mCurrentDevice.captureSingle(
                                    mCaptureOptionDefault,
                                    mCaptureResponseDefault,
                                    true);

                        }

                    }
                }
        );

        progressDialog = DisplayDialog.progressDialog(
                MainActivity.this,
                "Fingerprints Matched, Please Wait...",
                false
        );

    }

    private CaptureResponder mCaptureResponseDefault = new CaptureResponder() {
        @Override
        public boolean onCaptureEx(Object o, final Bitmap capturedImage, final IBioMiniDevice.TemplateData capturedTemplate, IBioMiniDevice.FingerState fingerState) {
            Log.d(TAG, "onCaptureEx: Capture Successfully");
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

                                templateData1 = capturedTemplate;
                                showConfirmDialog();
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
                    fingerprintImage.setImageResource(R.drawable.ic_error_outline);
                }
            });

            if (errorCode != IBioMiniDevice.ErrorCode.OK.value())
                printState("Capturing Failed");
        }

    };

    private void showDashboard(){

        isDashboardLayoutVisible = true;
        isFingerprintLayoutVisible = false;

        fingerprintRegLayout.setVisibility(View.GONE);

        dashboardLayout.setVisibility(View.VISIBLE);
        actionBar.setTitle("");
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_home);

        RecyclerView mRecyclerView = findViewById(R.id.dashboard_list);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(
                MainActivity.this, 2);
        mRecyclerView.setLayoutManager(layoutManager);

        DashboardAdapter adapter = new DashboardAdapter(MainActivity.this, DashboardItem.dashboardItems);
        if(mRecyclerView != null)
            mRecyclerView.setAdapter(adapter);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

//            getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.add(1000, 0, 0, "Refresh");
        menu.add(1000, 1, 0, "Logout");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case 0:
//                loadUsers();
                getAllSyncUsers();
                return true;

            case 1:
                confirmLogoutDialog.show();

                confirmLogoutDialog.findViewById(R.id.positiveButton).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                confirmLogoutDialog.dismiss();
                                logout();
                            }
                        }
                );

                confirmLogoutDialog.findViewById(R.id.negativeButton).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                confirmLogoutDialog.dismiss();
                            }
                        }
                );
                return true;

            case R.id.ic_clear:
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
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }

    }

    private void buildConfirmLogoutDialog(){

        confirmLogoutDialog = DisplayDialog.buildConfirmDialog(
                MainActivity.this, "Do you want to Logout",
                true, "Logout", "cancel"
        );
    }

    private void logout(){

        prefManager.clear();

        if (mBioMiniFactory != null) {
            mBioMiniFactory.close();
            mBioMiniFactory = null;
        }

        FingerPrint.fingerPrintPower(0);


        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);

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

    private void showProgressBar(){

        if(progressLayout.getVisibility() == View.GONE) {
            progressLayout.setVisibility(View.VISIBLE);

            startCaptureButton.setVisibility(View.GONE);

        }
    }

    private void hideProgressBar(){

        if(progressLayout.getVisibility() == View.VISIBLE){
            progressLayout.setVisibility(View.GONE);

            startCaptureButton.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onDestroy() {
        if (mBioMiniFactory != null) {
            mBioMiniFactory.close();
            mBioMiniFactory = null;
        }

        FingerPrint.fingerPrintPower(0);
        super.onDestroy();
    }

    private void showConfirmDialog(){

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.layout_confirm_fingerprint, null);
        dialogBuilder.setView(dialogView).setCancelable(false);

        confirmProgressLayout = dialogView.findViewById(R.id.confirmProgressLayout);
        confirmedFingerprint = dialogView.findViewById(R.id.confirmedFingerprint);
        confirmTextStatus = dialogView.findViewById(R.id.confirmTextStatus);
        textConfirm = dialogView.findViewById(R.id.textConfirm);

//        dialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });

//        dialogBuilder.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//                if(confirmEditText.getText().toString().
//                        matches(fingerprintImage.getText().toString())){
//
//                    dialog.dismiss();
//                    onRegisterFingerprint();
//
//                }else{
//                    showToastMessage(getString(R.string.finger_do_not_match));
//                }
//            }
//        });

        textConfirm.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        showProgressBarConfirm();
                        confirmTextStatus.setText("");
                        confirmedFingerprint.setImageBitmap(null);

                        //mCaptureOptionDefault.captureTimeout = (int)mCurrentDevice.getParameter(IBioMiniDevice.ParameterType.TIMEOUT).value;
                        if(mCurrentDevice != null){

                            mCaptureOptionDefault.captureImage = true;
                            mCaptureOptionDefault.extractParam.captureTemplate = true;
                            mCurrentDevice.captureSingle(
                                    mCaptureOptionDefault,
                                    mCaptureResponseConfirm,
                                    true);

                        }
                    }
                }
        );

        confirmFingerprintDialog = dialogBuilder.create();
        confirmFingerprintDialog.show();
    }

    private CaptureResponder mCaptureResponseConfirm = new CaptureResponder() {
        @Override
        public boolean onCaptureEx(Object o, final Bitmap capturedImage, final IBioMiniDevice.TemplateData capturedTemplate, IBioMiniDevice.FingerState fingerState) {
            Log.d(TAG, "onCaptureEx: Capture Successfully");
            printStateConfirm("Captured Fingerprint");

            runOnUiThread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {

                    if (capturedImage != null) {

                        hideProgressBarConfirm();
                        confirmedFingerprint.setVisibility(View.VISIBLE);
                        if (confirmedFingerprint != null)
                            confirmedFingerprint.setImageBitmap(capturedImage);
                    }

                    if(capturedTemplate != null) {
                        boolean isMatched = false;
                        if(mCurrentDevice.verify(capturedTemplate.data, capturedTemplate.data.length,
                                templateData1.data, templateData1.data.length)){
                            isMatched = true;
                        }

                        confirmFingerprintDialog.dismiss();
                        if(isMatched){

                            if(NetworkHelper.isConnected(MainActivity.this))
                                onStartRegisteringFingerprint();
//                                Toast.makeText(MainActivity.this, "register", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(MainActivity.this,
                                        "No Connection", Toast.LENGTH_LONG).show();
                        }else{

                            Toast.makeText(MainActivity.this,
                                    "Fingers do not match", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            return true;
        }

        @Override
        public void onCaptureError(Object o, int errorCode, String error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgressBarConfirm();
                    confirmedFingerprint.setImageResource(R.drawable.ic_error_outline);
                }
            });

            if (errorCode != IBioMiniDevice.ErrorCode.OK.value())
                printStateConfirm("Capturing Failed");
        }

    };

    synchronized public void printStateConfirm(final CharSequence str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                confirmTextStatus.setText(str);
            }
        });

    }

    private void showProgressBarConfirm(){

        if(confirmProgressLayout.getVisibility() == View.GONE) {
            confirmProgressLayout.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBarConfirm(){

        if(confirmProgressLayout.getVisibility() == View.VISIBLE){
            confirmProgressLayout.setVisibility(View.GONE);
        }

    }


    private void onStartRegisteringFingerprint(){

        if (!progressDialog.isShowing())
            progressDialog.show();

        final String templateDataString = Base64.encodeToString(templateData1.data, Base64.DEFAULT);
        registerFingerPrintTask(prefManager.getUser().getUsername(),
                templateDataString, templateData1.data.length);
    }

    private void registerFingerPrintTask(final String username, final String templateDataString, final int templateLength){

        Log.d(TAG, "registerFingerPrintTask: executed" );

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                "https://icapaas.000webhostapp.com/register_fingerprint.php",
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onResponse(String response) {

                        if(progressDialog.isShowing())
                            progressDialog.dismiss();

                        if (response.contains("Username is not Valid") ||
                            response.contains("Couldn't register fingerprint") ||
                            response.contains("Fingerprint data not available") ||
                            response.contains("Improper Request Method")) {

                            Toast.makeText(MainActivity.this,
                                    response, Toast.LENGTH_LONG).show();

                        }else if(response == null || response.contains("<br>") || response.contains("<!DOCTYPE")){

                            Toast.makeText(MainActivity.this, "Error Occured", Toast.LENGTH_SHORT).show();

                        }else{

                            User user = JSONParser.parseUser(response);

                            if(user != null){

                                prefManager.saveUser(user);

                                showDashboard();

                                showToastMessage(response);
                            }

//                            showToastMessage(response);
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (error instanceof TimeoutError) {
                            showToastMessage("Timeout Error");
                        } else if (error instanceof NoConnectionError) {
                            showToastMessage("No Connection Error");
                        } else if (error instanceof AuthFailureError) {
                            showToastMessage("Authentication Failure Error");
                        } else if (error instanceof NetworkError) {
                            showToastMessage("Network Error");
                        } else if (error instanceof ServerError) {
                            showToastMessage("Server Error");
                        } else if (error instanceof ParseError) {
                            showToastMessage("JSON Parse Error");
                        }

                        if(progressDialog.isShowing())
                            progressDialog.dismiss();
                    }
                }

        ) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", username);
                params.put("templateData", templateDataString);
                params.put("templateLength", String.valueOf(templateLength));
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

    private void showToastMessage(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

//    private void onSuccessRegisterFingerprint(User user){
//
//        prefManager.saveUser(user);
//
//        showDashboard();
//    }

    private void getAllSyncUsers(){

        Toast.makeText(this, "Syncing users", Toast.LENGTH_LONG).show();

        StringRequest stringRequest = new StringRequest(
                "https://icapaas.000webhostapp.com/getAllUsers.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        if(response.contains("There are no users")){
                            Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
                            //Toast.makeText(DonorsActivity.this, response, Toast.LENGTH_SHORT).show();
                        }else{

                            try{

                                JSONObject jsonObject = new JSONObject(response);
                                JSONArray jsonArray = jsonObject.getJSONArray("users");

                                for(int i=0; i<jsonArray.length(); i++){
                                    JSONObject userObject = jsonArray.getJSONObject(i);
                                    User user = new User();

                                    user.setUsername(userObject.getString("username"));
                                    user.setTemplateData(userObject.getString("templateData"));
                                    user.setTemplateLength(userObject.getInt("templateLength"));

//                                    if(users.size() > 0){
//
//                                        for(User newUser : users){
//
//                                            if(!newUser.getUsername().equals(user.getUsername())){
//                                                dbController.addUser(user.getUsername(),
//                                                        user.getTemplateData(),
//                                                        user.getTemplateLength());
//                                            }
//                                        }
//                                    }else{
//
//                                        dbController.addUser(user.getUsername(),
//                                                user.getTemplateData(),
//                                                user.getTemplateLength());
//
//                                    }

                                    dbController.addUser(user.getUsername(),
                                            user.getTemplateData(),
                                            user.getTemplateLength());

                                    Toast.makeText(MainActivity.this,
                                            "There are " + dbController.dbSyncCount() + " synced users", Toast.LENGTH_SHORT).show();
                                }

                            }catch (JSONException e){
                                e.printStackTrace();
                            }

//                            loadUsers();

                        }


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Toast.makeText(MainActivity.this,
                                error.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                }
        ) ;

        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }

//    private void loadUsers(){
//
//        if(users != null)
//            users.clear();
//
//        Cursor cursor = dbController.getUsers();
//
//        if(cursor.moveToFirst()){
//
//            do{
//                User user = new User();
//
//                user.setUsername(cursor.getString(cursor.getColumnIndex("username")));
//                user.setTemplateData(cursor.getString(cursor.getColumnIndex("templateData")));
//                user.setTemplateLength(cursor.getInt(cursor.getColumnIndex("templateLength")));
//
//                users.add(user);
//            }while (cursor.moveToNext());
//        }
//    }

}
