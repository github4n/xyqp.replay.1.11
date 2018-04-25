package com.zhuoan.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库基础操作类
 * @author lhp
 *
 */
public class DBUtil {
    private final static Logger logger = LoggerFactory.getLogger(DBUtil.class);

    
    /**
     * 返回多条记录
     * @param sql
     * @param params
     * @return
     */
    public static JSONArray getObjectListBySQL(String sql, Object[] params){
    	
    	// 从连接池获取数据库连接
    	Connection conn = DBConnectionPool.getConnection();
    	JSONArray jsonArray=new JSONArray();
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	try {
    		
    		// 预编译SQL语句
    		pstmt = conn.prepareStatement(sql);
			
			for (int i=0; i<params.length; i++) {
				
				pstmt.setObject(i+1, params[i]);
			}
			
			rs = pstmt.executeQuery();
			
			// 获取结果集列数
			int columCount = rs.getMetaData().getColumnCount();
			
			while(rs.next()){
				
				JSONObject jsonObject=new JSONObject();
				
				for (int i = 0; i < columCount; i++) {
					
					jsonObject.put(rs.getMetaData().getColumnName(i+1), rs.getObject(i+1));
					
				}
				jsonArray.add(jsonObject);
			}
			return jsonArray;
			
		} catch (SQLException e) {
            logger.error("SQL执行出错",e);
		}finally{
			close(conn, pstmt, rs);
		}
    	return jsonArray;
    }
    
    /**
     * 返回查询结果的第一条数据
     * @param sql
     * @param params
     * @return
     */
    public static JSONObject getObjectBySQL(String sql, Object[] params){
    	
    	// 从连接池获取数据库连接
    	Connection conn = DBConnectionPool.getConnection();
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	try {
    		// 预编译SQL语句
    		pstmt = conn.prepareStatement(sql);
			
			for (int i=0; i<params.length; i++) {
				
				pstmt.setObject(i+1, params[i]);
			}
			
			rs = pstmt.executeQuery();
			
			// 获取结果集列数
			int columCount = rs.getMetaData().getColumnCount();
			//columCount
			if(rs.next()){
				JSONObject jsonObject=new JSONObject();
				
				for (int i = 0; i < columCount; i++) {
					
					jsonObject.put(rs.getMetaData().getColumnName(i+1), rs.getObject(i+1));
				}
				
				return jsonObject;
			}
			
		} catch (SQLException e) {
			logger.error("SQL执行出错",e);
		}finally{
			close(conn, pstmt, rs);
		}
    	return null;
    }
    
    
    /**
     * 更新数据，返回影响行数
     * @param sql
     * @param params
     * @return
     */
    public static int executeUpdateBySQL(String sql, Object[] params){
    	
    	// 从连接池获取数据库连接
    	Connection conn = DBConnectionPool.getConnection();
    	PreparedStatement pstmt = null;
    	try {
    		// 预编译SQL语句
    		pstmt = conn.prepareStatement(sql);
			
			for (int i=0; i<params.length; i++) {
				
				// json对象需转化成String类型
				if (params[i] instanceof JSONObject || params[i] instanceof JSONArray) {
					pstmt.setString(i+1, params[i].toString());
				}else{
					pstmt.setObject(i+1, params[i]);
				}
			}
			
			return pstmt.executeUpdate();
			
		} catch (SQLException e) {
            logger.error("SQL执行出错",e);
		}finally{
			close(conn, pstmt, null);
		}
    	return 0;
    }



	/**
	 * 释放资源
	 * @param conn
	 * @param pstmt
	 * @param rs
	 */
	private static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
		
		try {
			if(rs != null){
				rs.close();
			}
			if(pstmt != null){
				pstmt.close();
			}
			if (conn != null){
				DBConnectionPool.closeConnection(conn);
			}
		}catch (SQLException e) {
            logger.error("释放资源出错",e);
       }
	}
    
}
