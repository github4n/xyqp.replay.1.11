package com.zhuoan.dao;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
/**
 * 数据库连接池管理类
 *
 */
@Component
public class DBConnectionPool {

    private final static Logger logger = LoggerFactory.getLogger(DBConnectionPool.class);

    private static DruidDataSource dataSource ;

    @Autowired
    public void setDruidDataSource(DruidDataSource dataSource) {
        DBConnectionPool.dataSource = dataSource;
    }

    private DBConnectionPool() {
        super();
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
            logger.error("",e);
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
