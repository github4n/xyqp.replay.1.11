package com.zhuoan.dao;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
/**
 * 数据库连接池管理类
 * 
 */
@Component
public class DBConnectionPool {
	
    static DataSource dataSource = new DataSource();

    private DBConnectionPool() {
        super();
    }

    static {
        PoolProperties poolProperties = new PoolProperties();
        Properties dbProperties = new Properties();
        try {
            dbProperties.load(DBConnectionPool.class.getClassLoader().getResourceAsStream("connect.properties"));
            //设置URL
            poolProperties.setUrl(dbProperties.getProperty("jdbc.url"));
            //设置驱动名
            poolProperties.setDriverClassName(dbProperties.getProperty("jdbc.driverClassName"));
            //设置数据库用户名
            poolProperties.setUsername(dbProperties.getProperty("jdbc.username"));
            //设置数据库密码
            poolProperties.setPassword(dbProperties.getProperty("jdbc.password"));
            //设置初始化连接数
            poolProperties.setInitialSize(Integer.valueOf(dbProperties.getProperty("jdbc.initialSize")));
            poolProperties.setMaxActive(Integer.valueOf(dbProperties.getProperty("jdbc.maxActive")));
            poolProperties.setMaxIdle(Integer.valueOf(dbProperties.getProperty("jdbc.maxIdle")));
            poolProperties.setMaxWait(Integer.valueOf(dbProperties.getProperty("jdbc.maxWait")));
            poolProperties.setRemoveAbandoned(Boolean.valueOf(dbProperties.getProperty("jdbc.removeAbandoned")));
            poolProperties.setRemoveAbandonedTimeout(Integer.valueOf(dbProperties.getProperty("jdbc.removeAbandonedTimeout")));
            
            dataSource.setPoolProperties(poolProperties);
        } catch (Exception e) {
            throw new RuntimeException("初始化数据库连接池失败");
        }
    }

    
    /**
     * 获取数据库连接
     * @return 数据库连接
     */
    public static final Connection getConnection() {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    
    /**
    * 关闭连接
    * @param conn 需要关闭的连接
    */
    public static void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("关闭数据库连接失败");
        }
    }
    
}
