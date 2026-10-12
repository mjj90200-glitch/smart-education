package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * @author SmartEdu Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
