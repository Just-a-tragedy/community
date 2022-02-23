package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import org.springframework.beans.factory.annotation.Autowired;

public class UserService {
    @Autowired
    UserMapper userMapper;
    public User findUserId(int id){
        return userMapper.selectById(id);
    }
}
