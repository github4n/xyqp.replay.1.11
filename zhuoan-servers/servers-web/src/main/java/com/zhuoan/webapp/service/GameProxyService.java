package com.zhuoan.webapp.service;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:57 2018/7/25
 * @Modified By:
 **/
public interface GameProxyService {

    /**
     * 清除缓存数据
     *
     * @param type     type
     * @param roomNo   roomNo
     * @param platform platform
     */
    void deleteCacheByKey(int type, String roomNo, String platform);
}
