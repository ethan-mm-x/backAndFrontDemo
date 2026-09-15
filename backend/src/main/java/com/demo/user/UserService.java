package com.demo.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.common.BizException;
import com.demo.security.LoginUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户业务：注册（BCrypt 落库）、登录校验、分页、批量删除。
 * <p>
 * Controller 只做协议层，真正读写库都在这里。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 用户名唯一校验后插入；密码存 BCrypt 哈希，不是明文。 */
    public void register(String username, String rawPassword) {
        Long cnt = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (cnt != null && cnt > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);
    }

    /** 查库 + BCrypt matches；成功返回轻量 LoginUser（给签发 JWT 用）。 */
    public LoginUser login(String username, String rawPassword) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        return new LoginUser(user.getId(), user.getUsername());
    }

    /** 分页查询；自动过滤 deleted=1 的逻辑删除行。 */
    public Page<UserVO> pageUsers(long page, long size, String username) {
        Page<User> p = userMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<User>()
                        .like(StringUtils.hasText(username), User::getUsername, username)
                        .orderByDesc(User::getId)
        );
        Page<UserVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(UserVO::from).toList());
        return voPage;
    }

    /** 批量逻辑删除：底层 UPDATE deleted=1，不是物理 DELETE。 */
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的用户");
        }
        userMapper.deleteByIds(ids);
    }
}
