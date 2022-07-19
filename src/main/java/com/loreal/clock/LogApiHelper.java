package com.loreal.clock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;



public class LogApiHelper {

    public static void main(String[] args) {
        JSONObject s = new JSONObject();
        s.put("firetruck","you firetrucked up");
        HashMap<String, Integer> hs = new HashMap<>();
        hs.put("received_tid", 2);
        HashMap<String, Integer> hspbi = new HashMap<>();
        hspbi.put("sum_received_tid", 2);
        JSONArray errObj = makeStatJson(hs, hspbi);
        System.out.println(errObj);
    }

    public static JSONArray makeStatJson(HashMap<String, Integer> statMessagesSql, HashMap<String, Integer> statMessagesPbi) {
        JSONObject ret = new JSONObject();
        ret.put("method", "push");
        ret.put("type", "statistics");
        ret.put("level", "information");
        JSONArray sinkArray = new JSONArray();
        JSONObject sinkObject = new JSONObject();
        sinkObject.put("id", "sink_pbi_01");
        sinkObject.put("type", "power_bi");
        sinkObject.put("name", "appservice_url");
        sinkArray.add(sinkObject);
        JSONObject sqlSinkObject = new JSONObject();
        sqlSinkObject.put("id", "sqldbsink");
        sqlSinkObject.put("type", "sql_db");
        //TODO: make this in application.properties.
        //sit
        //sqlSinkObject.put("name", "clockorder.log_messages");


        //prd
        sqlSinkObject.put("name", "ce2-cndat-sd-pd-clksd0.log_messages");
        sinkArray.add(sqlSinkObject);

        ret.put("sinks", sinkArray);
        ret.put("key_ref","ce2-cndat-as-np-clkas");
        ret.put("service_ref","ce2-cndat-as-np-clkas");
        ret.put("service_type","appservice");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        Date newDate = calendar.getTime();
        String newDateString = df.format(newDate);
        ret.put("timestamp", newDateString);

        JSONArray records = new JSONArray();

        //no sqldb, no sum
        JSONObject currec = new JSONObject();
        JSONObject record = new JSONObject();
        record.put("timestamp", newDateString);
        JSONArray recSinks = new JSONArray();
        recSinks.add("sqldbsink");
        JSONObject currecrecord = new JSONObject();
        for (String s : statMessagesSql.keySet()) {
            currecrecord.put(s, statMessagesSql.get(s));
        }
        currecrecord.put("timestamp", newDateString);
        currec.put("record", currecrecord);
        currec.put("sinks", recSinks);
        currec.put("record_id", UUID.randomUUID().toString());

        //pbi, yes sum
        JSONObject currecPbi = new JSONObject();
        JSONObject recordPbi = new JSONObject();
        recordPbi.put("timestamp", newDateString);
        JSONArray recSinksPbi = new JSONArray();
        recSinksPbi.add("sink_pbi_01");
        JSONObject currecrecordPbi = new JSONObject();
        for (String s : statMessagesPbi.keySet()) {
            currecrecordPbi.put(s, statMessagesPbi.get(s));
        }
        currecrecordPbi.put("timestamp", newDateString);
        currecPbi.put("record", currecrecordPbi);
        currecPbi.put("record_id", UUID.randomUUID().toString());
        currecPbi.put("sinks", recSinksPbi);

        records.add(currec);
        records.add(currecPbi);

        ret.put("records", records);
        System.out.println("ret");

        JSONArray wrapper = new JSONArray();
        wrapper.add(ret);

        return wrapper;
    }

    public static JSONArray makeErrorJson(JSONObject errorMessage) {
        JSONObject ret = new JSONObject();
        ret.put("method", "push");
        ret.put("type", "log");
        ret.put("level", "error");
        JSONArray sinkArray = new JSONArray();
        JSONObject sinkObject = new JSONObject();
        sinkObject.put("id", "sink_adx_01");
        sinkObject.put("type", "adx");
        sinkObject.put("name", "appservice_errors");
        sinkArray.add(sinkObject);

        ret.put("sinks", sinkArray);
        ret.put("key_ref","ce2-cndat-as-np-clkas");
        ret.put("service_ref","ce2-cndat-as-np-clkas");
        ret.put("service_type","appservice");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        Date newDate = calendar.getTime();
        String newDateString = df.format(newDate);
        ret.put("timestamp", newDateString);

        JSONArray records = new JSONArray();
        JSONObject currec = new JSONObject();
        JSONObject record = new JSONObject();
        record.put("timestamp", newDateString);

        JSONArray recSinks = new JSONArray();
        recSinks.add("sink_adx_01");
        recSinks.add("sqldbsink");
        currec.put("record", errorMessage);
        currec.put("record_id", UUID.randomUUID().toString());
        currec.put("sinks", recSinks);
        records.add(currec);

        ret.put("records", records);
        System.out.println("ret");

        JSONArray wrapper = new JSONArray();
        wrapper.add(ret);

        return wrapper;
    }
}
