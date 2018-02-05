package com.nickrobison.trestle.server.models;

/**
 * Created by nickrobison on 2/5/18.
 */
public class UIError {

    private String message;
    private String location;
    private String stack;

    public UIError() {
//        Not used
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }
}
