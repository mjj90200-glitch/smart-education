package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.dto.LoginResponse;
import com.smartedu.dto.PasswordChangeRequest;
import com.smartedu.dto.RegisterRequest;
import com.smartedu.dto.UserUpdateRequest;
import com.smartedu.entity.User;
import com.smartedu.mapper.UserMapper;
import com.smartedu.security.JwtUtil;
import com.smartedu.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务类
 * @author SmartEdu Team
 */
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户登录
     */
    public LoginResponse login(String username, String password) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = baseMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 4. 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        baseMapper.updateById(user);

        // 5. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 6. 构建用户信息
        UserInfoVO userInfo = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfo);

        return LoginResponse.of(token, userInfo);
    }

    /**
     * 用户注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User existingUser = baseMapper.selectOne(queryWrapper);

        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setGrade(request.getGrade());
        user.setMajor(request.getMajor());
        user.setClassName(request.getClassName());
        user.setDepartment(request.getDepartment());
        user.setTitle(request.getTitle());
        user.setStatus(1);

        // 3. 保存用户
        baseMapper.insert(user);
    }

    /**
     * 根据 ID 获取用户
     */
    public User getUserById(Long userId) {
        return baseMapper.selectById(userId);
    }

    /**
     * 更新用户信息（邮箱、手机、头像）
     * @return 更新后的用户信息
     */
    @Transactional
    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 只允许更新邮箱、手机、头像
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        baseMapper.updateById(user);
        return user;
    }

    /**
     * 更新用户头像
     */
    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setAvatar(avatarUrl);
        baseMapper.updateById(user);
    }

    /**
     * 修改密码（需要验证原密码）
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        baseMapper.updateById(user);
    }
}
