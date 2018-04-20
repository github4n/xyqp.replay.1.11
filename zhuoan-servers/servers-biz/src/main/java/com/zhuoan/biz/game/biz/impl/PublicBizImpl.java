package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.dao.GameDao;
import net.sf.json.JSONArray;
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
}
