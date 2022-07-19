package com.loreal.clock.Task;

import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.loreal.clock.EventHubAsync;
import com.loreal.clock.EventToHTTP;
import com.loreal.clock.HttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;



public class CallableSendHTTP implements Callable<Object>  {
    private String url;
    private String data;
    private HashMap<String, String> extraHeaders;
    private Map<String,String> data_condition;
    public CallableSendHTTP(String url, String data, HashMap<String, String> extraHeaders, Map<String,String> data_condition) {
        this.url = url;
        this.data = data;
        this.extraHeaders = extraHeaders;
        this.data_condition = data_condition;
    }

    @Override
    public Object call() throws Exception {
        try {
            String response = HttpClient.makePostRequest(url, data, extraHeaders, data_condition);
            //System.out.println(response);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
