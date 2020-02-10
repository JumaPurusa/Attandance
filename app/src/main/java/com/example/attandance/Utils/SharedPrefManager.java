package com.example.attandance.Utils;

import android.content.SharedPreferences;

import com.example.attandance.Models.User;

public class SharedPrefManager {

    private static final String TAG = SharedPrefManager.class.getSimpleName();

    private SharedPreferences prefs;

    private SharedPreferences.Editor editor;

    private static SharedPrefManager mInstance;

    private SharedPrefManager(SharedPreferences prefs){

        this.prefs = prefs;
        this.editor = prefs.edit();

    }
    public static synchronized SharedPrefManager getInstance(SharedPreferences prefs){

        if(mInstance == null){
            mInstance = new SharedPrefManager(prefs);
        }

        return mInstance;
    }

    public void saveUser(User user){

        editor.putString("username", user.getUsername());

        if(user.getTemplateData() != null){
            editor.putString("templateData", user.getTemplateData());
        }

        editor.putInt("templateLength", user.getTemplateLength());

        editor.apply();
    }
    
    public User getUser(){

        User user = new User();
        user.setUsername(prefs.getString("username", null));

        if(prefs.getString("templateData", null) != null){
            user.setTemplateData(prefs.getString("templateData", null));
        }
        user.setTemplateLength(prefs.getInt("templateLength", 0));

        return user;
    }
    
    public void clear(){

        editor.clear();

        editor.apply();
    }

}