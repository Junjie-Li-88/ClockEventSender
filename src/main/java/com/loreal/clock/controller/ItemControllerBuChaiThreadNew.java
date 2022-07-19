package com.loreal.clock.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.loreal.clock.*;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.loreal.clock.StringsAndConnectors.*;
import static com.loreal.clock.StringsAndConnectors.numreqs;

@RestController //该注解是 @Controller 和 @ResponseBody 注解的合体版
@NoArgsConstructor
public class ItemControllerBuChaiThreadNew {
    private static final Logger logger = LoggerFactory.getLogger(ItemControllerBuChaiThreadNew.class);
    @Value("${logApiUrl}")
    private String logApiUrl;
    @Value("${recThreshold}")
    private int recThreshold;
    //单个线程处理的数据量
    @Value("${thread.data.num}")
    private int singleCount;
    //处理的总数据量
    private int listSize;
    //开启的线程数
    private int runSize;
    //操做的数据集
    private List<String> list;
    //计数器
    private CountDownLatch begin,end;
    //线程池
    private ExecutorService executorService;
    //回调
    private CallBack callBack;

//    @Value("${eventHub.client.connectionString}")
//    private String connectionString;
    /*topic*/
    @Value("${eventHub.item.topic.name}")
    private String eventHubName;
    private static EventHubProducerClient itemProducer;
    private static EventHubProducerAsyncClient itemProducerAsync;
    private String errorMessage = "";
    public final static String path = "/handleItemBuChaiThreadMultiple";

    //    private static String connectionString = "Endpoint=sb://standard.servicebus.chinacloudapi.cn/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=b6mJdzrLM0iRXJB0JZnv58UxJNfj79xDCnnrQ+cchHA=";
//    private static String eventHubName = "item";
    @RequestMapping(path="/handleItemBuChaiThreadMultiple", produces = "application/json;charset=utf-8")
    public String handleItem(@RequestBody String parameters, HttpServletResponse response) throws IOException {
        List<String> list = new ArrayList<String>();
        List<JSONObject> list_json = new ArrayList<JSONObject>();
        if (parameters == null) {
            logger.info(" parameters is null,ClassName is:" + this.getClass().getName());
            errorMessage = "parameters is null";
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return ErrorMessageJson.newMessage("body cannot be null", 400, path);
        }
        JSONArray arrayOfItems = null;
        try {
            arrayOfItems = JSONArray.parseArray(parameters);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return ErrorMessageJson.newMessage("Body does not contain a valid js array", 400, "/ItemController");
        }

        for (int i = 0; i < arrayOfItems.size(); i++) {
            try {
                JSONObject curObj = JsonParameters(arrayOfItems.getJSONObject(i));
                    if (curObj == null && StringsAndConnectors.itemProducers.size() > 0) {
                        JSONObject errmsg = new JSONObject();
                        errmsg.put("code", "ERROR");
                        errmsg.put("message", errorMessage + arrayOfItems.get(i));
                        try {
                            JSONObject rawObj = arrayOfItems.getJSONObject(i);
                            String num_iid = rawObj.getJSONObject("item_get_response").getJSONObject("item").getString("num_iid");
                            errmsg.put("num_iid", num_iid);
                        } catch (Exception e) {
                            System.out.println("Not able to retrieve num_iid in item:" + e);
                        }
                        list_json.add(errmsg);
//                        HttpClient.makePostRequest(logApiUrl, LogApiHelper.makeErrorJson(errmsg).toString(), null);
                        continue;
                } else {
                    list.add(curObj.toString());
                }
            } catch (NullPointerException e) {
                if (StringsAndConnectors.itemProducers.size() > 0) {
                    JSONObject errmsg = new JSONObject();
                    errmsg.put("code", "ERROR");
                    errmsg.put("message", errorMessage + arrayOfItems.get(i));
                    try {
                        JSONObject rawObj = arrayOfItems.getJSONObject(i);
                        String num_iid = rawObj.getJSONObject("item_get_response").getJSONObject("item").getString("num_iid");
                        errmsg.put("num_iid", num_iid);
                    } catch (Exception nestede){
                        System.out.println("Not able to retrieve num_iid in item:" + nestede);
                    }
                    list_json.add(errmsg);

//                    HttpClient.makePostRequest(logApiUrl, LogApiHelper.makeErrorJson(errmsg).toString(), null);
                    continue;
                }
            }
        }

        int validEvents = list.size();
        int invalidEvents = arrayOfItems.size() - validEvents;
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        numItems += validEvents;
        numerrs += invalidEvents;
        numreqs += 1;
        mutex.release();

        //TODO: make this abstract
        if (numreqs > recThreshold) {
            System.out.println(numreqs + " > " + recThreshold);
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
//            System.out.println(statJson);
            numerrs = 0;
            numtids = 0;
            numreqs = 0;
            numItems = 0;
        }

//        System.out.println(list);
        ItemControllerBuChaiThreadNew tool = new ItemControllerBuChaiThreadNew(singleCount,list);
        tool.setCallBack(new CallBack<String>() {
            @Override
            public void method(List<String> list) {
                for (int i = 0; i < list.size(); i++) {
                    JSONObject myjson = JSONObject.parseObject(list.get(i));
                    String itemtid = null;
                    try {
                        itemtid = myjson.getJSONObject("item_get_response").getJSONObject("item").getString("num_iid");
                    } catch (Exception e) {
                        System.out.println("something doesnt exist in item");
                    }
                    if (StringsAndConnectors.itemProducers.size() == 0) {
                        JSONObject errmsg = new JSONObject();
                        errmsg.put("code", "ERROR");
                        errmsg.put("msg", "There are no eventhubs configured at the moment");
                        if (itemtid != null) {
                            errmsg.put("tid", itemtid);
                        }
                        list_json.add(errmsg);
                    } else {
                        //System.out.println(itemtid);
                        logger.info("Thread:" + Thread.currentThread().getId() + " === " + i + "/" + list.size() + " ");
                        JSONObject s = EventHubAsync.sendEvent(StringsAndConnectors.itemProducers.get(i % StringsAndConnectors.itemProducers.size()), list.get(i), 0, itemtid, "num_iid");
                        list_json.add(s);
                    }
                }
            }
        });

        try {
            tool.excute();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String bigStringForDebugging = null;
        //System.out.println(bigStringForDebugging);
        response.setStatus(HttpServletResponse.SC_OK);
        return list_json.toString();
    }

    public String getEventHubName() {
        return eventHubName;
    }
    //TODO: create initProducer class
//    public void initProducers() {
//        boolean initSuccess = false;
//        try {
//            itemProducer = new EventHubClientBuilder()
//                    .connectionString(connectionString, eventHubName)
//                    .buildProducerClient();
//            initSuccess = true;
//            itemProducerAsync = EventHubAsync.makeAsyncClient(connectionString, eventHubName);
//        } catch (Exception e) {
//            logger.error("Error initializing producer; retrying");
////            initProducers();
//        }
//    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public ItemControllerBuChaiThreadNew(int singleCount, List<String> list){
        this.singleCount = singleCount;
        this.list = list;
        if (list != null){
            this.listSize = list.size();
            this.runSize = (this.listSize/this.singleCount) + 1;
        }
    }

    public void excute() throws InterruptedException {
        executorService = Executors.newFixedThreadPool(runSize);
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
            MyThread<String> myThread = new MyThread(newList, begin, end) {
                @Override
                public void method(List list) {
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

    //抽象线程类
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

    //回调接口定义
    public interface CallBack<T>{
        public void method(List<T> list);
    }

    public JSONObject JsonParameters(JSONObject json) {
//        logger.info("----------请求入参----------" + parameters);
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String format = dateFormat.format(new Date());
                //JSONObject  = (JSONObject) jsonArray.get(i);
                JSONObject res = new JSONObject();
                JSONObject response = new JSONObject();
                if (itemProducer == null) {
//                    initProducers();
                }
                JSONObject itemGetResponse = json.getJSONObject("item_get_response");
                if (itemGetResponse == null) {
                    errorMessage = "json中item_get_response is null";
                    logger.error(errorMessage);
                    return null;
                }
                JSONObject item = itemGetResponse.getJSONObject("item");
                if (item == null) {
                    errorMessage = "json中item is null";
                    logger.error(errorMessage);
                    return null;
                }

                JSONObject skus = item.getJSONObject("skus");
                if (skus == null) {
                    errorMessage = "json中skus is null";
                    logger.error(errorMessage);
                    return null;
                }
                /*nanxunTimes*/
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.HOUR_OF_DAY, 8);
                Date newDate = calendar.getTime();
                String newDateString = df.format(newDate);
                logger.info("utc格式的时间：" + newDateString);
                /*新增nanXun和ali字段*/
                //item.put("nanxun_send_timestamp", newDateString);
                //item.put("ali_send_timestamp", newDateString);
                /*新增字段*/
                item.put("apps_send_timestamp", newDateString);
                item.put("apps_receive_timestamp", newDateString);

                if (item.containsKey("jdp_created")) {
                    item.put("ali_send_timestamp", item.get("jdp_created"));
                    //System.out.println("ENG JDP CREATEd IS HERE!!!!!!!!!!!!!!!!!!!!!!!!!");

                }
                if (item.containsKey("request_time")) {
                    item.put("nanxun_send_timestamp", item.get("request_time"));
                }

                res.put("item", item);
                response.put("item_get_response", res);
                return response;
        } catch (NullPointerException e) {
            //logger.info("错误json入参：\n" + parameters);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.error(sw.toString());
            errorMessage = "---JSON解析异常---";
            logger.error(errorMessage);
            return null;
        }
    }
}
