package com.demo.user;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量删除请求体：{@code { "ids": [1, 2, 3] }}。
 */
public class BatchDeleteRequest {

    /** 要删除的用户主键列表，不能为空 */
    @NotEmpty(message = "请选择要删除的用户")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
