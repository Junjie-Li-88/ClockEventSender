package com.loreal.clock.Task;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;

public class ThreadGetCosmos extends Thread{
    private static CosmosAsyncClient cosmosAsyncClient;
    public ThreadGetCosmos(EventHubProducerAsyncClient itemProducerAsync, String parm) {
        //this.parm = parm;
        //this.itemProducerAsync = itemProducerAsync;
    }

    @Override
    public void run() {
        //CosmosAsyncClient cosmosAsyncClient.readItem();
    }

}
