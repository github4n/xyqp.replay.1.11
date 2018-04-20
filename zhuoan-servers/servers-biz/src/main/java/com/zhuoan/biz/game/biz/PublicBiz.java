package com.zhuoan.biz.game.biz;


import net.sf.json.JSONArray;

/**
 * @author huaping.li
 * @Description: 游戏大厅公共操作接口
 * @date 2018-04-17 15:09
 */
public interface PublicBiz {

    /**
     * 获取房间设置
     * @param gid
     * @param platform
     * @return
     */
    public JSONArray getRoomSetting(int gid, String platform);

}
