package com.eiviayw.printsupport;

import android.app.Application;


/**
 * Created by Harden on 2018/7/17.
 */

public class MyApplication extends Application {
    private static MyApplication mInstance = null;



    public static MyApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

    }
}
