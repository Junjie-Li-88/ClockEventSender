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

public class CallableWriteCosmos implements Callable<Object> {
    private Object toInsert;
    CosmosAsyncContainer cosmosAsyncCont;

    public CallableWriteCosmos(Object toInsert, CosmosAsyncContainer cosmosAsyncCont) {
        this.toInsert = toInsert;
        this.cosmosAsyncCont = cosmosAsyncCont;
    }

    @Override
    public Object call() throws Exception {
        try {
            cosmosAsyncCont.createItem(toInsert);
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}

