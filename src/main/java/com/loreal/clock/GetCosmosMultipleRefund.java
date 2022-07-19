package com.loreal.clock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.azure.cosmos.*;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.loreal.clock.Task.CallableReadCosmos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class GetCosmosMultipleRefund {
    private static final Logger logger = LoggerFactory.getLogger(com.loreal.clock.controller.OrderControllerBuChaiThreadNew.class);
    //private static String cosmosEndpoint = "https://publiccosmos.documents.azure.cn:443/";
    //private static String cosmosKey = "WRpTSEy0x9buUz7sWxxJZFrDQP2r7HbnPZVbZsyeHkObH13zkq0zRD3U1r2YAAnb45NeJeapCUVZX9AZ5JLX3A==";

    @Value("${eventHub.client.cosmosEndpoint}")
    private String cosmosEndpoint;
    @Value("${eventHub.client.cosmosKey}")
    private String cosmosKey;

    private static CosmosClient cosmosClient = null;
    private static CosmosDatabase cosmosdb = null;
    private static CosmosContainer cosmosCont = null;
    private static CosmosAsyncDatabase cosmosAsyncDb = null;
    private static CosmosAsyncContainer cosmosAsyncCont = null;
    private static boolean isSuccess = false;
    public static String[] toDel = new String[] {
            "event_type",
            "event_status",
            "apps_send_timestamp",
            "changef_send_cosmos_timestamp",
            "guid",
            "event_message",
            "datab_receive_timestamp",
            "datab_send_timestamp",
            "changef_all_receive_timestamp",
            "eventh_receive_timestamp",
            "model_type",
            "eventh_send_timestamp",
            "_rid",
            "_self",
            "_etag",
            "_attachments",
            "_ts",
            "changef_finish_send_eventh_timestamp",
            "event_message",
            "changef_begin_send_eventh_timestamp",
            "eventh"
    };

    @RequestMapping(value = "/getCosmosMultipleRefund", produces = "application/json;charset=utf-8")
    public String handleReturn(@RequestBody String parameters, HttpServletResponse response) throws ExecutionException, InterruptedException {
        // 0 = 没初始化, 1 = 有refund_id和oid, 2 = 只有oid, 3 = 不符合规范
        int requestType = 0;
        if (cosmosdb == null && cosmosAsyncDb == null) {
            System.out.println("it's firetrucking null, calling initCosmos");
            try {
                initCosmos();
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return "error connecting to specified container: " + e;
            }
        }
        JSONArray oids = JSONArray.parseArray(parameters);
        JSONArray ret = sendIt(oids);
        return ret.toString();
    }

    //take in jsonarray of oids and returns jsonarray of cosmos items
    public JSONArray sendIt(JSONArray array) throws ExecutionException, InterruptedException {
        JSONArray ret = new JSONArray();
        List<Future> list = new ArrayList<Future>();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(40,array.size()));
        for (int i = 0; i < array.size(); i++) {
            try {
                //old: ["123"]; new: [{"oid":"123", "refundid":"456"}]
                JSONObject o = (JSONObject) array.get(i);
                String refundId = o.getString("refund_id");
                String oid = o.getString("oid");
                //what to do if partial jsons are good and rest are bad?
                if (refundId == null || oid == null) {
                    return new JSONArray();
                }
                Callable c = new CallableReadCosmos(oid, cosmosAsyncCont, refundId, true);
                Future f = pool.submit(c);
                list.add(f);
            } catch (Exception e) {
                if (e instanceof NotFoundException) {
                    System.out.println("not found");
                } else {
                    System.out.println("sth went wrong: " + e);
                    e.printStackTrace();
                }
            }
        }
        pool.shutdown();
        //System.out.println("size of list is " + list.size() + ", size of array is: " + array.size());
        for (int i = 0; i < list.size(); i++) {
            Future f = list.get(i);
            String id = ((JSONObject) array.get(i)).getString("oid");//.toString();
            Object s = f.get();
            if (s != null) {
                ret.add(s);
            }
            //ret.add(f.get());
            // 从Future对象上获取任务的返回值，并输出到控制台
            //System.out.println(">>>" + f.get().toString()); //OPTION + return 抛异常
        }
        return ret;
    }

    public void initCosmos() {
        System.out.println("creating cosmos client");
//        cosmosClient = new CosmosClientBuilder()
//                .endpoint(cosmosEndpoint)
//                .key(cosmosKey)
//                .buildClient();
        CosmosAsyncClient cosmosAsyncClient = new CosmosClientBuilder()
                .endpoint(cosmosEndpoint)
                .key(cosmosKey)
                .buildAsyncClient();
//        System.out.println("initing getDatabase");
//        cosmosdb = cosmosClient.getDatabase("clockcommon");
//        System.out.println("initing cont");
//        cosmosCont = cosmosdb.getContainer("tmall_common");

        cosmosAsyncDb = cosmosAsyncClient.getDatabase("clockcommon");
        cosmosAsyncCont = cosmosAsyncDb.getContainer("tmall_common");

        System.out.println("init cont ok");
    }
}