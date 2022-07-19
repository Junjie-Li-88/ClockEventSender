package com.loreal.clock;

import com.azure.messaging.eventhubs.*;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.*;
import com.azure.storage.blob.*;
//import com.loreal.clock.Task.CallableReadCosmos;

import com.loreal.clock.Task.CallableSendHTTP;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
//import sun.net.www.http.HttpClient;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

@PropertySource(value = "classpath:application.properties", encoding = "utf-8")
public class EventToHTTP {
//    private static final Logger logger = LoggerFactory.getLogger(com.loreal.clock.controller.OrderControllerBuChaiThreadNew.class);

    private static final Logger logger = LoggerFactory.getLogger(EventToHTTP.class);



    String path = "/mnt/clockevent/config/event.properties";

    public static int numEventsProcessed = 0;
    public static int numPekonSuccess = 0;
    public static int numPekonError = 0;
    public static int numGemSuccess = 0;
    public static int numGemError = 0;

    private static int prefetchCount = 8000;
    private static int batch_size = 20;
    private static int thread_num = 512;
    private static int maxWaitTime = 60;


    // sit
//    private static String connectionString = "Endpoint=sb://ce2-cndat-asp-np-clken1.servicebus.chinacloudapi.cn/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=PKb/dH9UUxUk/MENOOwYTDngaYMpQN9rtyNzz8WWRlw=";;
//    private static String logApiUrl = "https://ce2-cndat-as-np-clkas0.chinacloudsites.cn/logApi";
//    private static String cg = "poc1"; //System.getenv("cg");
//    private static String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=clocknonprdstorage;AccountKey=JntJebPPddQz19J8YL+48O3H/Ri60vgmeJLPf+bSuz51RIZX7hps5EhKxjVFJfKyQPcoqn0qUbZjYNpgtxzKfA==;EndpointSuffix=core.chinacloudapi.cn"; //System.getenv("storageConnectionString");
//    private static String storageContainerName =  "clockcontainer";//System.getenv("storageContainerName");
//    private static String gemToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE2NTE3MTgzNDQsImV4cCI6MTY1NDY1NTk0NH0.a0pQFP0-3hYG2SV-eaJmd8KL3-xUNaaVBnYAqMq_1CM";
//    private static String pekonUrl = "http://52.131.220.24:3001"; //System.getenv("pekonUrl");
//    private static String gemUrl = "http://52.131.220.24:3001"; //System.getenv("gemUrl");
//    private static String sqldb_name = "ce2-cndat-sd-np-clksd0";


    // prod
    private static String connectionString = PropertiesUtil.cosProperties.getProperty("connectionString");
    public static String logApiUrl = PropertiesUtil.cosProperties.getProperty("logApiUrl");
    private static String cg = PropertiesUtil.cosProperties.getProperty("cg"); //System.getenv("cg");
    private static String storageConnectionString = PropertiesUtil.cosProperties.getProperty("storageConnectionString");
    private static String storageContainerName =  PropertiesUtil.cosProperties.getProperty("storageContainerName");//System.getenv("storageContainerName");
    private static String gemToken = PropertiesUtil.cosProperties.getProperty("gemToken");
    private static String pekonUrl = PropertiesUtil.cosProperties.getProperty("pekonUrl"); //System.getenv("pekonUrl");
    private static String gemUrl = PropertiesUtil.cosProperties.getProperty("gemUrl"); //System.getenv("gemUrl");
    private static String sqldb_name = PropertiesUtil.cosProperties.getProperty("sqldb_name");


    //uat
//    private static String gemUrl = "https://gem-api-uat.platform-loreal.cn/cn/sell_out/tmall/v1/order_event"; //System.getenv("gemUrl");
    //    nonprd
//    private static String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=clocknonprdstorage;AccountKey=JntJebPPddQz19J8YL+48O3H/Ri60vgmeJLPf+bSuz51RIZX7hps5EhKxjVFJfKyQPcoqn0qUbZjYNpgtxzKfA==;EndpointSuffix=core.chinacloudapi.cn";
//    private static final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=clocknonprdstorage;AccountKey=JntJebPPddQz19J8YL+48O3H/Ri60vgmeJLPf+bSuz51RIZX7hps5EhKxjVFJfKyQPcoqn0qUbZjYNpgtxzKfA==;EndpointSuffix=core.chinacloudapi.cn";
//  prod
//    private static final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=ce2cndatstopdclksto;AccountKey=Bwp0dqW1qwhQvhOt3tETvbHeFpDYw2871YCrDsFux4ViPx3G+GkOTGIJyfajCDK4MRT+JsxawI3gWpc143Nzvg==;EndpointSuffix=core.chinacloudapi.cn";
//    private static final String storageContainerName = "clockcontainer";

//        private static final String log_api = "https://ce2-cndat-as-pd-clkas1.chinacloudsites.cn/logApi";
    //private static final String log_api = "http://52.131.220.24:3000";



    private static ExecutorService pool = Executors.newFixedThreadPool(thread_num);
    public static void main(String[] args) throws Exception {
        //System.
        // Create a blob container client that you use later to build an event processor client to receive and process events
        BlobContainerAsyncClient blobContainerAsyncClient = new BlobContainerClientBuilder()
                .connectionString(storageConnectionString)
                .containerName(storageContainerName)
                .buildAsyncClient();

//        if (connectionString == null) {
            //sit
//            connectionString = "Endpoint=sb://ce2-cndat-asp-np-clken1.servicebus.chinacloudapi.cn/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=PKb/dH9UUxUk/MENOOwYTDngaYMpQN9rtyNzz8WWRlw=";
            //prod
//            connectionString = "Endpoint=sb://ce2-cndat-ehn-pd-clken8.servicebus.chinacloudapi.cn/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=Q5HyBekPgnElDTVL7i7sc0oqo0TcfX9SYgL/MxI7ZjI=";
            //test
//            connectionString = "Endpoint=sb://ce2-cndat-ehn-np-clken9.servicebus.chinacloudapi.cn/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=RqQxhSJpCFYZj6wyiTB4sWQphfmVzyHNy+DWb2pRIT8=";
//        }

        // Create a builder object that you will use later to build an event processor client to receive and process events and errors.
        EventProcessorClientBuilder eventProcessorClientBuilder = new EventProcessorClientBuilder()
                .connectionString(connectionString, "original_order_creation")
                .consumerGroup(cg)
                .prefetchCount(prefetchCount)
                .processEventBatch(PARTITION_PROCESSOR, batch_size, Duration.ofSeconds(maxWaitTime))
//                .processEvent(PARTITION_PROCESSOR)
                .processError(ERROR_HANDLER)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient));

        EventProcessorClientBuilder eventProcessorClientBuilderUpdate = new EventProcessorClientBuilder()
                .connectionString(connectionString, "original_order_updated")
                .consumerGroup(cg)
                .prefetchCount(prefetchCount)
                .processEventBatch(PARTITION_PROCESSOR, batch_size, Duration.ofSeconds(maxWaitTime))
//                .processEvent(PARTITION_PROCESSOR)
                .processError(ERROR_HANDLER)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient));

        EventProcessorClientBuilder eventProcessorClientBuilderReturn = new EventProcessorClientBuilder()
                .connectionString(connectionString, "return_order_creation")
                .consumerGroup(cg)
                .prefetchCount(prefetchCount)
                .processEventBatch(PARTITION_PROCESSOR, batch_size, Duration.ofSeconds(maxWaitTime))
//                .processEvent(PARTITION_PROCESSOR)
                .processError(ERROR_HANDLER)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient));

        EventProcessorClientBuilder eventProcessorClientBuilderReturnUpdate = new EventProcessorClientBuilder()
                .connectionString(connectionString, "return_order_updated")
                .consumerGroup(cg)
                .prefetchCount(prefetchCount)
                .processEventBatch(PARTITION_PROCESSOR, batch_size, Duration.ofSeconds(maxWaitTime))
                .processError(ERROR_HANDLER)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient));

        EventProcessorClientBuilder eventpresaleDepositRefundOrderCreation = new EventProcessorClientBuilder()
                .connectionString(connectionString, "presale_deposit_refund_order_creation")
                .consumerGroup(cg)
                .prefetchCount(prefetchCount)
                .processEventBatch(PARTITION_PROCESSOR, batch_size, Duration.ofSeconds(maxWaitTime))
                .processError(ERROR_HANDLER)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient));

        EventProcessorClientBuilder eventpresaleOrderCreation = new EventProcessorClientBuilder()
                .connectionString(connectionString, "presale_order_creation")
                .consumerGroup(cg)
                .prefetchCount(prefetchCount)
                .processEventBatch(PARTITION_PROCESSOR, batch_size, Duration.ofSeconds(maxWaitTime))
                .processError(ERROR_HANDLER)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient));

        EventProcessorClientBuilder eventpresaleOrderUpdated = new EventProcessorClientBuilder()
                .connectionString(connectionString, "presale_order_updated")
                .consumerGroup(cg)
                .prefetchCount(prefetchCount)
                .processEventBatch(PARTITION_PROCESSOR, batch_size, Duration.ofSeconds(maxWaitTime))
                .processError(ERROR_HANDLER)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient));

        // Use the builder object to create an event processor client
        EventProcessorClient eventProcessorClient = eventProcessorClientBuilder.buildEventProcessorClient();
        EventProcessorClient a = eventProcessorClientBuilderReturnUpdate.buildEventProcessorClient();
        EventProcessorClient b = eventProcessorClientBuilderReturn.buildEventProcessorClient();
        EventProcessorClient c = eventProcessorClientBuilderUpdate.buildEventProcessorClient();
        EventProcessorClient d = eventpresaleDepositRefundOrderCreation.buildEventProcessorClient();
        EventProcessorClient e = eventpresaleOrderCreation.buildEventProcessorClient();
        EventProcessorClient f = eventpresaleOrderUpdated.buildEventProcessorClient();

        logger.info("Starting event processor");
        a.start();
        b.start();
        c.start();
        d.start();
        e.start();
        f.start();
        eventProcessorClient.start();

        logger.info("Press enter to stop.");
        System.in.read();

        logger.info("Stopping event processor");

//        a.stop();
//        b.stop();
//        c.stop();
//        d.stop();
//        e.stop();
//        f.stop();
//        eventProcessorClient.stop();
        logger.info("Event processor stopped.");

        logger.info("Exiting process");
    }

    public static final Consumer<EventBatchContext> PARTITION_PROCESSOR = eventBatchContext -> {
        String Error_Log = "";
        logger.info("====== "+eventBatchContext.getPartitionContext().getEventHubName()+" size: "+eventBatchContext.getEvents().size());

        List<EventData> event_list = eventBatchContext.getEvents();
        String[] array_sz = null;
        array_sz = new String[event_list.size()];
        String[] finalArray_list = array_sz;

        if (event_list.size()>0){
            for (int i=0;i<event_list.size();i++){
                finalArray_list[i]=event_list.get(i).getBodyAsString();
            }
            String data_comment = "["+StringUtils.join(finalArray_list, ",")+"]";
            //        PartitionContext partitionContext = eventBatchContext.getPartitionContext();
            //        EventData eventData = eventContext.getEventData();

            //System.out.printf("Processing event from partition %s with sequence number %d with body: %s%n",
            //        partitionContext.getPartitionId(), eventData.getSequenceNumber(), eventData.getBodyAsString());
            //        String body = eventData.getBodyAsString();
            //        System.out.println(body);
            try {
                //                Future gemFuture = pool.submit(gem);
//                    int finalK = k;


                        //异步提交
//                        if (gemToken == null) {
//                            logger.error("gemToken is null!!");
//                            gemToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE2NTQ3NDU1MzQsImV4cCI6MTY1NzY4MzEzNH0.XvToi00lCT9kkpNXnf7DPQuB1bu6YEXUBBAMmgygYWc";
//                        }

                        HashMap<String, String> extraHeaders = new HashMap<String, String>();

//                        numGemSuccess++;

                        Map<String,String> map_data_pekon = new HashMap<>();
                        map_data_pekon.put("target_url","pekon");
                        map_data_pekon.put("data_num", String.valueOf(event_list.size()));
                        map_data_pekon.put("event_type", eventBatchContext.getPartitionContext().getEventHubName());
                        Callable pekon = new CallableSendHTTP(pekonUrl, data_comment, extraHeaders, map_data_pekon);
                        Future<String> pekonFuture = pool.submit(pekon);
                        logger.info("==pekon=== "+pekonFuture.get());

                        Map<String,String> map_data_gem = new HashMap<>();
                        map_data_gem.put("target_url","gem");
                        map_data_gem.put("data_num", String.valueOf(event_list.size()));
                        map_data_gem.put("event_type", eventBatchContext.getPartitionContext().getEventHubName());
                        extraHeaders.put("Authorization", gemToken);
                        Callable gem = new CallableSendHTTP(gemUrl, data_comment, extraHeaders, map_data_gem);
                        Future<String> gemFuture = pool.submit(gem);
                        logger.info("===gem=== "+gemFuture.get());


                if (numEventsProcessed % 200 == 0) {
//                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//                    Date log_time = sdf.parse(String.valueOf(getGMT8Time()));
//                    String newDateString = df.format(getGMT8Time());

                    logger.info(getGMT8Time() + " " + " pekonSuccess:"+numPekonSuccess+ " pekonError:" + numPekonError +
                            " gemSuccess:" + numGemSuccess + " gemError:" + numGemError);
                }

            } catch (Exception e) {
                Error_Log = e.toString();
                logger.error(e.toString());
            }
            //写入错误日志
            if (! Error_Log.equals("")) {
                EventToHTTP et = new EventToHTTP();
                Map<String, String> map_data_error = new HashMap<>();
                map_data_error.put("current_time", et.getGMT8Time());
                map_data_error.put("uuid", UUID.randomUUID().toString());
                map_data_error.put("severity_id", String.valueOf(10));
                map_data_error.put("message", Error_Log);
                map_data_error.put("source", "call EventHub");
                map_data_error.put("target", "");
                map_data_error.put("retry_num", String.valueOf(1));
                map_data_error.put("state", "ignore");
                String http_log = et.Error_Log_Data(map_data_error);
                logger.info("error log api " + http_log);
            }
            eventBatchContext.updateCheckpoint();
//        System.out.println(new String(result_data));
            // Every 10 events received, it will update the checkpoint stored in Azure Blob Storage.
//        if (eventData.getSequenceNumber() % 20 == 0) {
//            eventContext.updateCheckpoint();
//        }
        }


    };
    //HttpClient httpclient = HttpClient.New("www.baidu.com");

    public static final Consumer<ErrorContext> ERROR_HANDLER = errorContext -> {
        logger.error("Error occurred in partition processor for partition %s, %s.%n",
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable());
    };

    public static String Log_Data(Map<String, String> map_data){
        String data ="[{" +
            "\"method\": \"push\"," +
                    "\"type\": \"log\"," +
                    "\"level\": \"information\"," +
                    "\"sinks\": [{" +
                    "\"name\": \"eventsender_url\"," +
                    "\"id\": \"sink_pbi_01\"," +
                    "\"type\": \"power_bi\"" +
                    "}," +
                    "{" +
                    "\"name\": \""+sqldb_name+".log_messages\"," +
                    "\"id\": \"sqldbsink\"," +
                    "\"type\": \"sql_db\"" +
                    "}" +
                    "]," +
                    "\"key_ref\": \"eventsender\"," +
                    "\"service_ref\": \"ce2-cndat-ks-pd-clkks1\"," +
                    "\"service_type\": \"aks\"," +
                    "\"timestamp\": \""+map_data.get("end_time")+"\"," +
                    "\"records\": [{" +
                    "\"sinks\": null," +
                    "\"category\": \"aks_eventsender_category\"," +
                    "\"record_id\": \""+map_data.get("uuid")+"\"," +
                    "\"record\": {" +
                    "\"api_name\": \""+map_data.get("api_name")+"\"," +
                    "\"event_type\": \""+map_data.get("event_type")+"\"," +
                    "\"start_time\": \""+map_data.get("start_time")+"\"," +
                    "\"finish_time\": \""+map_data.get("end_time")+"\"," +
                    "\"avg_duration\": "+map_data.get("avg_duration")+"," +
                    "\"sum_suceeded\": "+map_data.get("suceeded_num")+"," +
                    "\"sum_failed\": "+map_data.get("failed_num")+"," +
                    "\"log_time\": \""+map_data.get("end_time")+"\"" +
                    "}" +
                    "}]" +
                    "}]";
        return data;
    }

    public static String getGMT8Time(){
        String gmt8 = null;
        try {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"), Locale.CHINESE);
            Calendar day = Calendar.getInstance();
            day.set(Calendar.YEAR, cal.get(Calendar.YEAR));
            day.set(Calendar.MONTH, cal.get(Calendar.MONTH));
            day.set(Calendar.DATE, cal.get(Calendar.DATE));
            day.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
            day.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
            day.set(Calendar.SECOND, cal.get(Calendar.SECOND));
            day.set(Calendar.MILLISECOND, cal.get(Calendar.MILLISECOND));
            SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            gmt8 = sdf.format(day.getTime());

        } catch (Exception e) {
            System.out.println("获取GMT8时间 getGMT8Time() error !");
            e.printStackTrace();
            gmt8 = null;
        }
        return  gmt8;
    }

    public static String Error_Log_Data(Map<String, String> map_data){
        String data ="[" +
                " {" +
                " \"method\": \"push\"," +
                " \"type\": \"log\"," +
                " \"level\": \"Error\"," +
                " \"sinks\": [" +
                " {" +
                " \"id\": \"sink_sql_01\"," +
                " \"type\": \"sql_db\"," +
                " \"name\": \""+sqldb_name+".log_messages\"" +
                " }," +
                " {" +
                " \"id\": \"sink_adx_01\"," +
                " \"type\": \"adx\"," +
                " \"name\": \"eventsender_error_log\"" +
                " }" +
                " ]," +
                " \"key_ref\": \"eventsender\"," +
                " \"service_ref\": \"ce2-cndat-ks-pd-clkks1\"," +
                " \"service_type\": \"aks\"," +
                " \"timestamp\": \""+map_data.get("current_time")+"\"," +
                " \"records\": [" +
                " {" +
                " \"sinks\": null," +
                " \"record_id\": \""+map_data.get("uuid")+"\"," +
                " \"category\": \"eventsender_error_log\"," +
                " \"record\": {" +
                " \"severity\": "+map_data.get("severity_id")+"," +
                " \"message\": \""+map_data.get("message").replace("\"","\\\"").replace("\\n","")+"\"," +
                " \"source\": \""+map_data.get("source")+"\"," +
                " \"target\": \""+map_data.get("target")+"\"," +
                " \"retry\": "+map_data.get("retry_num")+"," +
                " \"state\": \""+map_data.get("state")+"\"," +
                " \"context\": {}" +
                " }" +
                " }" +
                " ]" +
                " }" +
                "]";
        String result_data = null;
        HttpClientUtil http = new HttpClientUtil();
        try {
            result_data = http.sendJsonToHttpsPost(logApiUrl, data, "utf-8","http");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("error log api: "+result_data + " json data:"+data);
        return result_data;
    }
}