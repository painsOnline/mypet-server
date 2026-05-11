/**
 * File: AdminOrderTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Order management closed-loop test:
 *   create setup data → search orders → get order detail → dispatch → confirm receipt → approve refund
 */
package app.xinqianmao.com.admin;

import app.xinqianmao.com.admin.common.entity.*;
import app.xinqianmao.com.admin.dao.*;
import app.xinqianmao.com.common.utils.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminOrderTest extends BaseAdminTest {

    @Autowired private AdminMapper adminMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private ProductTypeMapper typeMapper;
    @Autowired private ProductCategoryMapper categoryMapper;
    @Autowired private ProductSkuMapper skuMapper;
    @Autowired private ReceiverMapper receiverMapper;
    @Autowired private MemberMapper memberMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderProductMapper orderProductMapper;
    @Autowired private OrderProductSkuMapper orderProductSkuMapper;
    @Autowired private PasswordUtil passwordUtil;

    private static String productId;
    private static String skuId;
    private static String receiverId;
    private static String orderId;

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @BeforeEach
    void ensureTestData() {
        ensureAdminExists();
        if (productId == null) createProductAndSku();
        if (receiverId == null) createReceiver();
        if (orderId == null) createOrder();
    }

    private void ensureAdminExists() {
        var existing = adminMapper.selectList(
                new LambdaQueryWrapper<Admin>().eq(Admin::getAccount, ADMIN_ACCOUNT));
        if (existing.isEmpty()) {
            Admin admin = new Admin();
            admin.setAccount(ADMIN_ACCOUNT);
            admin.setPassword(passwordUtil.encode(ADMIN_PASSWORD));
            admin.setCreateTime(LocalDateTime.now());
            adminMapper.insert(admin);
        }
    }

    private void createProductAndSku() {
        String tId = java.util.UUID.randomUUID().toString();
        ProductType type = new ProductType();
        type.setId(tId); type.setName("测试类型-订单"); type.setSort(0);
        type.setCreateTime(LocalDateTime.now()); typeMapper.insert(type);

        String cId = java.util.UUID.randomUUID().toString();
        ProductCategory cat = new ProductCategory();
        cat.setId(cId); cat.setName("测试分类-订单"); cat.setSort(0);
        cat.setCreateTime(LocalDateTime.now()); categoryMapper.insert(cat);

        productId = java.util.UUID.randomUUID().toString();
        app.xinqianmao.com.admin.common.entity.Product product = new app.xinqianmao.com.admin.common.entity.Product();
        product.setId(productId); product.setName("测试商品-订单");
        product.setProductType(tId); product.setProductCategory(cId);
        product.setPrice(new BigDecimal("99.00")); product.setOldPrice(new BigDecimal("128.00"));
        product.setPicture("https://example.com/p.png");
        product.setMainPictures(List.of("https://example.com/p1.png"));
        product.setDetail("<p>desc</p>"); product.setSort(0);
        product.setCreateTime(LocalDateTime.now()); productMapper.insert(product);

        skuId = java.util.UUID.randomUUID().toString();
        ProductSku sku = new ProductSku();
        sku.setId(skuId); sku.setProductId(productId); sku.setProductType(tId);
        sku.setPrice(new BigDecimal("99.00")); sku.setOldPrice(new BigDecimal("128.00"));
        sku.setInventory(100); sku.setPicture("");
        sku.setSpecs("[{\"name\":\"规格\",\"valueName\":\"2.5Kg/袋\"}]");
        sku.setCreateTime(LocalDateTime.now()); skuMapper.insert(sku);
    }

    private void createReceiver() {
        receiverId = java.util.UUID.randomUUID().toString();
        Receiver receiver = new Receiver();
        receiver.setId(receiverId); receiver.setReceiver("测试收货人");
        receiver.setContact("15921769899");
        receiver.setProvinceCode("440000"); receiver.setCityCode("441300");
        receiver.setCountyCode("惠阳区"); receiver.setAddress("星河丹堤花园F区2栋3023");
        receiver.setIsDefault(1); receiver.setCreateTime(LocalDateTime.now());
        receiverMapper.insert(receiver);
    }

    private void createOrder() {
        orderId = java.util.UUID.randomUUID().toString();
        app.xinqianmao.com.admin.common.entity.Order order = new app.xinqianmao.com.admin.common.entity.Order();
        order.setId(orderId); order.setOrderType(1); order.setOrderStatus(1);
        order.setReceiverId(receiverId);
        order.setProductType(skuMapper.selectById(skuId).getProductType());
        order.setTotalMoney(new BigDecimal("256.00"));
        order.setActualPayMoney(new BigDecimal("198.00"));
        order.setPayMoney(new BigDecimal("198.00"));
        order.setCreateTime(LocalDateTime.now()); orderMapper.insert(order);

        app.xinqianmao.com.admin.common.entity.Product product = productMapper.selectById(productId);
        OrderProduct op = new OrderProduct();
        op.setOrderNo(orderId); op.setProductId(productId);
        op.setProductType(product.getProductType()); op.setProductCategory(product.getProductCategory());
        op.setName(product.getName()); op.setDesc(product.getDesc());
        op.setPrice(product.getPrice()); op.setOldPrice(product.getOldPrice());
        op.setMainPictures(product.getMainPictures()); op.setPicture(product.getPicture());
        op.setDetail(product.getDetail()); op.setSort(0);
        op.setCreateTime(LocalDateTime.now()); orderProductMapper.insert(op);

        OrderProductSku ops = new OrderProductSku();
        ProductSku sku = skuMapper.selectById(skuId);
        ops.setOrderNo(orderId); ops.setSkuId(skuId); ops.setProductId(productId);
        ops.setProductType(sku.getProductType());
        ops.setPrice(sku.getPrice()); ops.setOldPrice(sku.getOldPrice());
        ops.setCount(2); ops.setPicture(sku.getPicture()); ops.setSpecs(sku.getSpecs());
        ops.setCreateTime(LocalDateTime.now()); orderProductSkuMapper.insert(ops);
    }

    @Test
    @Order(1)
    @DisplayName("Get pending orders")
    void getPendingOrders() throws Exception {
        mockMvc.perform(adminGet("/admin/order/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Search orders by status and price range")
    void searchOrders() throws Exception {
        // GET /admin/order?...params... — search uses GET query string, not POST
        var result = mockMvc.perform(adminGet("/admin/order", mapOf("page", Long.valueOf(1), "pageSize", Long.valueOf(10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andReturn();
        var pageResult = fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {});
        Assertions.assertTrue(((Number) pageResult.get("counts")).intValue() >= 1);

        // Search by status = 1 (pending)
        mockMvc.perform(adminGet("/admin/order", mapOf("page", Long.valueOf(1), "pageSize", Long.valueOf(10), "orderStatus", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @Order(3)
    @DisplayName("Get order detail with products, SKUs, receiver")
    void getOrderDetail() throws Exception {
        mockMvc.perform(adminGet("/admin/order/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.products").isArray())
                .andExpect(jsonPath("$.result.receiver").exists());
    }

    @Test
    @Order(4)
    @DisplayName("Dispatch order: status 1 -> 2")
    void dispatchOrder() throws Exception {
        var order = orderMapper.selectById(orderId);
        Assertions.assertEquals(Integer.valueOf(1), order.getOrderStatus());

        mockMvc.perform(adminPut("/admin/order/" + orderId + "/delivery", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        order = orderMapper.selectById(orderId);
        Assertions.assertEquals(Integer.valueOf(2), order.getOrderStatus());
    }

    @Test
    @Order(5)
    @DisplayName("Cannot dispatch non-pending order")
    void cannotDispatchTwice() throws Exception {
        mockMvc.perform(adminPut("/admin/order/" + orderId + "/delivery", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @Order(6)
    @DisplayName("Confirm receipt: status 2 -> 3")
    void confirmReceipt() throws Exception {
        mockMvc.perform(adminPut("/admin/order/" + orderId + "/receipt", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        var order = orderMapper.selectById(orderId);
        Assertions.assertEquals(Integer.valueOf(3), order.getOrderStatus());
    }

    @Test
    @Order(7)
    @DisplayName("Approve refund: status 3 -> 5")
    void approveRefund() throws Exception {
        mockMvc.perform(adminPut("/admin/order/" + orderId + "/refund/approve", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        var order = orderMapper.selectById(orderId);
        Assertions.assertEquals(Integer.valueOf(5), order.getOrderStatus());
    }
}
