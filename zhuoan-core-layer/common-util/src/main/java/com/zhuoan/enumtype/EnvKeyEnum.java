package com.zhuoan.enumtype;

/**
 * EnvKeyEnum
 *
 * @author weixiang.wu
 * @date 2018 -04-01 14:13
 */
public enum EnvKeyEnum {

    /**
     * 当前环境：本地local=通配  线上=online
     */
    RUN_ENVIRONMENT("run_environment"),

    LOCAL_NAME("local_name"),
    LOCAL_PORT("local_port"),
    SERVER_IP("server_ip"),
    SERVER_PORT("server_port"),


    ;








    private String key;

    EnvKeyEnum(String key) {
        this.key = key;
    }

    /**
     * Gets key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets key.
     *
     * @param key the key
     */
    public void setKey(String key) {
        this.key = key;
    }
}
