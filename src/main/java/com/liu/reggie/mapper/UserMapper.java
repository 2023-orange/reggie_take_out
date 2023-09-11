package com.liu.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liu.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
