package com.loreal.clock.controller;

import com.alibaba.fastjson.JSONObject;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import com.loreal.clock.*;
import lombok.NoArgsConstructor;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.loreal.clock.StringsAndConnectors.*;

@RestController //该注解是 @Controller 和 @ResponseBody 注解的合体版
@NoArgsConstructor
public class OrderControllerBuChaiThreadNew {
    private static final Logger logger = LoggerFactory.getLogger(OrderControllerBuChaiThreadNew.class);
    private String curTid = "";
    @Value("${recThreshold}")
    private int recThreshold;
    //单个线程处理的数据量
    private static Random rand = new Random();

    @Value("${logApiUrl}")
    private String logApiUrl;
    @Value("${thread.data.num}")
    private int singleCount;
    // 根据tid写到几个Eve@Value("${thread.data.num}")
    //    private int singleCount;ntHub中
    @Value("${eventhub.thread.num}")
    private int eventhubNum;
    //处理的总数据量
    private int listSize;
    private boolean isPay;
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

    //@Value("#{'${eventHub.client.connectionString}'.split(',')}")
//    @Value("#{'${appServiceConnStrGen2}'.split('\\$\\$\\$')}")
//    private List<String> gen2ConnectionStrings;

//    @Value("#{'${appServiceConnStrCosmos}'.split('\\$\\$\\$')}")
//    private List<String> cosmosConnectionStrings;
    /*order_pay*/
    @Value("${eventHub.orderPay.topic.name}")
    private String OrderPayEventHubName;
    /*order_nopay*/
    @Value("${eventHub.orderNoPay.topic.name}")
    private String OrderNoPayEventHubName;
    @Value("${splitEventHubs}")
    private int splitEventHubs;
    private String errorMessage = "";
    public final static String path = "/handleOrderBuChaiThreadMultiple";
    public static List<EventHubProducerClient> producerOne;
    public static List<EventHubProducerClient> producerTwo;

    @RequestMapping(path="/handleOrderBuChaiThreadMultiple", produces = "application/json;charset=utf-8")
    public String handleOrder(@RequestBody String parameters, HttpServletResponse response) throws IOException {
        //if ()
//        System.out.println(parameters);
        List<String> list = new ArrayList<String>();
        List<String> nopaylist = new ArrayList<String>();
        List<JSONObject> list_json = new ArrayList<JSONObject>();
        List<EventHubProducerClient> curProducer = null;
//        EventHubProducerClient curProducer2 = null;
        List<EventHubProducerAsyncClient> curProducerAsync = null;
        List<EventHubProducerAsyncClient> cosmosCurProducerAsync = null;
//        EventHubProducerAsyncClient curProducerAsync2[] = null;
        String bigString = "";
        JSONArray paramArray = null;
        try {
            paramArray = JSONArray.fromObject(parameters);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return ErrorMessageJson.newMessage("Body does not contain a valid js array", 400, "/OrderController");
        }
        for (int i = 0; i < paramArray.size(); i++) {
            net.sf.json.JSONObject obj = paramArray.getJSONObject(i);
            SerializeReturnObject serialized = JsonParameters(obj);
            if (serialized == null && (StringsAndConnectors.cosmosEventHubStrings.size() != 0 || StringsAndConnectors.cosmosEventHubStrings.size() != 0)) {
                JSONObject errmsg = new JSONObject();
                errmsg.put("code", "ERROR");
                errmsg.put("message", errorMessage + obj.toString());
                try {
                    net.sf.json.JSONObject rawObj = paramArray.getJSONObject(i);
                    String tid = rawObj.getJSONObject("trade_fullinfo_get_response").getJSONObject("trade").getString("tid");
                    errmsg.put("tid", tid);
                } catch (Exception e){
                    System.out.println("Not able to retrieve tid:" + e);
                }
                list_json.add(errmsg);
//                HttpClient.makePostRequest(logApiUrl, LogApiHelper.makeErrorJson(errmsg).toString(), null);
                continue;
            }
            boolean payTimeIsNull = serialized.getIsNull();
            JSONObject processedJson = serialized.getRet();
            if (!payTimeIsNull) {
                list.add(serialized.getRet().toString());
            } else {
                nopaylist.add(serialized.getRet().toString());
            }
        }

//        System.out.println("lennopay is" + nopaylist.size());
//        System.out.println("paylen is " + list.size());
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int validEvents = list.size() + nopaylist.size();
        int invalidEvents = paramArray.size() - validEvents;
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
            com.alibaba.fastjson.JSONArray statJson = LogApiHelper.makeStatJson(fields, fieldsPbi);
            HttpClient.makePostRequest(logApiUrl, statJson.toString(), null, null);
            numerrs = 0;
            numtids = 0;
            numreqs = 0;
            numItems = 0;
        }

        OrderControllerBuChaiThreadNew tool = new OrderControllerBuChaiThreadNew(singleCount,list,true);
        OrderControllerBuChaiThreadNew nopaytool = new OrderControllerBuChaiThreadNew(singleCount,nopaylist,false);

        CallBack myCb = new CallBack<String>() {
            @Override
            public void method(List<String> curList) {

                List<EventHubProducerAsyncClient> curGen2Producer, curCosmosProducer;
                System.out.println("isPay is: " + isPay + " with len " + list.size());
                    curCosmosProducer = StringsAndConnectors.cosmosPayProducers;
                    curGen2Producer = StringsAndConnectors.orderPayProducers;
                //List<String> newList = list.();
                for (int i = 0; i < curList.size(); i++) {
                    if (Thread.currentThread().getId() % 100 == 0) {
                        logger.info("Thread:" + Thread.currentThread().getId() + " === " + i + "/" + list.size() + " ");
                    }
//                    System.out.println("list.get(i) is: " + list.get(i));
                    JSONObject json = JSONObject.parseObject(curList.get(i));
                    JSONObject trade = json.getJSONObject("trade_fullinfo_get_response");
                    JSONObject tradeJson = trade.getJSONObject("trade");

                    String tid = tradeJson.get("tid").toString();
                    //int tid_sj = Integer.parseInt(tid.substring(tid.length()-2))%curGen2Producer.size();
                    int cosmos_tid_sj = 0;
                    int tid_sj = 0;
                    if (curGen2Producer.size() > 0) {
                        tid_sj = Math.abs(rand.nextInt()) % curGen2Producer.size();
                    }
//                    System.out.println(tid_sj + " " + curGen2Producer.size());
                    //int cosmos_tid_sj = Integer.parseInt(tid.substring(tid.length()-2))%curCosmosProducer.size();
                    if (curCosmosProducer.size() > 0) {
                        cosmos_tid_sj = Math.abs(rand.nextInt()) % curCosmosProducer.size();
                    }
//                    System.out.println(cosmos_tid_sj + " " + curCosmosProducer.size());


                    JSONObject event_json = null;

                    int sendCount = 0;
                    if (curGen2Producer.size() > 0) {
//                        System.out.println("PAY SENDING TO: " + curGen2Producer.get(tid_sj).getEventHubName() + " " + curGen2Producer.get(tid_sj).getFullyQualifiedNamespace() + " " + tid);
                        event_json = EventHubAsync.sendEvent(curGen2Producer.get(tid_sj), json.toString(),tid_sj,tid, "tid");
                        sendCount += 2;
                    }
                    JSONObject event_json_cosmos = null;
                    if (curCosmosProducer.size() > 0) {
//                        System.out.println("PAY SENDING TO: " + curCosmosProducer.get(cosmos_tid_sj).getEventHubName() + " " + curCosmosProducer.get(cosmos_tid_sj).getFullyQualifiedNamespace() + " " + tid);
                        event_json_cosmos = EventHubAsync.sendEvent(curCosmosProducer.get(cosmos_tid_sj), json.toString(), tid_sj, tid, "tid");
                        sendCount += 3;
                    }

                    if (sendCount >= 5) {
                        if (event_json.getString("code").compareTo("ERROR") != 0) {
                            if (event_json_cosmos.getString("code").compareTo("ERROR") != 0) {
                                list_json.add(event_json);
                            } else {
                                list_json.add(event_json_cosmos);
                            }
                        } else {
                            list_json.add(event_json);
                        }
                    } else {
                        if (sendCount == 2) {
                            list_json.add(event_json);
                        } else if (sendCount == 3) {
                            list_json.add(event_json_cosmos);
                        } else if (sendCount == 0) {
                            JSONObject errmsg = new JSONObject();
                            errmsg.put("code", "ERROR");
                            errmsg.put("msg", "There are no eventhubs configured at the moment");
                            errmsg.put("tid", tid);
                            list_json.add(errmsg);
                            try {
//                                HttpClient.makePostRequest(logApiUrl, LogApiHelper.makeErrorJson(errmsg).toString(), null);
                            } catch (Exception e) {
                                System.out.println(e);
                            }
                        }
                    }
                }
            }
        };

        CallBack myCbNoPay = new CallBack<String>() {
            @Override
            public void method(List<String> curList) {

                List<EventHubProducerAsyncClient> curGen2Producer, curCosmosProducer;
                System.out.println("isPay is: " + isPay + " with len " + curList.size());
                curCosmosProducer = StringsAndConnectors.cosmosNoPayProducers;
                curGen2Producer = StringsAndConnectors.orderNoPayProducers;
                //List<String> newList = list.();
                for (int i = 0; i < curList.size(); i++) {
                    if (Thread.currentThread().getId() % 100 == 0) {
                        logger.info("Thread:" + Thread.currentThread().getId() + " === " + i + "/" + list.size() + " ");
                    }
//                    System.out.println("list.get(i) is: " + list.get(i));
                    JSONObject json = JSONObject.parseObject(curList.get(i));
                    JSONObject trade = json.getJSONObject("trade_fullinfo_get_response");
                    JSONObject tradeJson = trade.getJSONObject("trade");

                    String tid = tradeJson.get("tid").toString();
                    //int tid_sj = Integer.parseInt(tid.substring(tid.length()-2))%curGen2Producer.size();
                    int cosmos_tid_sj = 0;
                    int tid_sj = 0;
                    if (curGen2Producer.size() > 0) {
                        tid_sj = Math.abs(rand.nextInt()) % curGen2Producer.size();
                    }
//                    System.out.println(tid_sj + " " + curGen2Producer.size());
                    //int cosmos_tid_sj = Integer.parseInt(tid.substring(tid.length()-2))%curCosmosProducer.size();
                    if (curCosmosProducer.size() > 0) {
                        cosmos_tid_sj = Math.abs(rand.nextInt()) % curCosmosProducer.size();
                    }
//                    System.out.println(cosmos_tid_sj + " " + curCosmosProducer.size());

//                    System.out.println(list.get(i));
//                    System.out.println("SENDING TO: " + curGen2Producer.get(tid_sj).getEventHubName() + " " + curGen2Producer.get(tid_sj).getFullyQualifiedNamespace());// + curGen2Producer.get(tid_sj).getFullyQualifiedNamespace());
                    JSONObject event_json = null;

                    int sendCount = 0;
                    if (curGen2Producer.size() > 0) {
//                        System.out.println("NOPAY SENDING TO: " + curGen2Producer.get(tid_sj).getEventHubName() + " " + curGen2Producer.get(tid_sj).getFullyQualifiedNamespace() + " " + tid);
                        event_json = EventHubAsync.sendEvent(curGen2Producer.get(tid_sj), json.toString(),tid_sj,tid, "tid");
                        sendCount += 2;
                    }
                    JSONObject event_json_cosmos = null;
                    if (curCosmosProducer.size() > 0) {
//                        System.out.println("NOPAY SENDING TO: " + curCosmosProducer.get(cosmos_tid_sj).getEventHubName() + " " + curCosmosProducer.get(cosmos_tid_sj).getFullyQualifiedNamespace() + " " + tid);
                        event_json_cosmos = EventHubAsync.sendEvent(curCosmosProducer.get(cosmos_tid_sj), json.toString(), tid_sj, tid, "tid");
                        sendCount += 3;
                    }

                    if (sendCount >= 5) {
                        if (event_json.getString("code").compareTo("ERROR") != 0) {
                            if (event_json_cosmos.getString("code").compareTo("ERROR") != 0) {
                                list_json.add(event_json);
                            } else {
                                list_json.add(event_json_cosmos);
                            }
                        } else {
                            list_json.add(event_json);
                        }
                    } else {
                        if (sendCount == 2) {
                            list_json.add(event_json);
                        } else if (sendCount == 3) {
                            list_json.add(event_json_cosmos);
                        } else if (sendCount == 0) {
                            JSONObject errmsg = new JSONObject();
                            errmsg.put("code", "ERROR");
                            errmsg.put("msg", "There are no eventhubs configured at the moment");
                            errmsg.put("tid", tid);
                            list_json.add(errmsg);
                            try {
//                                HttpClient.makePostRequest(logApiUrl, LogApiHelper.makeErrorJson(errmsg).toString(), null);
                            } catch (Exception e) {
                                System.out.println(e);
                            }
                        }
                    }
                }
            }
        };
        tool.setIsPay(true);
        tool.setCallBack(myCb);
        nopaytool.setIsPay(false);
        nopaytool.setCallBack(myCbNoPay);
        try {
            this.isPay = true;
            tool.setIsPay(true);
            tool.excute();
            this.isPay = false;
            nopaytool.setIsPay(false);
            nopaytool.excute();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        response.setStatus(HttpServletResponse.SC_OK);
        return list_json.toString();
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public OrderControllerBuChaiThreadNew(int singleCount, List<String> list, boolean isPay){
        this.singleCount = singleCount;
        this.list = list;
        this.isPay = isPay;
        if (list != null){
            this.listSize = list.size();
            this.runSize = (this.listSize/this.singleCount) + 1;
        }
    }

    public void setList(List s) {
        this.list = s;
    }

    public void setIsPay(boolean isPay) {
        this.isPay = isPay;
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
//                System.out.println("list.size() is " + list.size() + " and listSize is " + listSize);
                endIndex = listSize;
                newList = list.subList(startIndex,endIndex);
            }
            //建立线程类处理数据
            MyThread myThread = new MyThread(newList, begin, end) {
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

    public SerializeReturnObject JsonParameters(net.sf.json.JSONObject parameters) {
//        logger.info("-------请求参数--------" + parameters);
        if (parameters == null) {
            logger.info("parameters is null,ClassName is:" + this.getClass().getName());
            return null;
        }
        net.sf.json.JSONObject json = net.sf.json.JSONObject.fromObject(parameters);
        net.sf.json.JSONObject trade;
        net.sf.json.JSONObject tradeJson;
        JSONObject time = new JSONObject();
        JSONObject response = new JSONObject();
        boolean payTimeIsNull = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String format = dateFormat.format(new Date());
            try {
//                net.sf.json.JSONObject trade1 = json.getJSONObject(i);
                trade = json.getJSONObject("trade_fullinfo_get_response");
//                trade = new JSONObject();
            } catch (Exception e) {
                errorMessage = "error parsing trade_fullinfo_get_response.trade";
                return null;
            }
            if (trade.isNullObject()) {
                errorMessage = "order 数据：trade_fullinfo_get_response is null,ClassName is:" + this.getClass().getName();
                logger.info(errorMessage);
                return null;
            }
            tradeJson = trade.getJSONObject("trade");
            if (tradeJson.isNullObject()) {
                logger.info(" order 数据：trade is null,ClassName is:" + this.getClass().getName());
                return null;
            }
            curTid = tradeJson.getString("tid");

            net.sf.json.JSONArray orders = tradeJson.getJSONObject("orders").getJSONArray("order");
            Iterator<Object> orderIterator = orders.iterator();
            while (orderIterator.hasNext()) {
                net.sf.json.JSONObject next = (net.sf.json.JSONObject) orderIterator.next();
                next.put("guid", UUID.randomUUID().toString());
            }

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        Date newDate = calendar.getTime();
        String newDateString = df.format(newDate);
//            logger.info("utc格式的时间：" + newDateString);
        /*新增nanXun和ali字段*/
        if (tradeJson.has("jdp_modified")) {
//            tradeJson.put("ali_send_timestamp", tradeJson.get("jdp_created"));
            tradeJson.put("ali_send_timestamp", tradeJson.get("jdp_modified"));
        }
        if (tradeJson.has("request_time")) {
            tradeJson.put("nanxun_send_timestamp", tradeJson.get("request_time"));
        }

            //eng_jdp_created and set as ali_send_timestamp
            //eng_request_time_from_isv as nanxun_send_timestamp

            /*新增字段*/
//            if () {
//                pay_time = tradeJson.getString("pay_time");
            payTimeIsNull = !tradeJson.has("pay_time");
            tradeJson.put("apps_send_timestamp", newDateString);
            tradeJson.put("apps_receive_timestamp", newDateString);
            /*外层塞值*/
            time.put("trade", tradeJson);
            response.put("trade_fullinfo_get_response", time);
//            System.out.println(response);
            return new SerializeReturnObject(response, payTimeIsNull);
        }
}