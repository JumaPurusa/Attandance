package com.example.attandance.Utils;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.attandance.R;

public class DisplayDialog {

    public static AlertDialog progressDialogWaiting(Activity activity, String message, boolean isCancelable){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.layout_progress_dialog, null);

        dialogBuilder.setCancelable(isCancelable);

//        ((TextView)view.findViewById(R.id.messageText)).setText(message);

        dialogBuilder.setView(view);

        return dialogBuilder.create();
    }

    public static AlertDialog progressDialog(Activity activity, String message, boolean isCancelable){

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.layout_progress_dialog, null);
        ((TextView)view.findViewById(R.id.textDialogMessage)).setText(message);
        builder.setView(view).setCancelable(isCancelable);

        return builder.create();
    }

    public static AlertDialog buildConfirmDialog(Activity activity, String message, boolean isCancelable,
                                                 String positiveText, String negativeText){

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.layout_confirm_dialog, null);
        dialogBuilder.setView(dialogView).setCancelable(isCancelable);

        ((TextView)dialogView.findViewById(R.id.textDialogMessage)).setText(message);
        ((TextView)dialogView.findViewById(R.id.positiveButton)).setText(positiveText);
        ((TextView)dialogView.findViewById(R.id.negativeButton)).setText(negativeText);

        return dialogBuilder.create();
    }
}
