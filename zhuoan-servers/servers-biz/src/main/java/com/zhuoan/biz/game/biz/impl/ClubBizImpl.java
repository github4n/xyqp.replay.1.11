package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.ClubBiz;
import com.zhuoan.biz.game.dao.ClubDao;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * ClubBizImpl
 *
 * @author wqm
 * @Date Created in 15:39 2018/8/22
 **/
@Service
public class ClubBizImpl implements ClubBiz {

    @Resource
    private ClubDao clubDao;

    @Override
    public JSONObject getUserClubByAccount(String account) {
        return clubDao.getUserClubByAccount(account);
    }

    @Override
    public JSONObject getClubByCode(String clubCode) {
        return clubDao.getClubByCode(clubCode);
    }

    @Override
    public JSONObject getClubById(long id) {
        return clubDao.getClubById(id);
    }

    @Override
    public JSONArray getClubMember(long clubId) {
        return clubDao.getClubMember(clubId);
    }

    @Override
    public JSONObject getUserByAccountAndUuid(String account, String uuid) {
        return clubDao.getUserByAccountAndUuid(account, uuid);
    }

    @Override
    public void updateClubInfo(JSONObject clubInfo) {
        clubDao.updateClubInfo(clubInfo);
    }

    @Override
    public void updateUserClubIds(long userId, String clubIds) {
        clubDao.updateUserClubIds(userId, clubIds);
    }
}
