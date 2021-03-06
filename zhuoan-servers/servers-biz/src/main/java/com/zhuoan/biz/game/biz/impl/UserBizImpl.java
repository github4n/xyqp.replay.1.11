package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.game.dao.GameDao;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author huaping.li
 * @Description: 玩家操作实现类
 * @date 2018-04-17 16:30
 */
@Service
public class UserBizImpl implements UserBiz {


    /**
     * 游戏数据操作接口
     */
    @Resource
    private GameDao gameDao;


    /**
     * 根据用户ID获取用户信息
     *
     * @param id 用户ID
     * @return
     */
    @Override
    public JSONObject getUserByID(long id) {

        return gameDao.getUserByID(id);
    }

    /**
     * 根据玩家账号获取用户信息
     *
     * @param account
     * @return
     */
    @Override
    public JSONObject getUserByAccount(String account) {

        return gameDao.getUserByAccount(account);
    }

    /**
     * 检查uuid是否合法
     *
     * @param account
     * @param uuid
     * @return
     */
    @Override
    public JSONObject checkUUID(String account, String uuid) {

        JSONObject user = gameDao.getUserByAccount(account);
        JSONObject jsonObject = new JSONObject();
        String code;
        String msg = "";
        if(user!=null){
            String userUuid = user.getString("uuid");
            if (uuid.equals(userUuid)){
                code = "1";
            }else{
                msg = "该帐号已在其他地方登录";
                code = "0";
            }
        } else{
            msg = "用户不存在";
            code = "0";
        }
        jsonObject.put("msg", msg);
        jsonObject.put("data", user);
        jsonObject.put("code", code);
        return jsonObject;
    }

    /**
     * 更新用户 余额
     *
     * @param data  [{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}]
     * @param types 更新类型  元宝:yuanbao  金币：coins   房卡：roomcard  积分：score
     */
    @Override
    public boolean updateUserBalance(JSONArray data, String types) {

        return gameDao.updateUserBalance(data, types);
    }

    /**
     * 插入分润表数据
     *
     * @param obj {"user":[{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}],"gid":x,"roomNo":xxx,"type":xxxx}
     */
    @Override
    public void insertUserdeduction(JSONObject obj) {

        gameDao.insertUserdeduction(obj);
    }

    /**
     * 获取玩家充值元宝（待更新到缓存的元宝）
     *
     * @return JSONArray
     * @throws
     * @date 2018年3月28日
     */
    @Override
    public JSONArray refreshUserBalance() {
        
        JSONArray array =  gameDao.getYbUpdateLog();
        // TODO: 2018-04-18 刷新缓存中的用户元宝数 

        // TODO: 2018-04-18 删除数据记录
        //gameDao.delYbUpdateLog();
        return array;
    }

    /**
     * 获取工会信息
     *
     * @param userInfo
     * @return JSONObject
     * @throws
     * @date 2018年4月10日
     */
    @Override
    public JSONObject getGongHui(JSONObject userInfo) {

        return gameDao.getGongHui(userInfo);
    }

    @Override
    public JSONObject getSysUser(String adminCode, String adminPass, String memo) {
        return gameDao.getSysUser(adminCode, adminPass, memo);
    }

    @Override
    public void updateUserPump(String account, String type, double sum) {
        gameDao.updateUserPump(account,type,sum);
    }

    @Override
    public void addUserTicketRec(JSONObject ticketRec) {
        gameDao.addUserTicketRec(ticketRec);
    }

    @Override
    public JSONObject getUserInfoByTel(String tel) {
        return gameDao.getUserInfoByTel(tel);
    }

    @Override
    public int updateUserInfo(JSONObject userInfo) {
        return gameDao.updateUserInfo(userInfo);
    }

    @Override
    public int deleteUserInfoById(long id) {
        return gameDao.deleteUserInfoById(id);
    }
}