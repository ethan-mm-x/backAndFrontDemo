package com.demo.user;

import java.time.LocalDateTime;

public class UserVO {

    private Long id;
    private String username;
    private LocalDateTime createdAt;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.createdAt = user.getCreatedAt();
        return vo;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
