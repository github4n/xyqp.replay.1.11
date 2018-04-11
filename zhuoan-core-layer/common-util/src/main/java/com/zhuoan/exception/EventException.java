package com.zhuoan.exception;

import com.zhuoan.enumtype.ResCodeEnum;

import java.util.Map;

/**
 * EventException
 *
 * @author weixiang.wu
 * @date 2018-04-11 15:10
 **/
public class EventException extends RuntimeException {


    private Map<String, String> mesageMap;
    private String message;
    private String code;

    /**
     * Instantiates a new Biz com.zhuoan.exception.
     *
     * @param message the message
     */
    public EventException(String message) {
        this.message = message;
    }

    /**
     * Instantiates a new Biz com.zhuoan.exception.
     *
     * @param message the message
     * @param code    the code
     */
    public EventException(String message, String code) {
        this.message = message;
        this.code = code;
    }

    /**
     * Instantiates a new Biz com.zhuoan.exception.
     *
     * @param messageMap the message map
     * @param message    the message
     * @param code       the code
     */
    public EventException(Map<String, String> messageMap, String message, String code) {
        this(message, code);
        this.mesageMap = messageMap;
    }

    /**
     * Instantiates a new Biz com.zhuoan.exception.
     *
     * @param resCodeEnum the res code enum
     */
    public EventException(ResCodeEnum resCodeEnum) {
        this.message = resCodeEnum.getResMessage();
        this.code = resCodeEnum.getResCode();
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets message map.
     *
     * @return the message map
     */
    public Map<String, String> getMessageMap() {
        return mesageMap;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EventException{");
        sb.append("message='").append(message).append('\'');
        sb.append(", code='").append(code).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
