package com.example.attandance.Utils;


import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.attandance.Models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

public class JSONParser {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static User parseUser(String response){

        User user = new User();

        try {
            JSONObject responseObject = new JSONObject(response);
            JSONObject jsonObject = responseObject.getJSONArray("user")
                    .getJSONObject(0);

            user.setUsername(jsonObject.getString("username"));

            if(!jsonObject.getString("templateData").equals("")){
               user.setTemplateData(jsonObject.getString("templateData"));
            }

            if(jsonObject.getInt("templateLength") != 0)
                user.setTemplateLength(jsonObject.getInt("templateLength"));

            return user;

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }



}

