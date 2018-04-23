package com.zhuoan.biz.model.dao;

import net.sf.json.JSONObject;

import java.io.Serializable;

/**
 * PumpDao
 *
 * @author weixiang.wu
 * @date 2018-04-23 20:16
 **/
public class PumpDao implements Serializable{

    private static final long serialVersionUID = -6962256051875959108L;

    private String daoType;

    private JSONObject objectDao;



    public PumpDao() {
    }

    public PumpDao(String daoType, JSONObject objectDao) {
        this.daoType = daoType;
        this.objectDao = objectDao;
    }

    public String getDaoType() {
        return daoType;
    }

    public void setDaoType(String daoType) {
        this.daoType = daoType;
    }

    public JSONObject getObjectDao() {
        return objectDao;
    }

    public void setObjectDao(JSONObject objectDao) {
        this.objectDao = objectDao;
    }
}
