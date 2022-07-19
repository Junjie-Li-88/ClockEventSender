package com.loreal.clock.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.azure.cosmos.*;
import com.azure.cosmos.implementation.ConflictException;
import com.azure.cosmos.implementation.uuid.UUIDClock;
import com.azure.cosmos.implementation.uuid.UUIDGenerator;
import com.azure.cosmos.implementation.uuid.UUIDType;
import com.azure.cosmos.implementation.uuid.impl.UUIDUtil;
import com.azure.messaging.eventhubs.*;
import com.loreal.clock.*;
import com.loreal.clock.Task.ThreadTestMultiple;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.loreal.clock.StringsAndConnectors.*;

@RestController
@NoArgsConstructor
public class ReturnControllerMultiple {
    private static final Logger logger = LoggerFactory.getLogger(OrderControllerBuChaiThreadNew.class);
    /*comosdb库*/
    private OrderControllerBuChaiThreadNew.CallBack callBack;
    @Value("${thread.data.num}")
    private int singleCount;
    @Value("${cosmosdb.Database}")
    private String cosmosDatabase;
    /*cosmos表*/
    @Value("${cosmosdb.table}")
    private String cosmosTable;

    /*eventHub*/
    private ExecutorService executorService;
    @Value("${eventHub.client.cosmosEndpointTemp}")
    private String cosmosEndpoint;
    @Value("${eventHub.client.cosmosKeyTemp}")
    private String cosmosKey;
    @Value("${logApiUrl}")
    private String logApiUrl;
    /*退单topic*/
    @Value("${eventHub.returnOrder.topic.name}")
    private String eventHubNameTopic;
    private CountDownLatch begin,end;
    String oid = null;
    private List<String> list;
    //处理的总数据量
    private int listSize;
    @Value("${recThreshold}")
    private int recThreshold;
    //开启的线程数
    private int runSize;
    //    private static String connectionString = "Endpoint=sb://standard.servicebus.chinacloudapi.cn/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=b6mJdzrLM0iRXJB0JZnv58UxJNfj79xDCnnrQ+cchHA=";
//    private static String cosmosEndpoint = "https://publiccosmos.documents.azure.cn:443/";
//    private static String cosmosKey = "WRpTSEy0x9buUz7sWxxJZFrDQP2r7HbnPZVbZsyeHkObH13zkq0zRD3U1r2YAAnb45NeJeapCUVZX9AZ5JLX3A==";
    private static EventHubProducerClient returnProducer;
    private static EventHubProducerAsyncClient returnProducerAsync;
    private static String receiveTime;
    private static String eventHubName = "return_order";
    private static CosmosAsyncDatabase cosmosAsyncDb = null;
    private static CosmosAsyncContainer cosmosAsyncCont = null;
    private static boolean isSuccess = false;
    private String errorMessage = "";
    private final String path = "/handleReturnBuChai";

    @RequestMapping(path="/handleReturnBuChaiMultiple",produces = "application/json;charset=utf-8")
    public String handleReturn(@RequestBody String parameters, HttpServletResponse response) throws IOException {
        String bigString = "";
        List<JSONObject> list_json = new ArrayList<JSONObject>();
        list = new ArrayList<String>();
//        String decode = parameters;
//        try {
//            decode = URLDecoder.decode(parameters, "utf-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        //调用解析json的方法
        JSONArray events = null;
        try {
            events = JSONArray.parseArray(parameters);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return ErrorMessageJson.newMessage("Body does not contain a valid js array", 400, "/ReturnController");
        }
        ArrayList<String> eventsString = new ArrayList<>();
        if (returnProducerAsync == null ) {
//            initProducers();
        }
        //TODO: don't convert from object to string over and over again!!
        for (int i = 0; i < events.size(); i++) {
//            System.out.println((JSONObject) events.get(i));
            String toAdd = null;
            try {
                toAdd = JsonParameters((JSONObject) events.get(i)).toString();
            } catch (NullPointerException e) {
                if (StringsAndConnectors.returnProducers.size() != 0) {
                    JSONObject errmsg = new JSONObject();
                    errmsg.put("code", "ERROR");
                    errmsg.put("message", errorMessage + events.get(i).toString().substring(0, Math.max(100, events.get(i).toString().length())));
                    //errmsg.put("tid", obj.toString());
                    try {
                        JSONObject rawObj = (JSONObject) events.get(i);
                        String tid = rawObj.getJSONObject("refund_get_response").getJSONObject("refund").getString("tid");
                        errmsg.put("tid", tid);
//                        HttpClient.makePostRequest(logApiUrl, LogApiHelper.makeErrorJson(errmsg).toString(), null);
                    } catch (Exception nestede){
                        System.out.println("Not able to retrieve tid in refund:" + nestede);
                    }
                    list_json.add(errmsg);
                    continue;
                }
            }
            if (toAdd == null) {
                if (StringsAndConnectors.returnProducers.size() != 0) {
                    JSONObject errmsg = new JSONObject();
                    errmsg.put("code", "ERROR");
                    errmsg.put("message", errorMessage + events.get(i).toString().substring(0, Math.max(100, events.get(i).toString().length())));
                    try {
                        JSONObject rawObj = (JSONObject) events.get(i);
                        String tid = rawObj.getJSONObject("refund_get_response").getJSONObject("refund").getString("tid");
                        errmsg.put("tid", tid);
                    } catch (Exception e){
                        System.out.println("Not able to retrieve tid in refund:" + e);
                    }
                    //errmsg.put("tid", obj.toString());
                    list_json.add(errmsg);
//                    HttpClient.makePostRequest(logApiUrl, LogApiHelper.makeErrorJson(errmsg).toString(), null);
                    continue;
                }
            } else {
//            System.out.println(toAdd);
                list.add(toAdd);
            }
        }

        int validEvents = list.size();
        int invalidEvents = events.size() - validEvents;
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        numtids += validEvents;
        numerrs += invalidEvents;
        numreqs += 1;
        mutex.release();

        if (numreqs > recThreshold) {
            HashMap<String, Integer> fields = new HashMap<>();
            fields.put("received_items", numItems);
            fields.put("received_tid", numtids);
            fields.put("received_tid_invalid", numerrs);
            fields.put("received_message", numreqs);

            HashMap<String, Integer> fieldsPbi = new HashMap<>();
            fieldsPbi.put("sum_received_items", numItems);
            fieldsPbi.put("sum_received_tid", numtids);
            fieldsPbi.put("sum_received_tid_invalid", numerrs);
            fieldsPbi.put("sum_received_message", numreqs);

            JSONArray statJson = LogApiHelper.makeStatJson(fields, fieldsPbi);
            HttpClient.makePostRequest(logApiUrl, statJson.toString(), null, null);
            numerrs = 0;
            numtids = 0;
            numreqs = 0;
            numItems = 0;
        }

        //TODO: make it use the list of return producers and do excute thing
        //replace hardcoded singleCount with singleCount
        ReturnControllerMultiple tool = new ReturnControllerMultiple(singleCount,list);

        tool.setCallBack(new OrderControllerBuChaiThreadNew.CallBack<String>() {
            @Override
            public void method(List<String> list) {
                if (StringsAndConnectors.returnProducers.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        JSONObject s = JSONObject.parseObject(list.get(i));
                        String tid = s.getJSONObject("refund_get_response").getJSONObject("refund").getString("refund_id");
                        if (StringsAndConnectors.returnProducers.size() == 0) {
                            JSONObject errmsg = new JSONObject();
                            errmsg.put("code", "ERROR");
                            errmsg.put("msg", "There are no eventhubs configured at the moment");
                            errmsg.put("tid", tid);
                            list_json.add(errmsg);
                        } else {
                            //send event
                            JSONObject event_json = EventHubAsync.sendEvent(StringsAndConnectors.returnProducers.get(i % StringsAndConnectors.returnProducers.size()), list.get(i), 0, tid, "refund_id");
                            //create cosmos item success
                            JSONObject meat = s.getJSONObject("refund_get_response").getJSONObject("refund");
                            boolean isSuccess = meat.getString("status").equals("SUCCESS");
                            if (isSuccess) {
                                logger.info("status为success isSUccess is " + isSuccess);
                                boolean createItemSuccess = createItem(s, oid);
                                if (createItemSuccess) {
//                                    System.out.println("createitem success");
                                    list_json.add(event_json);
                                } else {
                                    JSONObject retrec = new JSONObject();
                                    retrec.put("tid",tid);
                                    retrec.put("code","ERROR");
                                    retrec.put("message","failed to create item in cosmosdb");
//                                    System.out.println("createitem not success");
                                    list_json.add(retrec);
                                }
                            } else {
//                                System.out.println("no need to create item; returning event send status");
                                list_json.add(event_json);
                            }
                        }
                    }
                }
            }
        });
        try {
//            System.out.println("excuting");
            tool.excute();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //send to event hubs
//        //TODO: change to callable to get send status
////        ThreadTestMultiple test = new ThreadTestMultiple(returnProducerAsync, eventsString);
//        test.start();
        //TODO: check that createItem is success before sending OK
        return list_json.toString();
    }

    public interface CallBack<T>{
        public void method(List<T> list);
    }

    public void excute() throws InterruptedException {
        //runSize = 50;
        executorService = Executors.newFixedThreadPool(runSize);
//        System.out.println("im in excute baby");
        begin = new CountDownLatch(1);
        end = new CountDownLatch(runSize);
        //建立线程
        int startIndex = 0;
        int endIndex = 0;
        List<String> newList = null;
        for (int i = 0; i < runSize; i++) {
            //计算每一个线程对应的数据
            if (i < (runSize - 1)){
                startIndex = i * singleCount;
                endIndex = (i + 1) * singleCount;
                newList = list.subList(startIndex,endIndex);
            }else {
                startIndex = i * singleCount;
                endIndex = listSize;
                newList = list.subList(startIndex,endIndex);
            }
            //建立线程类处理数据
            MyThread myThread = new MyThread(newList, begin, end) {
                @Override
                public void method(List list) {
//                    System.out.println("calling callBack.method");
                    callBack.method(list);
                }
            };
            //执行线程
            executorService.execute(myThread);
        }
        //计数器减一
        begin.countDown();
        end.await();
        //关闭线程池
        executorService.shutdown();
    }

    public abstract class MyThread<T> implements Runnable{

        private List<T> list;
        private CountDownLatch begin,end;

        public MyThread(List<T> list, CountDownLatch begin, CountDownLatch end){
            this.list = list;
            this.begin = begin;
            this.end = end;
        }

        @Override
        public void run() {
            try {
                //执行程序
                method(list);
                begin.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                //计数器减一
                end.countDown();
            }
        }

        public abstract void method(List<T> list);
    }


    public void setCallBack(OrderControllerBuChaiThreadNew.CallBack callBack) {
        this.callBack = callBack;
    }

    public void initCosmos() {
//        System.out.println("creating cosmos client");
        CosmosAsyncClient cosmosAsyncClient = new CosmosClientBuilder()
                .endpoint(cosmosEndpoint)
                .key(cosmosKey)
                .buildAsyncClient();
        cosmosAsyncDb = cosmosAsyncClient.getDatabase(cosmosDatabase);
        cosmosAsyncCont = cosmosAsyncDb.getContainer(cosmosTable);
    }

    //创建
    public boolean createItem(Object json, String partitionKey) {
        if (cosmosAsyncDb == null || cosmosAsyncCont == null) {
            System.out.println("it's null, calling initCosmos");
            initCosmos();
        }
        JSONObject toInsert = (JSONObject) json;
        String guid = "";
        if (toInsert.containsKey("id")) {
            guid = toInsert.getString("id");
        }
//        System.out.println("Sending to cosmos THIS:\n" + json);
        int createStatusCode = -1;
        try {
            createStatusCode = cosmosAsyncCont.createItem(json).block().getStatusCode();
            return true;
        } catch (ConflictException ce) {
            UUID secondID = UUIDUtil.constructUUID(UUIDType.RANDOM_BASED, new Random().nextInt(), 99999);
            toInsert.put("id", UUID.randomUUID().toString());
            try {
                int secondTryStatus = cosmosAsyncCont.createItem(json).block().getStatusCode();
                System.out.println("secondTryStatus is: " + secondTryStatus);
                return true;
            } catch (Exception e) {
                System.out.println("second creation not successful: " + e);
                return false;
            }
        }
    }


    public ReturnControllerMultiple(int singleCount, List<String> list){
        this.singleCount = singleCount;
        this.list = list;
        if (list != null){
            this.listSize = list.size();
            this.runSize = (this.listSize/this.singleCount) + 1;
        }
    }

    //解析json数据
    public JSONObject JsonParameters(JSONObject parameters) {
//        logger.info("-----请求入参-----" + parameters);
        JSONObject res = null;
        try {
            if (parameters == null) {
                logger.info(" parameters is null,ClassName is:" + this.getClass().getName());
                errorMessage = "body is null";
                return null;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String format = dateFormat.format(new Date());
            if (returnProducer == null) {
//                initProducers();
            }
            JSONObject json = parameters;
            res = new JSONObject();
            JSONObject refundGetResponse = json.getJSONObject("refund_get_response");
            if (refundGetResponse == null) {
                errorMessage = "return 中 refund_get_response is null";
                logger.error(errorMessage);
                return null;

            }
            JSONObject refundJson = refundGetResponse.getJSONObject("refund");
            if (refundJson == null) {
                errorMessage = "return 中 refund is null ";
                logger.error(errorMessage);
                return null;
            }

            String oidJson = refundJson.getString("oid");
            if (oidJson == null) {
                errorMessage = "return 中 oid is null";
                logger.error(errorMessage);
                return null;
            }
            oid = oidJson;
            if (refundJson.getString("status") == null) {
                errorMessage = "获取json中的 status 为空";
                logger.error(errorMessage);
                return null;
            }
            isSuccess = refundJson.getString("status").equals("SUCCESS");
            /*nanxunTimes*/
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, 8);
            Date newDate = calendar.getTime();
            String newDateString = df.format(newDate);
            logger.info("utc格式的时间：" + newDateString);

            /*新增nanXun和ali字段*/
            if (refundJson.containsKey("jdp_modified")) {
//            tradeJson.put("ali_send_timestamp", tradeJson.get("jdp_created"));
                refundJson.put("ali_send_timestamp", refundJson.get("jdp_modified"));
            }
            if (refundJson.containsKey("request_time")) {
                refundJson.put("nanxun_send_timestamp", refundJson.get("request_time"));
            }

            /*新增时间值*/
            refundJson.put("apps_send_timestamp", newDateString);
            refundJson.put("apps_receive_timestamp", newDateString);
            /*外层塞值*/
            JSONObject refundNew = new JSONObject();
            //Iterator<String> refundJsonIterator = refundJson.keySet().iterator();
            //Iterator<String>
            //res.putAll(refundJson);
            refundNew.put("refund", refundJson);
            res.put("refund_get_response", refundNew);
            /*新增type 暂时值为oid*/
            res.put("partition_key", oidJson);
            String curId = UUID.randomUUID().toString();
            res.put("id", curId);
            /*新增type 暂时值为refund*/
            res.put("model_type", "refund");
        } catch (Exception e) {
            errorMessage = "错误json入参";
            logger.error(errorMessage + "\n" + parameters);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.error(sw.toString());
            return null;
        }
//        System.out.println("RES:" + res.toString());
        return res;
    }
}
