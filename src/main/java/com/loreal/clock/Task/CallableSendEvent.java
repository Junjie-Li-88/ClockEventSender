package com.loreal.clock.Task;

import com.alibaba.fastjson.JSONObject;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.loreal.clock.EventHubAsync;
import com.loreal.clock.GetCosmosMultiple;

import java.util.concurrent.Callable;

public class CallableSendEvent implements Callable<Object> {
    private static EventHubProducerAsyncClient producerAsync;
    private String parm;
    public CallableSendEvent(EventHubProducerAsyncClient producer, String parm) {
        this.parm = parm;
        this.producerAsync = producer;
    }
    //https://gem-api-uat.platform-loreal.cn/cn/sell_out/tmall/v1/order_event
    @Override
    public Object call() throws Exception {
        try {
            return EventHubAsync.sendEvent(producerAsync, parm);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
