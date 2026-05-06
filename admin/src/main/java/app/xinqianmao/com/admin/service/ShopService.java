/**
 * File: ShopService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.Shop;
import app.xinqianmao.com.admin.common.pojo.ShopSaveRequest;
import app.xinqianmao.com.admin.dao.ShopMapper;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopMapper shopMapper;

    public Shop getConfig() {
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        return shops.isEmpty() ? new Shop() : shops.get(0);
    }

    public void updateConfig(ShopSaveRequest req) {
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        Shop shop;
        if (shops.isEmpty()) {
            shop = new Shop();
            shop.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        } else {
            shop = shops.get(0);
            shop.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        }
        if (req.getName() != null) shop.setName(req.getName());
        if (req.getLogo() != null) shop.setLogo(req.getLogo());
        if (req.getFreeShippingAmount() != null) shop.setFreeShippingAmount(req.getFreeShippingAmount());
        if (req.getBanners() != null) shop.setBanners(req.getBanners());

        if (shops.isEmpty()) {
            shopMapper.insert(shop);
        } else {
            // t_shop is single-row, no id column — use update(entity, wrapper) with empty wrapper
            shopMapper.update(shop, new LambdaQueryWrapper<>());
        }
    }
}
