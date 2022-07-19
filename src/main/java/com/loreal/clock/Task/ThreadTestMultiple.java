package com.loreal.clock.Task;

import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.loreal.clock.EventHubAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ThreadTestMultiple extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ThreadTest.class);

    private static EventHubProducerAsyncClient itemProducerAsync;
    @Override
    public void run() {
        EventHubAsync.sendEventBatch(itemProducerAsync, parm);
    }

    private ArrayList<String> parm;

    public ArrayList<String> getParm() {
        return parm;
    }

    public void setParm(ArrayList<String> parm) {

        this.parm = parm;
    }

    public ThreadTestMultiple(EventHubProducerAsyncClient itemProducerAsync, ArrayList<String> parm) {
        this.parm = parm;
        this.itemProducerAsync = itemProducerAsync;
    }
}
