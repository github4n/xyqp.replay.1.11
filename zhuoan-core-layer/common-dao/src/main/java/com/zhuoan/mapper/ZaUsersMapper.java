package com.zhuoan.mapper;

import com.github.miemiedev.mybatis.paginator.domain.PageBounds;
import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.zhuoan.model.ZaUsers;
import com.zhuoan.model.condition.ZaUsersCondition;
import com.zhuoan.model.vo.ZaUsersVO;
@Deprecated
public interface ZaUsersMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ZaUsers record);

    int insertSelective(ZaUsers record);

    ZaUsers selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ZaUsers record);

    int updateByPrimaryKey(ZaUsers record);

    PageList<ZaUsersVO> queryAllUsersByCondition(ZaUsersCondition zaUsersCondition, PageBounds pageBounds);
}