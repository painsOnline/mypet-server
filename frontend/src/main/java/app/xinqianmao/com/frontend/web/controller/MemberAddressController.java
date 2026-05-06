/**
 * File: MemberAddressController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.frontend.common.entity.Receiver;
import app.xinqianmao.com.frontend.common.pojo.AddressResponse;
import app.xinqianmao.com.frontend.common.pojo.AddressSaveRequest;
import app.xinqianmao.com.frontend.dao.ReceiverMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "地址管理", description = "用户收货地址管理")
@RestController
@RequestMapping("/member/address")
@RequiredArgsConstructor
public class MemberAddressController {

    private final ReceiverMapper receiverMapper;

    @Operation(summary = "获取用户所有地址")
    @GetMapping
    public Result<List<AddressResponse>> listAll() {
        List<Receiver> list = receiverMapper.selectList(
                new LambdaQueryWrapper<Receiver>().orderByDesc(Receiver::getIsDefault));
        return Result.ok(list.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "获取单个地址")
    @GetMapping("/{id}")
    public Result<AddressResponse> detail(@PathVariable String id) {
        Receiver r = receiverMapper.selectById(id);
        if (r == null) throw new BizException("404", "地址不存在");
        return Result.ok(toResponse(r));
    }

    @Operation(summary = "添加收货地址")
    @PostMapping
    public Result<AddressResponse> create(@Valid @RequestBody AddressSaveRequest request) {
        Receiver r = new Receiver();
        r.setReceiver(request.getReceiver());
        r.setContact(request.getContact());
        r.setProvinceCode(request.getProvinceCode());
        r.setCityCode(request.getCityCode());
        r.setCountyCode(request.getCountyCode());
        r.setAddress(request.getAddress());
        r.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        r.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        receiverMapper.insert(r);
        return Result.ok(toResponse(r));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping("/{id}")
    public Result<AddressResponse> update(@PathVariable String id, @Valid @RequestBody AddressSaveRequest request) {
        Receiver r = receiverMapper.selectById(id);
        if (r == null) throw new BizException("404", "地址不存在");
        r.setReceiver(request.getReceiver());
        r.setContact(request.getContact());
        r.setProvinceCode(request.getProvinceCode());
        r.setCityCode(request.getCityCode());
        r.setCountyCode(request.getCountyCode());
        r.setAddress(request.getAddress());
        r.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        r.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        receiverMapper.updateById(r);
        return Result.ok(toResponse(r));
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        receiverMapper.deleteById(id);
        return Result.ok(id);
    }

    private AddressResponse toResponse(Receiver r) {
        AddressResponse resp = new AddressResponse();
        resp.setId(r.getId());
        resp.setReceiver(r.getReceiver());
        resp.setContact(r.getContact());
        resp.setProvinceCode(r.getProvinceCode());
        resp.setCityCode(r.getCityCode());
        resp.setCountyCode(r.getCountyCode());
        resp.setAddress(r.getAddress());
        resp.setIsDefault(r.getIsDefault());
        resp.setFullLocation(r.getProvinceCode() + " " + r.getCityCode() + " " + r.getCountyCode());
        return resp;
    }
}
