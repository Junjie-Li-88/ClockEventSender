package com.loreal.clock.Task;

import com.alibaba.fastjson.JSONObject;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.loreal.clock.EventHubAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ThreadTest extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ThreadTest.class);

    private static EventHubProducerAsyncClient itemProducerAsync;
    @Override
    public void run() {
        EventHubAsync.sendEvent(itemProducerAsync, parm);
    }

    private String parm;

    public String getParm() {
        return parm;
    }

    public void setParm(String parm) {

        this.parm = parm;
    }

    public ThreadTest(EventHubProducerAsyncClient itemProducerAsync, String parm) {
        this.parm = parm;
        this.itemProducerAsync = itemProducerAsync;
    }


}

