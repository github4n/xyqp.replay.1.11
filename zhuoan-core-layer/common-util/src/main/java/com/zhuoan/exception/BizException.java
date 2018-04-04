package com.zhuoan.exception;

import com.zhuoan.enumtype.ResCodeEnum;

import java.util.Map;

/**
 * BizException
 *
 * @author weixiang.wu
 * @date 2018 -04-01 13:24
 */
public class BizException extends RuntimeException {

    private Map<String, String> mesageMap;
    private String message;
    private String code;

    /**
     * Instantiates a new Biz com.zhuoan.exception.
     *
     * @param message the message
     */
    public BizException(String message) {
        this.message = message;
    }

    /**
     * Instantiates a new Biz com.zhuoan.exception.
     *
     * @param message the message
     * @param code    the code
     */
    public BizException(String message, String code) {
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
    public BizException(Map<String, String> messageMap, String message, String code) {
        this(message, code);
        this.mesageMap = messageMap;
    }

    /**
     * Instantiates a new Biz com.zhuoan.exception.
     *
     * @param resCodeEnum the res code enum
     */
    public BizException(ResCodeEnum resCodeEnum) {
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
        final StringBuilder sb = new StringBuilder("BizException{");
        sb.append("message='").append(message).append('\'');
        sb.append(", code='").append(code).append('\'');
        sb.append('}');
        return sb.toString();
    }
}