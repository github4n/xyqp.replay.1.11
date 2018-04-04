package com.zhuoan.enumtype;

/**
 * PaginationEnum
 *
 * @author weixiang.wu
 * @date 2018 -04-01 14:06
 */
public enum PaginationEnum {
    /**
     * Data pagination enum.
     */
    DATA("data"),
    /**
     * Draw pagination enum.
     */
    DRAW("draw"),
    /**
     * Records total pagination enum.
     */
    RECORDS_TOTAL("recordsTotal"),
    /**
     * Records filtered pagination enum.
     */
    RECORDS_FILTERED("recordsFiltered");

    private String constant;

    PaginationEnum(String constant) {
        this.constant = constant;
    }

    /**
     * Gets com.zhuoan.constant.
     *
     * @return the com.zhuoan.constant
     */
    public String getConstant() {
        return constant;
    }

    /**
     * Sets com.zhuoan.constant.
     *
     * @param constant the com.zhuoan.constant
     */
    public void setConstant(String constant) {
        this.constant = constant;
    }
}
