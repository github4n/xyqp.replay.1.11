package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.PropsBiz;
import com.zhuoan.biz.game.dao.PropsDao;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:02 2018/7/25
 * @Modified By:
 **/
@Service
public class PropsBizImpl implements PropsBiz {

    @Resource
    private PropsDao propsDao;

    @Override
    public JSONArray getPropsInfoByPlatform(String platform) {
        return propsDao.getPropsInfoByPlatform(platform);
    }

    @Override
    public JSONObject getPropsInfoById(long propsId) {
        return propsDao.getPropsInfoById(propsId);
    }

    @Override
    public JSONObject getUserPropsByType(String account, int propsType) {
        return propsDao.getUserPropsByType(account, propsType);
    }

    @Override
    public void addOrUpdateUserProps(JSONObject userProps) {
        propsDao.addOrUpdateUserProps(userProps);
    }
}
