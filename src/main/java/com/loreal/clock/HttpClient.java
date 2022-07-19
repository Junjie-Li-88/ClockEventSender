package com.loreal.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HttpClient<main> {
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    public static String makePostRequest(String url, String content, HashMap<String, String> extraHeaders, Map<String,String> data_condition) throws IOException {
        Integer responseCode = -1;
        SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //重试10次，如果2xx就退出，非2xx就重试
        for(int i=1;i<=10;i++) {
            String Error_Log="";
            String state_data="";
            //开始时间
            Date start_time = null;
            try {
                start_time = sdf.parse(EventToHTTP.getGMT8Time());
            } catch (ParseException e) {
                logger.error(e.toString());
            }
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            if (extraHeaders != null) {
                for (HashMap.Entry<String, String> entry : extraHeaders.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            try {
                OutputStream os = connection.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                osw.write(content);
                osw.flush();
                osw.close();
                os.close();  //don't forget to close the OutputStream

                logger.info("Send 'HTTP POST' request to : " + url);


                responseCode = connection.getResponseCode();
                logger.info("Response Code : " + responseCode);

                if (responseCode >= 200 && responseCode < 300) {//  == HttpURLConnection.HTTP_OK) {
                    BufferedReader inputReader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = inputReader.readLine()) != null) {
                        response.append(inputLine);
                    }
                    inputReader.close();
//                System.out.println(response.toString());
                    if (data_condition.get("target_url").equals("pekon")) {
                        EventToHTTP.numPekonSuccess+=Integer.parseInt(data_condition.get("data_num"));
                    }
                    if (data_condition.get("target_url").equals("gem")) {
                        EventToHTTP.numGemSuccess+=Integer.parseInt(data_condition.get("data_num"));
                    }
                    state_data="succeeded";
                    logger.info("url :"+url +" "+response.toString());
                } else {
                    BufferedReader inputReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = inputReader.readLine()) != null) {
                        response.append(inputLine);
                    }
                    inputReader.close();
                    if (data_condition.get("target_url").equals("pekon")) {
                        EventToHTTP.numPekonError+=Integer.parseInt(data_condition.get("data_num"));
                    }
                    if (data_condition.get("target_url").equals("gem")) {
                        EventToHTTP.numGemError+=Integer.parseInt(data_condition.get("data_num"));
                    }
                    Error_Log = url+" "+response.toString();
                    logger.error("Something went wrong sending to " + url + ": HTTP Code is " + responseCode + "HTTP log is "+response.toString() + " content data: " + content);
                }
            } catch (Exception e) {
                logger.error("url is "+ url +" Something went wrong in HTTPClient: "+e.toString() + " content data: "+content);
                Error_Log = url+" "+e.toString();
                if (data_condition.get("target_url").equals("pekon")) {
                    EventToHTTP.numPekonError+=Integer.parseInt(data_condition.get("data_num"));
                }
                if (data_condition.get("target_url").equals("gem")) {
                    EventToHTTP.numGemError+=Integer.parseInt(data_condition.get("data_num"));
                }
            }
            // 记录统计日志
            //insert api log
            Map<String,String> map_data = new HashMap<>();
            map_data.put("start_time",sdf.format(start_time));
            map_data.put("uuid",UUID.randomUUID().toString());
            map_data.put("event_type", data_condition.get("event_type"));
            map_data.put("api_name", data_condition.get("target_url"));

            if (Error_Log.equals("")) {
                map_data.put("suceeded_num", data_condition.get("data_num"));
                map_data.put("failed_num", String.valueOf(0));
            } else {
                map_data.put("suceeded_num", String.valueOf(0));
                map_data.put("failed_num", data_condition.get("data_num"));
            }

            Date end_time = null;
            String result_data="";
            String json_data="";
            try {
                end_time = sdf.parse(String.valueOf(EventToHTTP.getGMT8Time()));
                map_data.put("end_time",sdf.format(end_time));
                map_data.put("avg_duration", String.valueOf((end_time.getTime() - start_time.getTime())/1000));
                json_data = EventToHTTP.Log_Data(map_data);

                HttpClientUtil http = new HttpClientUtil();
                result_data = http.sendJsonToHttpsPost(EventToHTTP.logApiUrl, json_data, "utf-8","http");
            } catch (ParseException e) {
                logger.error(e.toString());
                Error_Log = e.toString();
            } catch (NoSuchAlgorithmException e) {
                logger.error(e.toString());
                Error_Log = e.toString();
            } catch (KeyManagementException e) {
                logger.error(e.toString());
                Error_Log = e.toString();
            }
            logger.info("log api: "+result_data + " json data:"+json_data);

            //写入错误日志
            if (! Error_Log.equals("")) {
                EventToHTTP et = new EventToHTTP();
                Map<String, String> map_data_error = new HashMap<>();
                map_data_error.put("current_time", et.getGMT8Time());
                map_data_error.put("uuid", UUID.randomUUID().toString());
                int severity_id;
                if (i >= 10) {
                    severity_id = 10;
                } else {
                    severity_id = 5;
                }
                map_data_error.put("severity_id", String.valueOf(severity_id));
                map_data_error.put("message", "responseCode: "+responseCode +" message data:" + Error_Log);
                map_data_error.put("source", "call api");
                map_data_error.put("target", data_condition.get("target_url")+" api "+url);
                map_data_error.put("retry_num", String.valueOf(i));
                if (state_data.equals("succeeded")){
                    map_data_error.put("state", "succeeded");
                }else{
                    map_data_error.put("state", "ignore");
                }

                String http_log = et.Error_Log_Data(map_data_error);
                logger.info("error log api " + http_log);
            }

            if (state_data.equals("succeeded")){
                break;
            }
        }
        return responseCode.toString();
    }

//    public static void main(String[] args) {
//        HashMap<String, String> extraHeaders = new HashMap<String, String>();
//        try {
//            HttpClient.makePostRequest("http://52.131.220.24:3001/error","",extraHeaders,"pekon");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}