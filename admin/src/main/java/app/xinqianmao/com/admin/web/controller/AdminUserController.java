/**
 * File: AdminUserController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.pojo.UserListResponse;
import app.xinqianmao.com.admin.common.pojo.UserSearchRequest;
import app.xinqianmao.com.admin.service.UserService;
import app.xinqianmao.com.common.result.PageResult;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * User management controller.
 */
@Tag(name = "用户管理", description = "用户查询和统计数据")
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "用户列表", description = "按手机号、加入时间搜索用户，含订单统计")
    @GetMapping
    public Result<PageResult<UserListResponse>> search(UserSearchRequest request) {
        return Result.ok(PageResult.of(userService.search(request)));
    }
}
