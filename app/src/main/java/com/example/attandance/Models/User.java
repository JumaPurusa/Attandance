package com.example.attandance.Models;

public class User {

    private String username;
    private String templateData;
    private int templateLength;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTemplateData() {
        return templateData;
    }

    public void setTemplateData(String templateData) {
        this.templateData = templateData;
    }

    public int getTemplateLength() {
        return templateLength;
    }

    public void setTemplateLength(int templateLength) {
        this.templateLength = templateLength;
    }

}
