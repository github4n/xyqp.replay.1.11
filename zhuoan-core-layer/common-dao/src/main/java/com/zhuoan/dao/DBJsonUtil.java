package com.zhuoan.dao;

import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DBJsonUtil {
	
	/**
	 * 保存或更新(根据id判断,id<0或不包含id时insert,否则update)
	 * @param jsonObject
	 * @param tablename
	 * @return
	 */
	public static int saveOrUpdate(JSONObject jsonObject, String tablename){
		
		if(jsonObject.has("id")){
			if(jsonObject.getLong("id")<0){
			    return add(jsonObject, tablename);
            }
			return update(jsonObject, tablename);
		}else{
		    return add(jsonObject, tablename);
        }
	}
	
	/**
	 * 根据表名添加一条记录(键名需与表中字段名相同)
	 * @param jsonObject
	 * @param tablename
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static int add(JSONObject jsonObject, String tablename){
		
		String sqlHead="insert into "+tablename+"(";
		String sqlTail=") values (";
		
		List<Object> paramList=new ArrayList<Object>();
		
		Iterator iterator = jsonObject.keys();
		// 遍历获取json的键值对
		while(iterator.hasNext()){
			String key = (String) iterator.next();
			Object value = jsonObject.get(key);
			if(key=="id"){
			    //continue;
            }
			sqlHead+=key+",";
			sqlTail+="?,";
			paramList.add(value);
		}
		// 去除最后一个逗号
		sqlHead=sqlHead.substring(0, sqlHead.length()-1);
		sqlTail=sqlTail.substring(0, sqlTail.length()-1);
		// 整合两段sql
		String sql=sqlHead+sqlTail+")";
		// 将变量list转换成数组
		Object[] params=new Object[paramList.size()];
		for(int i=0;i<paramList.size();i++){
			params[i]=paramList.get(i);
		}
		return DBUtil.executeUpdateBySQL(sql, params);
	}

	/**
	 * 根据表名和id更新相应表,返回受影响行数
	 * (json中必须有id,键与数据库中的字段名相同)
	 * @param jsonObject
	 * @param tablename
	 * @return
	 */
	public static int update(JSONObject jsonObject, String tablename) {
		
		String sql="update "+tablename+" set ";
		List<Object> paramList=new ArrayList<Object>();
        // 对应的参数
		Object paramTail=null;
		
		Iterator iterator = jsonObject.keys();
		// 遍历获取json的键值对
		while(iterator.hasNext()){
			String key = (String) iterator.next();
			Object value = jsonObject.get(key);
            //where条件
			if(key=="id") {
				paramTail=value;
			} else {  //需更新的字段
				sql+=key+"=?,";
				paramList.add(value);
			}
		}
		// 去除最后一个逗号
		sql=sql.substring(0, sql.length()-1);
		//将where条件填入
		sql+=" where id=?";
		if(paramTail==null){
		    return 0;
        }
		paramList.add(paramTail);
		// 将变量list转换成数组
		Object[] params=new Object[paramList.size()];
		for(int i=0;i<paramList.size();i++){
			params[i]=paramList.get(i);
		}
		return DBUtil.executeUpdateBySQL(sql, params);
	}
	
	/**
	 * 根据表名和条件(键名keyname)更新相应表,返回受影响行数
	 * (json中必须有相应keyname,键与数据库中的字段名相同)
	 * sql=update tablename set jsonkey1=jsonvalue1,jsonkey2=jsonvalue2... where keyname=json[keyname]
	 * @param jsonObject
	 * @param tablename
	 * @param keyname
	 * @return
	 */
	public static int update(JSONObject jsonObject, String tablename, String keyname) {
		
		String sql="update "+tablename+" set ";
		List<Object> paramList=new ArrayList<Object>();
        // 对应的参数
		Object paramTail=null;
		
		Iterator iterator = jsonObject.keys();
		// 遍历获取json的键值对
		while(iterator.hasNext()){
			String key = (String) iterator.next();
			Object value = jsonObject.get(key);
            //where条件
			if(key==keyname) {
				paramTail=value;
			} else {  //需更新的字段
				sql+=key+"=?,";
				paramList.add(value);
			}
		}
		// 去除最后一个逗号
		sql=sql.substring(0, sql.length()-1);
		//将where条件填入
		sql+=" where "+keyname+"=?";
		if(paramTail==null){
		    return 0;
        }
		paramList.add(paramTail);
		// 将变量list转换成数组
		Object[] params=new Object[paramList.size()];
		for(int i=0;i<paramList.size();i++){
			params[i]=paramList.get(i);
		}
		return DBUtil.executeUpdateBySQL(sql, params);
	}
}
