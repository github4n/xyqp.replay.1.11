package com.zhuoan.constant;

import com.zhuoan.biz.model.GameRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class Constant implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(Constant.class);

    private static final long serialVersionUID = 6536626351642832853L;

    /**
     * 玩家在线状态-在线
     */
    public final static int ONLINE_STATUS_YES = 1;
    /**
     * 玩家在线状态-不在线
     */
    public final static int ONLINE_STATUS_NO = 0;

    /**
     * 加载配置文件信息
     */
    public static Properties cfgProperties = new Properties();

    /**
     * zagame项目名
     */
    public static String DOMAIN = "/zagame";

    static {

        try {
            cfgProperties.load(Constant.class.getClassLoader().getResourceAsStream("config/common.properties"));
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    /**
     * 20180315
     * 房间列表
     */
    public static Map<String, GameRoom> gameRoomMap = new ConcurrentHashMap<String, GameRoom>();

}