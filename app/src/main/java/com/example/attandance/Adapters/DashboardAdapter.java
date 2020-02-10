package com.example.attandance.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.attandance.Activities.Activities.AttendanceActivity;
import com.example.attandance.Activities.Activities.AttendanceReportActivity;
import com.example.attandance.Activities.Activities.ProfileActivity;
import com.example.attandance.Activities.Activities.RegisterActivity;
import com.example.attandance.Models.DashboardItem;
import com.example.attandance.R;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardViewHolder>{

    private Context mContext;
    private DashboardItem[] dashboardItems;

    public DashboardAdapter(Context context, DashboardItem[] list){
        this.mContext = context;
        this.dashboardItems = list;
    }

    @NonNull
    @Override
    public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dashboard_item_view, parent, false);
        return new DashboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DashboardViewHolder holder, final int position) {

        final DashboardItem dashboardItem = dashboardItems[position];

        Glide.with(mContext)
                .load(dashboardItem.getImage())
                .into(holder.imageView);

        holder.textView.setText(dashboardItem.getText());

        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if(position == 0){

                            mContext.startActivity(
                                    new Intent(mContext, AttendanceActivity.class)
                            );
                        }else if(position == 1){

                            mContext.startActivity(
                                    new Intent(mContext, AttendanceReportActivity.class)
                            );
                        }else if(position == 2){

                            mContext.startActivity(
                                    new Intent(mContext, RegisterActivity.class)
                            );
                        }else if(position == 3){
                            mContext.startActivity(
                                    new Intent(mContext, ProfileActivity.class)
                            );
                        }
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return dashboardItems.length;
    }

    public class DashboardViewHolder extends RecyclerView.ViewHolder{

        private ImageView imageView;
        private TextView textView;

        public DashboardViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            textView = itemView.findViewById(R.id.textView);
        }
    }
}

