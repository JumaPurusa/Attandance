package com.example.attandance.Models;

import com.example.attandance.R;

public class DashboardItem {

    private int image;
    private String text;

    public static DashboardItem[] dashboardItems = {
            new DashboardItem(R.drawable.ic_assignment_ind, "Attendance"),
            new DashboardItem(R.drawable.ic_assignment,"Attendance Report"),
            new DashboardItem(R.drawable.ic_person_add, "Register Worker"),
            new DashboardItem(R.drawable.ic_person_outline, "Profile")
    };

    public DashboardItem(int image, String text) {
        this.image = image;
        this.text = text;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
