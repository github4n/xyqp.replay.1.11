package com.zhuoan.enumtype;

/**
 * EnvKeyEnum
 *
 * @author weixiang.wu
 * @date 2018 -04-01 14:13
 */
public enum EnvKeyEnum {
    KEY("key"), //just a demo use



    LOCAL_IP("local_ip"),
    LOCAL_PORT("local_port");








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
