package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.MatchBiz;
import com.zhuoan.biz.game.dao.MatchDao;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:16 2018/7/12
 * @Modified By:
 **/
@Service
public class MatchBizImpl implements MatchBiz {

    @Resource
    private MatchDao matchDao;

    @Override
    public JSONArray getMatchSettingByType(int type) {
        return matchDao.getMatchSettingByType(type);
    }

    @Override
    public JSONObject getMatchSettingById(long matchId, long gameId) {
        return matchDao.getMatchSettingById(matchId, gameId);
    }

    @Override
    public void updateMatchSettingById(long matchId, String createTime) {
        matchDao.updateMatchSettingById(matchId, createTime);
    }

    @Override
    public JSONObject getMatchInfoByMatchId(long matchId, int isFull, int isEnd) {
        return matchDao.getMatchInfoByMatchId(matchId, isFull, isEnd);
    }

    @Override
    public void addOrUpdateMatchInfo(JSONObject obj) {
        matchDao.addOrUpdateMatchInfo(obj);
    }

    @Override
    public void updateMatchInfoByMatchNum(String matchNum, int isFull) {
        matchDao.updateMatchInfoByMatchNum(matchNum, isFull);
    }

    @Override
    public JSONArray getRobotList(int count) {
        return matchDao.getRobotList(count);
    }

    @Override
    public void updateUserCoinsAndScoreByAccount(String account, int coins, int score, int roomCard) {
        matchDao.updateUserCoinsAndScoreByAccount(account, coins, score, roomCard);
    }

    @Override
    public JSONObject getUserWinningRecord(String account, int gameId) {
        return matchDao.getUserWinningRecord(account, gameId);
    }

    @Override
    public void addOrUpdateUserWinningRecord(JSONObject winningRecord) {
        matchDao.addOrUpdateUserWinningRecord(winningRecord);
    }

}
