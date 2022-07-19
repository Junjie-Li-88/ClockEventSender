package com.loreal.clock.Task;

import com.alibaba.fastjson.JSONObject;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.loreal.clock.GetCosmosMultiple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableReadCosmos implements Callable<Object> {
    private String oid;
    CosmosAsyncContainer cosmosAsyncCont;
    private boolean isRefund;
    private String refundId;
    public CallableReadCosmos(String oid, CosmosAsyncContainer container, String refundId, boolean isRefund) {
        this.oid = oid;
        this.cosmosAsyncCont = container;
        this.isRefund = isRefund;
        this.refundId = refundId;
    }

    @Override
    public Object call() throws Exception {
        try {
            PartitionKey partKey = new PartitionKey(oid);
            CosmosItemResponse responseItem = null;
            if (isRefund) {
                responseItem = cosmosAsyncCont.readItem(refundId, partKey, JSONObject.class).block();
            } else {
                responseItem = cosmosAsyncCont.readItem(oid, partKey, JSONObject.class).block();
            }
            JSONObject item = (JSONObject) responseItem.getItem();
            for (String s : GetCosmosMultiple.toDel) {
                item.remove(s);
            }
//            if (item.containsKey("order_id")) {
//                Object order_id = item.get("order_id");
//                item.put("order_id", order_id.toString());
//            }
            return item;
        } catch (Exception e) {
            System.out.println("cosmos read exception: " + oid + " " + e );
            return null;
        }
    }
}
