package com.eiviayw.printsupport;

import android.app.Application;
import android.content.Context;

class MyApplication extends Application{
    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }
}