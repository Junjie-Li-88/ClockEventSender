package com.loreal.clock;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;

public class SerializeReturnObject {
    private JSONObject ret;
    private boolean payTimeIsNull;

    public SerializeReturnObject(JSONObject ret, boolean payTimeIsNull) {
        this.ret = ret;
        this.payTimeIsNull = payTimeIsNull;
    }

    public JSONObject getRet() {
        return this.ret;
    }

    public boolean getIsNull() {
        return this.payTimeIsNull;
    }
}
