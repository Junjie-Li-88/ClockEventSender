package com.loreal.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class PropertiesUtil {
    public static void main(String args[]) {
        System.out.println(PropertiesUtil.cosProperties.getProperty("storageConnectionString"));
    }

    private static Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
    public static Properties cosProperties;
    static {
//        //PropertiesUtil.cosProperties.getProperty("maxItemCount");

        String localPath = "C:\\Users\\junjie.li2\\Desktop\\temp";//"C:\\Users\\jason.lin\\Downloads\\clock\\clockjavaapi\\target\\";
        //String url =(System.getenv("config")==null?"/mnt/clockevent/config/event.properties":System.getenv("config")) + File.separator+"event.config";
        String url =(System.getenv("config")==null?localPath:System.getenv("config")) + File.separator+"event.config";
////        url = "/mnt/clockevent/config/event.properties";
        // deleteme when deploying
//        url = "C:\\Users\\jason.lin\\Downloads\\clock\\clockjavaapi\\target\\props.txt";
//        logger.info("PropertiesUtil url:{}",url);
        Properties p = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(url));
            p.load(in);
        } catch (IOException e) {
            logger.error("PropertiesUtil IOException:",e);
        }
        cosProperties=p;
    }
}
