package com.loreal.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 实现HTTPS协议POST请求JSON报文
 *
 * @author gblfy
 * @date 2020-06-19
 */

public class HttpClientUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static class TrustAnyTrustManager implements X509TrustManager {
        // 该方法检查客户端的证书，若不信任该证书则抛出异常。由于我们不需要对客户端进行认证，因此我们只需要执行默认的信任管理器的这个方法。
        // JSSE中，默认的信任管理器类为TrustManager。
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        /*
         * 该方法检查服务器的证书，若不信任该证书同样抛出异常。通过自己实现该方法，可以使之信任我们指定的任何证书。
         * 在实现该方法时，也可以简单的不做任何处理， 即一个空的函数体，由于不会抛出异常，它就会信任任何证书。(non-Javadoc)
         */
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        // 返回受信任的X509证书数组。
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }

    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * post方式请求服务器(https协议)
     *
     * @param url     求地址
     * @param content 参数
     * @param charset 编码
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws IOException
     */
    public static String sendJsonToHttpsPost(String url, String content,
                                             String charset,String type) throws NoSuchAlgorithmException,
            KeyManagementException, IOException {
        Integer responseCode = -1;
        //重试10次，如果2xx就退出，非2xx就重试
        for(int i=1;i<=3;i++) {
            String error_Log="";
            String state_data="";
            if (type.equals("http")){

                URL urlObj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");

                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                try {
                    OutputStream os = connection.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                    osw.write(content);
                    osw.flush();
                    osw.close();
                    os.close();  //don't forget to close the OutputStream

                    logger.info("Send 'HTTP POST' request to : " + url);


                    responseCode = connection.getResponseCode();
                    logger.info("Response Code : " + responseCode);

                    if (responseCode >= 200 && responseCode < 300) {//  == HttpURLConnection.HTTP_OK) {
                        BufferedReader inputReader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = inputReader.readLine()) != null) {
                            response.append(inputLine);
                        }
                        inputReader.close();
                        state_data="succeeded";
                        logger.info("url :"+url +" "+response.toString());
                    } else {
                        BufferedReader inputReader = new BufferedReader(
                                new InputStreamReader(connection.getErrorStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = inputReader.readLine()) != null) {
                            response.append(inputLine);
                        }
                        inputReader.close();
                        error_Log = url + ": HTTP Code is " + responseCode + "HTTP log is "+response.toString();
                        logger.error("Something went wrong sending to " + url + ": HTTP Code is " + responseCode + "HTTP log is "+response.toString()+ " content data: "+content);
                    }
                } catch (Exception e) {
                    error_Log = url +" Something went wrong in HTTPClient: "+e.toString();
                    logger.error("url is "+ url +" Something went wrong in HTTPClient: "+e.toString() + " content data: "+content);
                }
            } else {
                /*
                 * 类HttpsURLConnection似乎并没有提供方法设置信任管理器。其实，
                 * HttpsURLConnection通过SSLSocket来建立与HTTPS的安全连接
                 * ，SSLSocket对象是由SSLSocketFactory生成的。
                 * HttpsURLConnection提供了方法setSSLSocketFactory
                 * (SSLSocketFactory)设置它使用的SSLSocketFactory对象。
                 * SSLSocketFactory通过SSLContext对象来获得，在初始化SSLContext对象时，可指定信任管理器对象。
                 */
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, new TrustManager[]{new TrustAnyTrustManager()},
                        new java.security.SecureRandom());

                URL console = new URL(url);
                HttpsURLConnection conn = (HttpsURLConnection) console.openConnection();
                conn.setSSLSocketFactory(sc.getSocketFactory());
                conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
                conn.setDoOutput(true);
                // 设置请求头
                conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                conn.connect();
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.write(content.getBytes(charset));
                // 刷新、关闭
                out.flush();
                out.close();
                responseCode = conn.getResponseCode();
                return responseCode.toString();
            }
            //写入错误日志
            if (! error_Log.equals("")) {
                EventToHTTP et = new EventToHTTP();
                Map<String, String> map_data_error = new HashMap<>();
                map_data_error.put("current_time", et.getGMT8Time());
                map_data_error.put("uuid", UUID.randomUUID().toString());
                int severity_id;
                if (i >= 3) {
                    severity_id = 10;
                } else {
                    severity_id = 5;
                }
                map_data_error.put("severity_id", String.valueOf(severity_id));
                map_data_error.put("message", "responseCode:"+responseCode+" message data:"+error_Log);
                map_data_error.put("source", "call log api");
                map_data_error.put("target", "log api "+url);
                map_data_error.put("retry_num", String.valueOf(i));
                if (state_data.equals("succeeded")){
                    map_data_error.put("state", "succeeded");
                } else {
                    map_data_error.put("state", "ignore");
                }

                String http_log = et.Error_Log_Data(map_data_error);
                logger.info("error log api " + http_log);
            }

            if (state_data.equals("succeeded")){
                break;
            }
        }
        return responseCode.toString();

    }

    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        String data = "https://ce2-cndat-as-pd-clkas1.chinacloudsites.cn/logApi: HTTP Code is 403HTTP log is ﻿<!DOCTYPE html><html><head> <title>Web App - Unavailable</title> <style type=\""
        +"text / css \"> html { height: 100%; width: 100%; } #feature { width: 960px; margin: 95px auto 0 auto; overflow: auto; } #content { font-family: \""
                +"Segoe UI \"; font-weight: normal; font-size: 22px; color: #ffffff; float: left; width: 460px; margin-top: 68px; margin-left: 0px; vertical-align: middle; } #content h1 { font-family: \""
                +"Segoe UI Light \"; color: #ffffff; font-weight: normal; font-size: 60px; line-height: 48pt; width: 800px; } p a, p a:visited, p a:active, p a:hover { color: #ffffff; } #content a.button { background: #0DBCF2; border: 1px solid #FFFFFF; color: #FFFFFF; display: inline-block; font-family: Segoe UI; font-size: 24px; line-height: 46px; margin-top: 10px; padding: 0 15px 3px; text-decoration: none; } #content a.button img { float: right; padding: 10px 0 0 15px; } #content a.button:hover { background: #1C75BC; } </style></head><body bgcolor=c\""
                +"#00abec\"> <div id= \"feature\" > < div id = \"content\" > < h1 id = \"unavailable\" > Error 403 - This web app is stopped. < /h1> <p id=\"tryAgain\">The web app you have attempted to reach is currently stopped and does not accept any requests. Please try to reload the page or visit it again soon.</p > < p id = \"toAdmin\" > If you are the web app administrator,"
                +"please find the common 403 error scenarios and resolution < a href = \"https://go.microsoft.com/fwlink/?linkid=2095007\""
                +"target = \"_blank\" > here < /a>. For further troubleshooting tools and recommendations, please visit <a href=\"https:/ / portal.azure.com / \">Azure Portal</a>.</p> </div> </div></body></html>";
       System.out.println(data.replace("\"","\\\"").replace("\\n"," "));
    }

}