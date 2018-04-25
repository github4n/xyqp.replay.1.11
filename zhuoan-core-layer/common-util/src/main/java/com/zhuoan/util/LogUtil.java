package com.zhuoan.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {

    private final static Logger logger = LoggerFactory.getLogger(LogUtil.class);

    /**
     * 消息打印
     *
     * @param msg the msg
     */
    public static void print(String msg){

        logger.info(msg);
	}
	


	

}
