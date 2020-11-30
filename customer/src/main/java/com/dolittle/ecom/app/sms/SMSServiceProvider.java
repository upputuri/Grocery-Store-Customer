package com.dolittle.ecom.app.sms;

public interface SMSServiceProvider {
    public int sendOTP(String numbers, String message);
    public int sendNotification(String numbers, String message);
}
