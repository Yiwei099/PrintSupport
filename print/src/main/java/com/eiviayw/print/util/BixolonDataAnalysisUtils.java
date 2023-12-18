package com.eiviayw.print.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-18 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 必胜龙SDK相关工具
 */
public class BixolonDataAnalysisUtils {

    public static String[] handleFindNetData(String data){
        try {
            int j = 0;
            for (int i = data.length() - 1; i >= 0; i--) {
                char ch = data.charAt(i);
                String pre = data.substring(0, i);
                String post = data.substring(i);
                String ins = "printer" + j + ":";
                if (ch == '{') {
                    data = pre + ins + post;
                    j++;
                }
            }
            data = "{" + data + "}";
            JSONObject jsonObject = new JSONObject(data);
            Iterator<String> tempGroupKey = jsonObject.keys();


            int i = 0;
            String macAddress = "", address = "", port = "", systemName = "";
            final String[] items = new String[jsonObject.length()];

            while (tempGroupKey.hasNext()) {
                String grpKey = tempGroupKey.next();

                JSONObject obj = new JSONObject(jsonObject.get(grpKey).toString());
                Iterator<String> tempChildKey = obj.keys();
                while (tempChildKey.hasNext()) {
                    String key = tempChildKey.next();
                    switch (key) {
                        case "macAddress":
                            macAddress = obj.getString(key);
                            break;
                        case "address":
                            address = obj.getString(key);
                            break;
                        case "portNumber":
                            port = obj.getString(key);
                            break;
                        case "systemName":
                            systemName = obj.getString(key);
                            items[i++] = macAddress + "," + address + "," + port;
                            break;
                    }
                }
            }
            return items;
        } catch (JSONException e) {
        }

        return null;
    }
}
