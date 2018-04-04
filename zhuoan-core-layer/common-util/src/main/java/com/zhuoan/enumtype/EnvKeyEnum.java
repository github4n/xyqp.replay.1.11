package com.zhuoan.enumtype;

/**
 * EnvKeyEnum
 *
 * @author weixiang.wu
 * @date 2018 -04-01 14:13
 */
public enum EnvKeyEnum {
    KEY("key"), //just a demo use



    LOCAL_NAME("local_name"),
    LOCAL_IP("local_ip"),
    LOCAL_REMOTE_IP("local_remote_ip"),
    LOCAL_PORT("local_port"),


    SERVER_IP("server_ip"),
    SERVER_REMOTE_IP("server_remote_ip"),
    SERVER_PORT("server_port"),
    SERVER_DOMAIN("server_domain"),


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
