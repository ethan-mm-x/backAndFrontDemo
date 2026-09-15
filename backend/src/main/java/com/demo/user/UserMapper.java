package com.demo.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。继承 {@link BaseMapper} 后自带 insert/select/update/delete/分页等，
 * 一般不用手写 XML（复杂 SQL 再加方法即可）。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
