package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController {
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title,String content){
        User user = hostHolder.getUser();
        if(user==null){
            return CommunityUtil.getJSONString(403, "你还没有登录哦!");
        }
        DiscussPost post = new DiscussPost();
        post.setContent(content);
        post.setTitle(title);
        post.setUserId(user.getId());
        post.setCreateTime(new Date());
        discussPostService.insertDiscussPosts(post);
        return CommunityUtil.getJSONString(0, "发布成功!");
    }


    @GetMapping("/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model){
        DiscussPost post = discussPostService.selectDiscussPostById(discussPostId);
        model.addAttribute("post",post);
        User user = userService.findUserId(post.getUserId());
        model.addAttribute("user",user);
        return "/site/discuss-detail";
    }
}
