package com.loreal.clock.controller;

import com.alibaba.fastjson.JSONObject;
import com.loreal.clock.StringsAndConnectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
public class DebuggingController {
    @Value("${eventhub.thread.num}")
    private int eventhubNum;
    @Value("${eventhub.thread.num}")
    private String asdasd;
    @RequestMapping("/debug")
    public String handleItem(@RequestBody String parameters, HttpServletResponse response) {
        JSONObject obj = JSONObject.parseObject(parameters);
        String s = obj.getString("envvar");
        String pswd = obj.getString("pswd");
        if (pswd == null || pswd.compareTo("haveyouseenmyunbornchild1") != 0) {
            return "pswd not ok";
        }
        String ret = "last update time: " + StringsAndConnectors.prevUpdateTime + "\n";
        ret += "update status: " + 1 + "\n";
        ret += "cosmos prods: " + StringsAndConnectors.cosmosNoPayProducers.size() + "\n";
        ret += "gen2 prods: " + StringsAndConnectors.orderPayProducers.size() + "\n";
        ret += "return prods: " + StringsAndConnectors.returnProducers.size() + "\n";
        ret += "item prods: " + StringsAndConnectors.itemProducers.size() + "\n";
        ret += System.getenv(s) + "\n";
        return ret;
    }
}
