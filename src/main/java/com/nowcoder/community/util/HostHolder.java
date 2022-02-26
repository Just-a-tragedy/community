package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/*
代替session
使用Localhost存储用户信息
* */
@Component
public class HostHolder {
    private ThreadLocal<User> users=new ThreadLocal<>();
    public void setUser(User user){
        users.set(user);
    }
    public User getUser(){
        return users.get();
    }
    public void clear(){
        users.remove();
    }
}
