package com.loreal.clock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.PartitionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Array;
import java.util.ArrayList;

@RestController
public class GetCosmosAbnormal {
    private static final Logger logger = LoggerFactory.getLogger(com.loreal.clock.controller.OrderControllerBuChaiThreadNew.class);
    @Value("${eventHub.client.cosmosEndpoint}")
    private String cosmosEndpoint;
    @Value("${eventHub.client.cosmosKey}")
    private String cosmosKey;
    CosmosClient cosmosClient = null;
    CosmosDatabase cosmosdb = null;
    CosmosContainer cosmosCont = null;
    CosmosContainer cosmosAbnormalCont = null;
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
            "event_message"
    };

    @RequestMapping(value = "/getCosmosAbnormal", produces = "application/json;charset=utf-8")
    public String handleReturn(@RequestBody String parameters, HttpServletResponse response) {
        if (cosmosdb == null || cosmosCont == null) {
            System.out.println("it's firetrucking null, calling initCosmos");
            try {
                initCosmos("test", "test3");
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return "error connecting to specified container: " + e;
            }
        }
        //1 = 只有oid, 2 = 有refund_id和oid, 默认是2

        boolean hasRefundId = false;
        JSONObject reqObject = JSONObject.parseObject(parameters);
        String id = reqObject.getString("oid");
        String refundid = reqObject.getString("refund_id");

        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "oid cannot be null.";
        }

        if (refundid != null) {
            hasRefundId = true;
        }
        // order api:
        // oid = (/id=oid /partition_key=oid) (common)

        // refund api:
        // refund_id (/id=refund_id /partition_key=refund_id) (common)
        // sql api?
        //tuidan partition key is refund_id
        int status = 0;
        PartitionKey abnormalPartKey = new PartitionKey(id);
        PartitionKey commmonPartKey = null;
        if (!hasRefundId) {
            try {
                JSONObject commonitem = null;
                JSONObject abnormalitem = cosmosAbnormalCont.readItem(id, abnormalPartKey, JSONObject.class).getItem();
                status = 1;
                JSONArray refund_ids = abnormalitem.getJSONArray("refund");
                JSONArray results = new JSONArray();
                JSONArray errors = new JSONArray();
                if (refund_ids == null) {
                    return "Entry in abnormal container exists but has no refund";
                } else {
                    System.out.println(refund_ids);
                    for (Object o : refund_ids) {
                        try {
                            System.out.println(o);
                            commmonPartKey = new PartitionKey(id);
                            commonitem = cosmosCont.readItem(o.toString(), commmonPartKey, JSONObject.class).getItem();
                            System.out.println(commonitem);
                            for (String keyToRemove : toDel) {
                                commonitem.remove(keyToRemove);
                            }
                            results.add(commonitem);
                        } catch (Exception e) {
                            System.out.println("error getting refundid: " + o.toString() + e.toString());
                            errors.add(o.toString());
                        }
                    }
                    System.out.println("im out of the for loop");
                    JSONObject ret = new JSONObject();
                    ret.put("results", results);
                    ret.put("errors", errors);
                    return ret.toJSONString();
                }
                //System.out.println(item.toJSONString());
                // /id = oid?  /partition_key = oid? -> refund_id
                // /refund_id = (common de /id), /refund_id = (common de /partition_key)
            } catch (Exception e) {
                if (e instanceof NotFoundException) {
                    if (status == 0) {
                        response.setStatus(HttpServletResponse.SC_ACCEPTED);
                        return "This is not an abnormal order.";
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return "Cannot find entry with refund_id in Common container";
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    System.out.println(e);
                    return "An error has occured: " + e.toString();
                }
            }
        } else {
            try {
                commmonPartKey = new PartitionKey(refundid);
                JSONObject commonitem = cosmosCont.readItem(refundid, commmonPartKey, JSONObject.class).getItem();
                for (String keyToRemove : toDel) {
                    commonitem.remove(keyToRemove);
                }
                return commonitem.toJSONString();
            } catch (Exception e) {
                if (e instanceof NotFoundException) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return "Cannot find entry with refund_id in Common container";
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return "An error has occured: " + e.toString();
                }
            }
        }
    }

    //TODO: figure out the logic
    public void initCosmos(String dbname, String containerName) {
        System.out.println("creating cosmos client");
        cosmosClient = new CosmosClientBuilder()
                .endpoint(cosmosEndpoint)
                .key(cosmosKey)
                .buildClient();
//        cosmosClient.createDatabaseIfNotExists("test")
//                // TIP: Our APIs are Reactor Core based, so try to chain your calls
//                .map(databaseResponse -> cosmosClient.getDatabase(databaseResponse.getProperties().getId()))
//                .subscribe(database -> System.out.printf("Created database '%s'.%n", database.getId()));
        System.out.println("initing getDatabase");
        cosmosdb = cosmosClient.getDatabase("clockcommon");
        System.out.println("initing cont");
        cosmosCont = cosmosdb.getContainer("tmall_common");
        System.out.println("init common cont ok");
        cosmosAbnormalCont = cosmosdb.getContainer("tmall_abnormal");
        System.out.println("init cosmosAbnormalCont ok");
    }
}