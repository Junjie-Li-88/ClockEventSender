package com.loreal.clock.dto;

import javax.servlet.http.HttpServletResponse;

public class BaseResDTO {
    private int Rescode;
    private String message;
    private int failCode;
    private String failMessage;

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
       this.message = message;
    }
    public int getFailCode() {
        return failCode;
    }
    public void setFailCode(int failCode) {
        this.failCode = failCode;
    }
    public int getRescode() {
        return Rescode;
    }
    public void setRescode(int ResCode ) {
        this.Rescode = Rescode;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }
    
} 
