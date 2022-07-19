package com.loreal.clock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.AzureAuthorityHosts;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.models.DeletedKey;
import com.azure.security.keyvault.keys.models.KeyType;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.loreal.clock.controller.ItemControllerBuChaiThreadNew;
import com.microsoft.azure.kusto.data.*;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
//import com.sun.security.sasl.ClientFactoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;


import java.io.*;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

@Component
public class Main {
    static ClientSecretCredential sp;
//    @Value("${spAppId}")
    private static  String spClientId = System.getenv("CUSTOMCONNSTR_spAppId");
//    @Value("${spClientSecret}")
    private static String spClientSecret = System.getenv("CUSTOMCONNSTR_spClientSecret");;
//    @Value("${spTenantId}")
    private static String spTenantId = System.getenv("CUSTOMCONNSTR_spTenantId");
    private static String blobcs = System.getenv("CUSTOMCONNSTR_blobcs");

    @Value("${spTenantId}")
    private String sptid;

    @Scheduled(cron = "0 */2 * ? * *")
    public void updateConnStrings() throws URISyntaxException, DataServiceException, DataClientException {
        blobcs = "DefaultEndpointsProtocol=https;AccountName=ce2cndatstopdclksto;AccountKey=Bwp0dqW1qwhQvhOt3tETvbHeFpDYw2871YCrDsFux4ViPx3G+GkOTGIJyfajCDK4MRT+JsxawI3gWpc143Nzvg==;EndpointSuffix=core.chinacloudapi.cn";
        spClientId = "d440a2e8-985b-4b6f-8175-d81de31a151f";
        spTenantId = "1338e9e4-3189-42f4-9a96-b67c3549f5c5";
        spClientSecret = "id.2ux..jGJCUl~LNDu3Lv5Q._vm42xt4L";
        sp = new ClientSecretCredentialBuilder()
                .authorityHost(AzureAuthorityHosts.AZURE_CHINA)
                .clientId(spClientId)
                .clientSecret(spClientSecret)
                .tenantId(spTenantId)
                .build();

        // Create a BlobServiceClient object which will be used to create a container client
        //String blobcs = "DefaultEndpointsProtocol=https;AccountName=ce2cndatstonpclksto;AccountKey=8t7H9sTGtBR6fXmV0E1Cq/P14MoMWacZlbxDJyOGlBir1w16hw/ML3D1iTeUXgtGvfmuYBPa6rGa7rciJl7MWg==;EndpointSuffix=core.chinacloudapi.cn";
        System.out.println("your order: " + sptid);
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(blobcs).buildClient();
        //Create a unique name for the container
        String containerName = "clockcontainer";
        // Create the container and return a container client object
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient bc = containerClient.getBlobClient("appservice/appservice-config/appservice-config.json");

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            bc.downloadStream(outputStream);
            String rawString = outputStream.toString();
            if (rawString.compareTo(StringsAndConnectors.previousWhole) == 0) {
                System.out.println("no changes detected, not doing");
                return;
            } else {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.HOUR_OF_DAY, 8);
                Date newDate = calendar.getTime();
                String newDateString = df.format(newDate);
                StringsAndConnectors.prevUpdateTime = newDateString;
                StringsAndConnectors.previousWhole = rawString;
            }
            JSONObject whole = JSONObject.parseObject(outputStream.toString());
            JSONArray cosmos = whole.getJSONArray("cosmos");
            JSONArray gen2_order = whole.getJSONArray("gen2_order");
            JSONArray gen2_item = whole.getJSONArray("gen2_item");
            JSONArray gen2_refund = whole.getJSONArray("gen2_refund");
            StringsAndConnectors.returnEventHubStrings = doit(gen2_refund);
            StringsAndConnectors.cosmosEventHubStrings = doit(cosmos);
            StringsAndConnectors.itemEventHubStrings = doit(gen2_item);
            StringsAndConnectors.orderEventHubStrings = doit(gen2_order);
            System.out.println(StringsAndConnectors.returnEventHubStrings);
            //StringsAndConnectors.initConnectors();
            StringsAndConnectors.updateProducers();
            System.out.println("producers updated successfully");
            StringsAndConnectors.updateStatus = "success";
        } catch (IOException e) {
            StringsAndConnectors.updateStatus = e.toString();
            e.printStackTrace();
        }
    }

    //takes a json array of keys and urls, returns arraylist of secrets
    public static ArrayList<String> doit(JSONArray curArray) {
        ArrayList<String> secrets = new ArrayList<>();
        for (int i = 0; i < curArray.size(); i++) {
            JSONObject curObj = curArray.getJSONObject(i);
            String keyName = curObj.getString("key");
            String url = curObj.getString("url");
            System.out.println(url);
            //url = "https://clock-databricks-kv-sit.vault.azure.cn/";
            //System.out.println(url);
            SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(url)
                    .credential(sp)
                    .buildClient();
            KeyVaultSecret retrievedSecret = secretClient.getSecret(keyName);
            String s = String.valueOf(retrievedSecret.getValue());
            secrets.add(s);
        }
        return secrets;
    }
}