package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        comment.setUserId(hostHolder.getUser().getId());
        commentService.addComment(comment);
        return "redirect:/discuss/detail/" + discussPostId;
    }

}
