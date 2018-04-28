package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.dao.GameDao;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:38 2018/4/20
 * @Modified By:
 **/
@Service
public class PublicBizImpl implements PublicBiz{

    @Resource
    GameDao gameDao;

    @Override
    public JSONArray getRoomSetting(int gid, String platform) {
        return gameDao.getRoomSetting(gid,platform);
    }

    @Override
    public JSONObject getSysBaseSet() {
        return gameDao.getSysBaseSet();
    }

    @Override
    public JSONObject getAPPGameSetting() {
        return gameDao.getAPPGameSetting();
    }
    @Override
    public JSONArray getAppObjRec(Long userId, int doType, String gid,String roomid, String roomNo) {
        return gameDao.getAppObjRec(userId,doType,gid,roomid,roomNo);
    }

    @Override
    public void addAppObjRec(JSONObject object) {
        gameDao.addAppObjRec(object);
    }
}
