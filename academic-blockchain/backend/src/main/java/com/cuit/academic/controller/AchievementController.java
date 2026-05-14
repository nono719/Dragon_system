package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.common.PageResult;
import com.cuit.academic.dto.AchievementCreateRequest;
import com.cuit.academic.dto.AchievementDetailVO;
import com.cuit.academic.dto.AchievementUpdateRequest;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.service.AchievementService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService service;

    @PostMapping
    public ApiResponse<Achievement> create(HttpServletRequest req, @Valid @RequestBody AchievementCreateRequest body) {
        return ApiResponse.ok(service.create(CurrentUser.userId(req), body));
    }

    @PutMapping("/{id}")
    public ApiResponse<Achievement> update(HttpServletRequest req, @PathVariable Long id, @RequestBody AchievementUpdateRequest body) {
        return ApiResponse.ok(service.update(CurrentUser.userId(req), id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest req, @PathVariable Long id) {
        service.delete(CurrentUser.userId(req), id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}")
    public ApiResponse<AchievementDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    /**
     * 我的成果列表。
     * 不传 pageNum / pageSize 时返回完整列表（兼容旧调用方）；
     * 传 pageNum >= 1 时启用分页，返回 PageResult。
     */
    @GetMapping("/mine")
    public ApiResponse<?> mine(HttpServletRequest req,
                               @RequestParam(required = false) Integer pageNum,
                               @RequestParam(required = false) Integer pageSize) {
        Long userId = CurrentUser.userId(req);
        if (pageNum != null && pageNum > 0) {
            int ps = (pageSize == null || pageSize <= 0) ? 10 : pageSize;
            PageHelper.startPage(pageNum, ps);
            List<Achievement> list = service.listMine(userId);
            return ApiResponse.ok(PageResult.of(new PageInfo<>(list)));
        }
        return ApiResponse.ok(service.listMine(userId));
    }

    @GetMapping
    public ApiResponse<PageResult<Achievement>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) String keyword) {
        PageHelper.startPage(pageNum, pageSize);
        List<Achievement> list = service.listAll(keyword);
        return ApiResponse.ok(PageResult.of(new PageInfo<>(list)));
    }
}
