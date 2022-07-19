package com.loreal.clock;

import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ErrorMessageJson {
    public static String newMessage(String message, int status, String path) {
        JSONObject returnMessage = new JSONObject();
        returnMessage.put("message", message);
        returnMessage.put("status", status);
        returnMessage.put("path", path);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String format = dateFormat.format(new Date());
        returnMessage.put("timestamp", format);
        return returnMessage.toJSONString();
    }
}