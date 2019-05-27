package com.testpro;

public class Status
{
    private String message;
    private int statusCode;
    private String description;

    public Status(String msg, int code) {
        message = msg;
        statusCode = code;
    }

    public Status(String msg, int code, String desc) {
        message = msg;
        statusCode = code;
        description = desc;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}