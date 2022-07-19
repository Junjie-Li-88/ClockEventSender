package com.loreal.clock;

import org.springframework.context.annotation.PropertySource;

import java.text.SimpleDateFormat;
import java.util.*;

//import com.loreal.clock.Task.CallableReadCosmos;
//import sun.net.www.http.HttpClient;
//import org.slf4j.LoggerFactory;

@PropertySource(value = "classpath:application.properties", encoding = "utf-8")
public class EventToHTTPSample {
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
//        String preUuid = UUID.randomUUID().toString();
//        System.out.println(preUuid);
//        Map<String,String> map_data = new HashMap<>();
//        map_data.put("id","123123");
//        map_data.put("name","john");
//        String data ="[{" +
//                "\"method\": \"push\"," +
//                "\"type\": \"log\"," +
//                "\"level\": \"information\"," +
//                "\"sinks\": [{" +
//                "\"name\": \"eventsender_url\"," +
//                "\"id\": \"sink_pbi_01\"," +
//                "\"type\": \"power_bi\"" +
//                "}," +
//                "{" +
//                "\"name\": \"ce2-cndat-sd-pd-clksd0.log_messages\"," +
//                "\"id\": \"sqldbsink\"," +
//                "\"type\": \"sql_db\"" +
//                "}" +
//                "]," +
//                "\"key_ref\": \"eventsender\"," +
//                "\"service_ref\": \"ce2-cndat-ks-pd-clkks1\"," +
//                "\"service_type\": \"aks\"," +
//                "\"timestamp\": \""+map_data.get("end_time")+"\"," +
//                "\"records\": [{" +
//                "\"sinks\": null," +
//                "\"category\": \"aks_eventsender_category\"," +
//                "\"record_id\": \""+map_data.get("uuid")+"\"," +
//                "\"record\": [{" +
//                "\"event_type\": \""+map_data.get("event_type")+"\"," +
//                "\"start_time\": \""+map_data.get("start_time")+"\"," +
//                "\"finish_time\": \""+map_data.get("end_time")+"\"," +
//                "\"avg_duration\": "+map_data.get("avg_duration")+"," +
//                "\"sum_suceeded\": "+map_data.get("suceeded_num")+"," +
//                "\"sum_failed\": "+map_data.get("failed_num")+"," +
//                "\"log_time\": \""+map_data.get("end_time")+"\"" +
//                "}]" +
//                "}]" +
//                "}]";
        System.out.println(getGMT8Time());

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
            SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            gmt8 = sdf.format(day.getTime());

        } catch (Exception e) {
            System.out.println("获取GMT8时间 getGMT8Time() error !");
            e.printStackTrace();
            gmt8 = null;
        }
        return  gmt8;
    }
}

//eventhub: a b c
//       V
// node 1: a || a b c
// node 2: b || a b c
// node 3: c || a b c
//       V
// pekon: abc || abcabcabc