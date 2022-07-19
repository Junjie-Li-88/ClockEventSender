package com.loreal.clock;

import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class StringsAndConnectors {
    @Value("${eventHub.orderPay.topic.name}")
    public static String OrderPayEventHubName;
    /*order_nopay*/
    @Value("${eventHub.orderNoPay.topic.name}")
    public static String OrderNoPayEventHubName;
    @Value("${eventHub.returnOrder.topic.name}")
    public static String returnEventHubName;
    @Value("${eventHub.item.topic.name}")
    public static String itemEventHubName;
    public static String updateStatus = "";

    public static String previousWhole = "";
    public static String prevUpdateTime = "";

    public static ArrayList<String> orderEventHubStrings = new ArrayList<>();
    public static ArrayList<String> returnEventHubStrings = new ArrayList<>();
    public static ArrayList<String> itemEventHubStrings = new ArrayList<>();
    public static ArrayList<String> cosmosEventHubStrings = new ArrayList<>();

    //gen2
    public static ArrayList<EventHubProducerAsyncClient> orderPayProducers = new ArrayList<>();
    public static ArrayList<EventHubProducerAsyncClient> orderNoPayProducers = new ArrayList<>();
    public static ArrayList<EventHubProducerAsyncClient> returnProducers = new ArrayList<>();
    public static ArrayList<EventHubProducerAsyncClient> itemProducers = new ArrayList<>();
    //cosmos (only orderPay and orderNoPay)
    public static ArrayList<EventHubProducerAsyncClient> cosmosPayProducers = new ArrayList<>();
    public static ArrayList<EventHubProducerAsyncClient> cosmosNoPayProducers = new ArrayList<>();
    public static int numtids = 0;
    public static int numoids = 0;
    public static int numerrs = 0;
    public static int numreqs = 0;
    public static int numItems = 0;
    public static Semaphore mutex = new Semaphore(1);


    public static void updateProducers() {
        if (OrderPayEventHubName == null) {
            OrderPayEventHubName = "order_pay";
        }
        if (OrderNoPayEventHubName == null) {
            OrderNoPayEventHubName = "order_nopay";
        }
        if (returnEventHubName == null) {
            returnEventHubName = "return_order";
        }
        if (itemEventHubName == null) {
            itemEventHubName = "item";
        }

        orderPayProducers.clear();
        for (String cs : orderEventHubStrings) {
            orderPayProducers.add(EventHubAsync.makeAsyncClient(cs, OrderPayEventHubName));
        }
        orderNoPayProducers.clear();
        for (String cs : orderEventHubStrings) {
            orderNoPayProducers.add(EventHubAsync.makeAsyncClient(cs, OrderNoPayEventHubName));
        }
        returnProducers.clear();
        for (String cs : returnEventHubStrings) {
            returnProducers.add(EventHubAsync.makeAsyncClient(cs, returnEventHubName));
        }
        itemProducers.clear();
        for (String cs : itemEventHubStrings) {
            itemProducers.add(EventHubAsync.makeAsyncClient(cs, itemEventHubName));
        }
        cosmosPayProducers.clear();
        for (String cs : cosmosEventHubStrings) {
            cosmosPayProducers.add(EventHubAsync.makeAsyncClient(cs, OrderPayEventHubName));
        }
        cosmosNoPayProducers.clear();
        for (String cs : cosmosEventHubStrings) {
            cosmosNoPayProducers.add(EventHubAsync.makeAsyncClient(cs, OrderNoPayEventHubName));
        }
    }

}
