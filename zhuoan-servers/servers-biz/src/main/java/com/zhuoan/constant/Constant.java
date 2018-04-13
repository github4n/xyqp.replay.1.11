package com.zhuoan.constant;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.bdx.BDXGameRoom;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.model.zjh.ZJHGame;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Constant implements Serializable{


    // 客户端标识（用户唯一标识）
    public final static String CLIENTTAG = "clienttag";
    private static final long serialVersionUID = 6536626351642832853L;

    /**
     * 获取用户标识
     * @param client
     * @return
     */
    public static String getClientTag(SocketIOClient client){

        if(client!=null&&client.has(Constant.CLIENTTAG)){
            return client.get(Constant.CLIENTTAG);
        }
        return "";
    }

    // 玩家在线状态
    public final static int ONLINE_STATUS_YES=1;//在线
    public final static int ONLINE_STATUS_NO=0;//不在线

    // 加载配置文件信息
    public static Properties cfgProperties = new Properties();

    /**
     * zagame项目名
     */
    public static String DOMAIN = "/zagame";

    static{

        try {
            cfgProperties.load(Constant.class.getClassLoader().getResourceAsStream("config/common.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /////////////////// 麻将游戏 ////////////////////////

    /**
     * 20180315
     * 房间key
     */
    public final static String ROOM_KEY_COMMEN="ROOM_KEY_COMMEN";

    /**
     * 20180315
     * 房间列表
     */
    public static Map<String, GameRoom> gameRoomMap = new ConcurrentHashMap<String,GameRoom>();

    /**
     * 麻将房间key
     */
    public final static String ROOM_KEY_MJ="ROOM_KEY_MJ";

    /**
     * 泉州麻将游戏房间列表
     */
//    public static Map<String, MJGame> mjGameMap = new ConcurrentHashMap<String, MJGame>();

    /**
     * 福州麻将游戏房间列表
     */
//    public static Map<String, FZMJGame> fzmjGameMap = new ConcurrentHashMap<String, FZMJGame>();

    /**
     * 漳浦麻将游戏房间列表
     */
//    public static Map<String, ZPMJGame> zpmjGameMap = new ConcurrentHashMap<String, ZPMJGame>();

    /**
     * 无锡麻将游戏房间列表
     */
//    public static Map<String, WXMJGame> wxmjGameMap = new ConcurrentHashMap<String, WXMJGame>();

    /**
     * 南安麻将游戏房间列表
     */
//    public static Map<String, NAMJGame> namjGameMap = new ConcurrentHashMap<String, NAMJGame>();

    ////////////////////////////////////////////////////

    /////////////////// 牛牛游戏 ////////////////////////

    /**
     *  牛牛游戏房间Key
     */
    public final static String ROOM_KEY_NN = "ROOM_KEY_NN";

    /**
     * 牛牛游戏房间列表
     */
    public static Map<String, NNGameRoom> niuNiuGameMap = new ConcurrentHashMap<String, NNGameRoom>();

    ////////////////////////////////////////////////////

    /////////////////// 斗地主游戏 ////////////////////////

    /**
     *  斗地主游戏房间Key
     */
    public static String ROOM_KEY_DDZ = "ROOM_KEY_DDZ";

    /**
     * 斗地主游戏房间列表
     */
//    public static Map<String, GameDDZRoom> ddzGameMap = new HashMap<String, GameDDZRoom>();
    ////////////////////////////////////////////////////

    /////////////////// 十三水游戏 ////////////////////////

    /**
     *  十三水游戏房间Key
     */
    public static String ROOM_KEY_SSS = "ROOM_KEY_SSS";

    /**
     * 十三水游戏房间列表
     */
    public static Map<String, SSSGameRoom> sssGameMap = new ConcurrentHashMap<String, SSSGameRoom>();

    ////////////////////////////////////////////////////
    /////////////////// 比大小游戏 ////////////////////////

    /**
     *  比大小游戏房间Key
     */
    public static String ROOM_KEY_BDX = "ROOM_KEY_BDX";

    /**
     * 比大小游戏房间列表
     */
    public static Map<String, BDXGameRoom> bdxGameMap = new ConcurrentHashMap<String, BDXGameRoom>();

    ////////////////////////////////////////////////////

    /////////////////// 牌九游戏 ////////////////////////

    /**
     *  牌九游戏房间Key
     */
    public static String ROOM_KEY_PJ = "ROOM_KEY_PJ";

    /**
     * 牌九游戏房间列表
     */
//    public static Map<String, GamePJRoom> pjGameMap = new HashMap<String, GamePJRoom>();

    /////////////////// 骨牌牌九游戏 ////////////////////////

    /**
     *  骨牌牌九游戏房间Key
     */
    public static String ROOM_KEY_PJXY = "ROOM_KEY_PJXY";

    /**
     * 骨牌牌九游戏房间列表
     */
//    public static Map<String, GamePJXYRoom> pjxyGameMap = new HashMap<String, GamePJXYRoom>();

    ////////////////////////////////////////////////////

    //////////////////// 欢乐哈游戏 	////////////////////
    /**
     * 欢乐哈游戏房间key
     */
    public static String ROOM_KEY_HLH = "ROOM_KEY_HLH";

    /**
     * 欢乐哈游戏房间列表
     */
//    public static Map<String, GameHLHRoom> hlhGameMap = new HashMap<String, GameHLHRoom>();
    ///////////////////	 欢乐哈游戏		////////////////////

    /////////////////// 炸金花游戏 ////////////////////////

    /**
     *  炸金花游戏房间Key
     */
    public static String ROOM_KEY_ZJH = "ROOM_KEY_ZJH";

    /**
     * 炸金花游戏房间列表
     */
    public static Map<String, ZJHGame> zjhGameMap = new HashMap<String, ZJHGame>();

    ////////////////////////////////////////////////////

    /////////////////// 百家乐游戏 ////////////////////////

    /**
     *  百家乐游戏房间Key
     */
    public static String ROOM_KEY_BJL = "ROOM_KEY_BJL";

    /**
     *  百家乐游戏房间列表
     */
//    public static Map<String, GameBJLRoom> bjlGameMap = new HashMap<String, GameBJLRoom>();

    ////////////////////////////////////////////////////

    /////////////////// 抢红包游戏 ////////////////////////

    /**
     *  抢红包游戏房间Key
     */
    public static String ROOM_KEY_HB = "ROOM_KEY_HB";

    /**
     * 抢红包游戏房间列表
     */
//    public static Map<String, GameHBRoom> hbGameMap = new HashMap<String, GameHBRoom>();
}