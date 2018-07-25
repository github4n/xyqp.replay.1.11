package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.PropsDao;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:03 2018/7/25
 * @Modified By:
 **/
@Component
public class PropsDaoImpl implements PropsDao {
    @Override
    public JSONArray getPropsInfoByPlatform(String platform) {
        String sql = "select id,game_id,props_type,props_name,props_price,cost_type,duration,status,platform,description,img_url " +
            "from za_props_info where platform=?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{platform});
    }

    @Override
    public JSONObject getPropsInfoById(long propsId) {
        String sql = "select id,game_id,props_type,props_name,props_price,cost_type,duration,status,platform,description,img_url " +
            "from za_props_info where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{propsId});
    }

    @Override
    public JSONObject getUserPropsByType(String account, int propsType) {
        String sql = "select id,user_account,game_id,props_type,props_name,end_time,status from za_user_props where " +
            "user_account=? and props_type=?";
        JSONObject userProps = DBUtil.getObjectBySQL(sql, new Object[]{account, propsType});
        if (!Dto.isObjNull(userProps)) {
            return TimeUtil.transTimeStamp(DBUtil.getObjectBySQL(sql, new Object[]{account, propsType}),"yyyy-MM-dd HH:mm:ss","end_time");
        }
        return userProps;
    }

    @Override
    public void addOrUpdateUserProps(JSONObject userProps) {
        DBJsonUtil.saveOrUpdate(userProps, "za_user_props");
    }
}
