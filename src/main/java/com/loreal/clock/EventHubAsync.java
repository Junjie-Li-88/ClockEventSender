package com.loreal.clock;

import com.alibaba.fastjson.JSONObject;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.models.CreateBatchOptions;
import com.loreal.clock.controller.OrderControllerBuChaiThreadNew;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class EventHubAsync {

    private static final Logger logger = LoggerFactory.getLogger(EventHubAsync.class);
    public static void main(String[] args) {
        String endpoint = "Endpoint=sb://standard.servicebus.chinacloudapi.cn/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=b6mJdzrLM0iRXJB0JZnv58UxJNfj79xDCnnrQ+cchHA=";
        String ehname = "one";
        EventHubProducerAsyncClient producer = makeAsyncClient(endpoint, ehname);
        for (int i = 0; i < 500; i++) {
            //sendEvent(producer, "firetruck",0,"1");
        }
        //System.out.println("All good my brutha");
    }

    public static EventHubProducerAsyncClient makeAsyncClient(String endpoint, String ehname) {
        EventHubProducerAsyncClient producer = new EventHubClientBuilder()
                .connectionString(endpoint, ehname)
                .buildAsyncProducerClient();
        return producer;
    }

    public static JSONObject sendEvent(EventHubProducerAsyncClient producer, String event, int tid_sj, String tid, String idKey) {
        CreateBatchOptions options = new CreateBatchOptions().setMaximumSizeInBytes(600000);
        JSONObject json = new JSONObject();
        try {
            AtomicReference<EventDataBatch> currentBatch = new AtomicReference<>(
                    producer.createBatch(options).block())
                    ;
            AtomicBoolean errorExists = new AtomicBoolean(false);

            producer.createBatch().flatMap(batch -> {
                batch.tryAdd(new EventData(event));
                return producer.send(batch);
            }).subscribe(unused -> {
                    },
                    error -> {
                        logger.error("Error occurred while sending batch:" + error);
                        errorExists.set(true);
                    });
        } catch (Exception e){
            logger.error("=============="+e);
            json.put("tid",tid);
            json.put("code","ERROR");
            json.put("message",e.toString());
            return json;
        }
        json.put(idKey,tid);
        json.put("code","Success");
        json.put("message","");
        return json;
    }
    public static boolean sendEvent(EventHubProducerAsyncClient producer, String event) {
        CreateBatchOptions options = new CreateBatchOptions().setMaximumSizeInBytes(1000000);
        AtomicReference<EventDataBatch> currentBatch = new AtomicReference<>(
                producer.createBatch(options).block())
                ;
        AtomicBoolean errorExists = new AtomicBoolean(false);
        producer.createBatch().flatMap(batch -> {
            batch.tryAdd(new EventData(event));
            return producer.send(batch);
        }).subscribe(unused -> { },
                error -> {
                    logger.error("Error occurred while sending batch:" + error);
                    errorExists.set(true);
                },
                () -> logger.info(" Send complete."));
        if (errorExists.get() == false) {
            return true;
        }
        return false;
    }

    public static boolean sendEventBatch(EventHubProducerAsyncClient producer, ArrayList<String> events) {
        CreateBatchOptions options = new CreateBatchOptions().setMaximumSizeInBytes(9000);
        AtomicReference<EventDataBatch> currentBatch = new AtomicReference<>(
                producer.createBatch(options).block())
                ;
        AtomicBoolean errorExists = new AtomicBoolean(false);
        producer.createBatch().flatMap(batch -> {
            for (String event : events) {
                //System.out.println("sending batch: " + event);
                batch.tryAdd(new EventData(event));
            }
            return producer.send(batch);
        }).subscribe(unused -> { },
                error -> {
                    System.err.println("Error occurred while sending batch:" + error);
                    errorExists.set(true);
                },
                () -> System.out.println("Send complete."));
        if (errorExists.get() == false) {
            return true;
        }
        return false;
    }
}
