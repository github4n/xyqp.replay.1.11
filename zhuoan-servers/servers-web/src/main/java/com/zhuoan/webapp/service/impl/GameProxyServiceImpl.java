package com.zhuoan.webapp.service.impl;

import com.zhuoan.service.cache.RedisService;
import com.zhuoan.webapp.service.GameProxyService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:57 2018/7/25
 * @Modified By:
 **/
@Service
public class GameProxyServiceImpl implements GameProxyService {

    @Resource
    private RedisService redisService;

    @Override
    public void deleteCacheByKey(int type, String roomNo, String platform) {
        switch (type) {
            case 1:
                break;
            case 2:
                redisService.deleteByKey("game_setting");
                break;
            case 3:
                redisService.deleteByKey("game_setting");
                break;
            case 4:
                redisService.deleteByKey("sign_reward_info_" + platform);
                break;
            case 5:
                redisService.deleteByKey("notice_" + platform);
                break;
            default:
                break;
        }
    }
}
