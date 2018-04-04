package com.zhuoan.model.condition;

import com.github.miemiedev.mybatis.paginator.domain.PageBounds;

/**
 * BaseCondition
 *
 * @author: weixiang.wu
 * @date: 2018 -04-01 12:34
 */
public class BaseCondition {

    /** The Page no. 若无赋值默认为第一页 */
    private Integer pageNo = 1;

    /** The Page limit. */
    private Integer pageLimit;

    /**
     * Gets page bounds.
     *
     * @return the page bounds
     */
    public PageBounds getPageBounds() {
        return new PageBounds(pageNo, pageLimit, true);
    }

    /**
     * Sets page no.
     *
     * @param pageNo the page no
     */
    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    /**
     * Gets page no.
     *
     * @return the page no
     */
    public Integer getPageNo() {
        return pageNo;
    }

    /**
     * Gets page limit.
     *
     * @return the page limit
     */
    public Integer getPageLimit() {
        return pageLimit;
    }

    /**
     * Sets page limit.
     *
     * @param pageLimit the page limit
     */
    public void setPageLimit(Integer pageLimit) {
        this.pageLimit = pageLimit;
    }
}
