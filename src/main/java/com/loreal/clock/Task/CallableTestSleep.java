package com.loreal.clock.Task;

import com.loreal.clock.HttpClient;

import java.util.HashMap;
import java.util.concurrent.Callable;

public class CallableTestSleep implements Callable<Object> {
    private int i;
    public CallableTestSleep(int i) {
        this.i = i;
    }

    @Override
    public Object call() throws Exception {
        try {
            Thread.sleep(4000);
            return i;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
