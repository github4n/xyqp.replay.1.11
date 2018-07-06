package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.FundBiz;
import com.zhuoan.biz.game.dao.FundDao;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:18 2018/7/5
 * @Modified By:
 **/
@Service
public class FundBizImpl implements FundBiz{

    @Resource
    private FundDao fundDao;

    @Override
    public void updateUserScoreByOpenId(String openId, double score) {
        fundDao.updateUserScoreByOpenId(openId, score);
    }

    @Override
    public JSONObject getSysUsers() {
        return fundDao.getSysUsers();
    }

    @Override
    public void updateSysUserStatusByAccount(String account, int status) {
        fundDao.updateSysUserStatusByAccount(account,status);
    }

    @Override
    public JSONObject getVersionInfo() {
        return fundDao.getVersionInfo();
    }

    @Override
    public JSONObject getUserInfoByOpenId(String openId) {
        return fundDao.getUserInfoByOpenId(openId);
    }

    @Override
    public JSONObject getUserInfoByAccount(String account) {
        return fundDao.getUserInfoByAccount(account);
    }

    @Override
    public void insertUserInfo(String account, JSONObject obj, String platform) {
        fundDao.insertUserInfo(account, obj, platform);
    }
}
