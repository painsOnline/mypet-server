/**
 * File: MemberAddressService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.service;

import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.UUIDUtil;
import app.xinqianmao.com.frontend.common.entity.Receiver;
import app.xinqianmao.com.frontend.common.pojo.AddressResponse;
import app.xinqianmao.com.frontend.common.pojo.AddressSaveRequest;
import app.xinqianmao.com.frontend.dao.ReceiverMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Member address management service.
 * Currently, t_receiver has no user FK column; all addresses are treated as
 * belonging to the authenticated user of the current tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAddressService {

    private final ReceiverMapper receiverMapper;

    /**
     * List all delivery addresses for the current user.
     */
    public List<AddressResponse> listByUser(String userId) {
        List<Receiver> receivers = receiverMapper.selectList(new LambdaQueryWrapper<>());
        return receivers.stream().map(this::toAddressResponse).collect(Collectors.toList());
    }

    /**
     * Get single address by ID.
     */
    public AddressResponse getById(String id) {
        Receiver receiver = receiverMapper.selectById(id);
        if (receiver == null) {
            throw new BizException("404", "地址不存在");
        }
        return toAddressResponse(receiver);
    }

    /**
     * Create a new delivery address.
     */
    @Transactional
    public AddressResponse create(AddressSaveRequest req) {
        Receiver receiver = new Receiver();
        receiver.setId(UUIDUtil.uuid());
        receiver.setReceiver(req.getReceiver());
        receiver.setContact(req.getContact());
        receiver.setProvinceCode(req.getProvinceCode());
        receiver.setCityCode(req.getCityCode());
        receiver.setCountyCode(req.getCountyCode());
        receiver.setAddress(req.getAddress());
        receiver.setIsDefault(req.getIsDefault() != null ? req.getIsDefault() : 0);
        receiver.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        receiverMapper.insert(receiver);
        return toAddressResponse(receiver);
    }

    /**
     * Update an existing delivery address.
     */
    @Transactional
    public AddressResponse update(String id, AddressSaveRequest req) {
        Receiver receiver = receiverMapper.selectById(id);
        if (receiver == null) {
            throw new BizException("404", "地址不存在");
        }
        receiver.setReceiver(req.getReceiver());
        receiver.setContact(req.getContact());
        receiver.setProvinceCode(req.getProvinceCode());
        receiver.setCityCode(req.getCityCode());
        receiver.setCountyCode(req.getCountyCode());
        receiver.setAddress(req.getAddress());
        receiver.setIsDefault(req.getIsDefault() != null ? req.getIsDefault() : 0);
        receiver.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        receiverMapper.updateById(receiver);
        return toAddressResponse(receiver);
    }

    /**
     * Delete a delivery address by ID.
     */
    @Transactional
    public void delete(String id) {
        Receiver receiver = receiverMapper.selectById(id);
        if (receiver == null) {
            throw new BizException("404", "地址不存在");
        }
        receiverMapper.deleteById(id);
    }

    /**
     * Convert Receiver entity to AddressResponse DTO.
     */
    private AddressResponse toAddressResponse(Receiver r) {
        AddressResponse resp = new AddressResponse();
        resp.setId(r.getId());
        resp.setReceiver(r.getReceiver());
        resp.setContact(r.getContact());
        resp.setProvinceCode(r.getProvinceCode());
        resp.setCityCode(r.getCityCode());
        resp.setCountyCode(r.getCountyCode());
        resp.setAddress(r.getAddress());
        resp.setIsDefault(r.getIsDefault());
        // Build fullLocation from codes
        resp.setFullLocation(buildFullLocation(r.getProvinceCode(), r.getCityCode(), r.getCountyCode()));
        return resp;
    }

    /**
     * Build full location string from province/city/county codes.
     * Currently concatenates codes with spaces; in production this would look up actual names.
     */
    private String buildFullLocation(String provinceCode, String cityCode, String countyCode) {
        StringBuilder sb = new StringBuilder();
        if (provinceCode != null && !provinceCode.isBlank()) sb.append(provinceCode);
        if (cityCode != null && !cityCode.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(cityCode);
        }
        if (countyCode != null && !countyCode.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(countyCode);
        }
        return sb.toString();
    }
}
