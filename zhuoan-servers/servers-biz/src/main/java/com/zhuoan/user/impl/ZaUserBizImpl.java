package com.zhuoan.user.impl;

import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.zhuoan.user.ZaUserBiz;
import com.zhuoan.mapper.ZaUsersMapper;
import com.zhuoan.model.ZaUsers;
import com.zhuoan.model.condition.ZaUsersCondition;
import com.zhuoan.model.vo.ZaUsersVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Deprecated
public class ZaUserBizImpl implements ZaUserBiz {

    @Resource
    private ZaUsersMapper zaUsersMapper;

    @Override
    public int insertSelective(ZaUsers record) {
        return zaUsersMapper.insertSelective(record);
    }

    @Override
    public ZaUsers selectByPrimaryKey(Long id) {
        return zaUsersMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(ZaUsers record) {
        return zaUsersMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(ZaUsers record) {
        return zaUsersMapper.updateByPrimaryKey(record);
    }

    @Override
    public PageList<ZaUsersVO> queryAllUsersByCondition(ZaUsersCondition zaUsersCondition) {
        return zaUsersMapper.queryAllUsersByCondition(zaUsersCondition, zaUsersCondition.getPageBounds());
    }

}
