package com.demo.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.common.ApiResult;
import com.demo.common.OperLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @OperLog("分页查询用户")
    @GetMapping
    public ApiResult<Page<UserVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码从 1 开始") long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(value = 100, message = "每页最多 100 条") long size,
            @RequestParam(required = false) String username
    ) {
        return ApiResult.ok(userService.pageUsers(page, size, username));
    }

    @OperLog("批量删除用户")
    @DeleteMapping("/batch")
    public ApiResult<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest req) {
        userService.batchDelete(req.getIds());
        return ApiResult.ok();
    }
}
