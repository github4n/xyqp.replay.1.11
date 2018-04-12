package com.zhuoan.user;

import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.zhuoan.model.ZaUsers;
import com.zhuoan.model.condition.ZaUsersCondition;
import com.zhuoan.model.vo.ZaUsersVO;

/**
 * Just a Demo。。。
 */
@Deprecated
public interface ZaUserBiz {
    /**
     * Insert selective int.
     *
     * @param record the record
     * @return the int
     */
    int insertSelective(ZaUsers record);

    /**
     * Select by primary key za users.
     *
     * @param id the id
     * @return the za users
     */
    ZaUsers selectByPrimaryKey(Long id);

    /**
     * Update by primary key selective int.
     *
     * @param record the record
     * @return the int
     */
    int updateByPrimaryKeySelective(ZaUsers record);

    /**
     * Update by primary key int.
     *
     * @param record the record
     * @return the int
     */
    int updateByPrimaryKey(ZaUsers record);

    /**
     * Query all users by condition page list.
     *
     * @param zaUsersCondition the za users condition
     * @return the page list
     */
    PageList<ZaUsersVO> queryAllUsersByCondition(ZaUsersCondition zaUsersCondition);
}
