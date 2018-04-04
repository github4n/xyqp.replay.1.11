package com.zhuoan.webapp.config;

import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

/**
 * The type Environment use config.
 *
 * @author weixiang.wu
 * @date 2018 -04-01 12:42
 */
@Component
@PropertySources
    (
        value =
        @PropertySource
            (
                value =
                    {
                        "classpath:config/common.properties"
//                        , "classpath:db.properties" //如果是相同的key，则最后一个起作用
                    }
//                , ignoreResourceNotFound = true //默认false，即不忽略，找不到将抛出异常
            )
    )
public class EnvironmentUseConfig {
}
