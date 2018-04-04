package com.zhuoan.enumtype;


/**
 * The enum Res code enum.
 *
 * @author weixiang.wu
 * @date 2018 -04-01 14:10
 */
public enum ResCodeEnum {
    RES_MSG("resMsg"),
    RES_CODE("resCode"),
    SUCCESS("000000", "成功"),
    SYSTEM_EXCEPTION("900001", "系统异常"),




    OTHER("111111", "我是返回信息");


    private String resCode;
    private String resMessage;

    ResCodeEnum(String resCode) {
        this.resCode = resCode;
    }

    ResCodeEnum(String resCode, String resMessage) {
        this.resCode = resCode;
        this.resMessage = resMessage;
    }

    public String getResCode() {
        return resCode;
    }

    public void setResCode(String resCode) {
        this.resCode = resCode;
    }

    public String getResMessage() {
        return resMessage;
    }

    public void setResMessage(String resMessage) {
        this.resMessage = resMessage;
    }
}
