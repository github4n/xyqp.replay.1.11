package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
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
    public JSONArray getRoomSetting(int gid, String platform, int flag) {
        return gameDao.getRoomSetting(gid,platform,flag);
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

    @Override
    public JSONObject getNoticeByPlatform(String platform){
        return gameDao.getNoticeByPlatform(platform);
    }

    @Override
    public JSONArray getGoldSetting(JSONObject obj){
        return gameDao.getGoldSetting(obj);
    }

    @Override
    public JSONObject getUserSignInfo(String platform, long userId) {
        return gameDao.getUserSignInfo(platform,userId);
    }

    @Override
    public int addOrUpdateUserSign(JSONObject obj) {
        return gameDao.addOrUpdateUserSign(obj);
    }

    @Override
    public JSONObject getSignRewardInfoByPlatform(String platform) {
        return gameDao.getSignRewardInfoByPlatform(platform);
    }

    @Override
    public JSONArray getArenaInfo() {
        return gameDao.getArenaInfo();
    }

    @Override
    public void addOrUpdateUserCoinsRec(long userId, int score,int type) {
        String nowDate = TimeUtil.getNowDate("yyyy-MM-dd");
        JSONObject userCoinsRec = gameDao.getUserCoinsRecById(userId,type,nowDate+" 00:00:00",nowDate+" 23:59:59");
        JSONObject obj = new JSONObject();
        obj.put("user_id",userId);
        obj.put("type",type);
        obj.put("createTime", TimeUtil.getNowDate());
        if (!Dto.isObjNull(userCoinsRec)) {
            obj.put("id",userCoinsRec.getLong("id"));
            if (score>0) {
                obj.put("win",userCoinsRec.getInt("win")+1);
            }
            if (score<0) {
                obj.put("lose",userCoinsRec.getInt("lose")+1);
            }
            obj.put("coins",userCoinsRec.getInt("coins")+score);
        }else {
            obj.put("coins",score);
            if (score>0) {
                obj.put("win",1);
                obj.put("lose",0);
            }
            if (score<0) {
                obj.put("win",0);
                obj.put("lose",1);
            }
        }
        gameDao.addOrUpdateUserCoinsRec(obj);
    }

    @Override
    public JSONObject getUserGameInfo(String account) {
        return gameDao.getUserGameInfo(account);
    }

    @Override
    public void addOrUpdateUserGameInfo(JSONObject obj) {
        gameDao.addOrUpdateUserGameInfo(obj);
    }

    @Override
    public void addUserWelfareRec(String account, double sum, int type, int gameId) {
        JSONObject userInfo = gameDao.getUserByAccount(account);
        if (!Dto.isObjNull(userInfo)) {
            JSONObject obj = new JSONObject();
            obj.put("userid", userInfo.getLong("id"));
            obj.put("gid", gameId);
            obj.put("type", type);
            obj.put("sum", sum);
            obj.put("doType", CommonConstant.SCORE_CHANGE_DO_TYPE_WELFARE);
            obj.put("creataTime", TimeUtil.getNowDate());
            if (type + 1 == CommonConstant.CURRENCY_TYPE_SCORE && userInfo.containsKey("score")) {
                obj.put("pocketOld", userInfo.getInt("score"));
                obj.put("memo", "实物券变动");
            }else if (type + 1 == CommonConstant.CURRENCY_TYPE_YB && userInfo.containsKey("yuanbao")) {
                obj.put("pocketOld", userInfo.getDouble("yuanbao"));
                obj.put("memo", "红包券变动");
            }else if (type + 1 == CommonConstant.CURRENCY_TYPE_ROOM_CARD && userInfo.containsKey("yuanbao")) {
                obj.put("pocketOld", userInfo.getDouble("yuanbao"));
                obj.put("memo", "钻石变动");
            }
            obj.put("pocketChange", sum);
            obj.put("operatorType", CommonConstant.SCORE_CHANGE_TYPE_OTHER);
            if (userInfo.containsKey("platform")) {
                obj.put("platform", userInfo.getString("platform"));
            }
            gameDao.addUserWelfareRec(obj);
        }
    }

    @Override
    public JSONObject getAppSettingInfo(String platform) {
        return gameDao.getAppSettingInfo(platform);
    }
}
