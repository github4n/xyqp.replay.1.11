package com.zhuoan.enumtype.games;

/**
 * EnvvalueEnum
 *
 * @author weixiang.wu
 * @date 2018 -04-08 11:45
 */
public enum GamesEnum {
    /**
     * Common games enum.通用
     */
    COMMON(0),

    /**
     * Nn games enum.牛牛
     */
    NN(1),

    /**
     * Sss games enum.十三水
     */
    SSS(4),

    /**
     * Zjh games enum.炸金花
     */
    ZJH(6),

    /**
     * Bdx games enum.比大小
     */
    BDX(10),
    

    ;


    private int value;

    GamesEnum(int value) {
        this.value = value;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public int getvalue() {
        return value;
    }

    /**
     * Sets value.
     *
     * @param value the value
     */
    public void setvalue(int value) {
        this.value = value;
    }
}
