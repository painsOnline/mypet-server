/**
 * File: FullFlowTest.java
 * Author: system
 * Date: 2026-05-06
 *
 * Full business flow test: address → cart → order (cart checkout + direct buy).
 * Uses real HTTP calls via RestTemplate to ensure data persists in DB.
 * Run standalone: starts Spring Boot server on random port.
 */
package app.xinqianmao.com.frontend;

import app.xinqianmao.com.common.DbInitializer;
import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.frontend.common.entity.Cart;
import app.xinqianmao.com.frontend.common.entity.Member;
import app.xinqianmao.com.frontend.common.entity.Product;
import app.xinqianmao.com.frontend.common.entity.ProductSku;
import app.xinqianmao.com.frontend.common.entity.OrderProduct;
import app.xinqianmao.com.frontend.common.entity.OrderProductSku;
import app.xinqianmao.com.frontend.common.entity.OrderReceiver;
import app.xinqianmao.com.frontend.common.entity.Receiver;
import app.xinqianmao.com.frontend.dao.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = app.xinqianmao.com.frontend.web.FrontendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional(propagation = Propagation.NEVER) // Never rollback — data must persist
class FullFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private ReceiverMapper receiverMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderProductMapper orderProductMapper;
    @Autowired
    private OrderProductSkuMapper orderProductSkuMapper;
    @Autowired
    private OrderReceiverMapper orderReceiverMapper;
    @Autowired
    private app.xinqianmao.com.frontend.dao.ProductSkuMapper skuMapper;
    @Autowired
    private app.xinqianmao.com.frontend.dao.ProductMapper productMapper;
    @Autowired
    private app.xinqianmao.com.common.auth.JwtUtil jwtUtil;

    private static final ObjectMapper om = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule());
    private static final String TENANT = "xlong";

    private static String member1Id, member1Token;
    private static String member2Id, member2Token;
    private static String member3Id, member3Token;

    private static String product1Id, product1SkuId;
    private static String product2Id, product2SkuId;
    private static String product1Name, product2Name;
    private static BigDecimal product1Price, product2Price;
    private static BigDecimal product1OldPrice;

    private static String addr1Id, addr2Id, addr3Id;
    private static String order1Id, order2Id, order3Id;

    @BeforeAll
    static void initDb() {
        DbInitializer.ensureDatabases();
    }

    // ============================================================
    // STEP 0: Pick 3 members and 2 products from DB
    // ============================================================
    @Test
    @Order(1)
    @DisplayName("Pick 3 members and fetch existing products with SKUs")
    void pickMembersAndProducts() {
        TenantContext.set(TENANT);
        try {
            // Pick 3 members (first, middle, last)
            List<Member> members = memberMapper.selectList(
                    new LambdaQueryWrapper<Member>().orderByAsc(Member::getCreateTime));
            assertTrue(members.size() >= 3, "Need at least 3 members");
            member1Id = members.get(0).getId();
            member2Id = members.get(members.size() / 2).getId();
            member3Id = members.get(members.size() - 1).getId();
            member1Token = jwtToken(member1Id);
            member2Token = jwtToken(member2Id);
            member3Token = jwtToken(member3Id);
            System.out.println("Members: " + member1Id + " / " + member2Id + " / " + member3Id);

            // Pick 2 products with SKUs
            List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().eq(Product::getIsEnable, 1));
            assertTrue(products.size() >= 2, "Need at least 2 enabled products");
            Product p1 = products.get(0);
            Product p2 = products.get(products.size() - 1);
            product1Id = p1.getId();
            product2Id = p2.getId();
            product1Name = p1.getName();
            product2Name = p2.getName();
            product1Price = p1.getPrice();
            product2Price = p2.getPrice();
            product1OldPrice = p1.getOldPrice();

            // Get first SKU for each
            List<ProductSku> skus1 = skuMapper.selectList(
                    new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, product1Id));
            List<ProductSku> skus2 = skuMapper.selectList(
                    new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, product2Id));
            assertFalse(skus1.isEmpty(), "Product 1 must have at least 1 SKU");
            assertFalse(skus2.isEmpty(), "Product 2 must have at least 1 SKU");
            product1SkuId = skus1.get(0).getId();
            product2SkuId = skus2.get(0).getId();
            System.out.println("Products: " + product1Name + " (SKU:" + product1SkuId + ") / "
                    + product2Name + " (SKU:" + product2SkuId + ")");
        } finally {
            TenantContext.clear();
        }
    }

    // ============================================================
    // STEP 1: Create default addresses for ALL 30 members
    // ============================================================
    @Test
    @Order(2)
    @DisplayName("Create default addresses for all members")
    void createAddressesForAllMembers() {
        TenantContext.set(TENANT);
        try {
            // Skip if addresses already exist (idempotent)
            long existingCount = receiverMapper.selectCount(new LambdaQueryWrapper<>());
            if (existingCount >= 30) {
                System.out.println("[SKIP] Addresses already exist: " + existingCount);
                // Load existing address IDs for our 3 test members
                List<Receiver> r1 = receiverMapper.selectList(
                        new LambdaQueryWrapper<Receiver>().eq(Receiver::getContact, "13810000001"));
                List<Receiver> r2 = receiverMapper.selectList(
                        new LambdaQueryWrapper<Receiver>().eq(Receiver::getContact, "13810000001"));
                // Need to find address by member ID — use contact field which stores phone
                // For simplicity, pick the first address for each member
                List<Receiver> allAddrs = receiverMapper.selectList(new LambdaQueryWrapper<>());
                if (!allAddrs.isEmpty()) {
                    addr1Id = allAddrs.get(0).getId();
                    addr2Id = allAddrs.get(allAddrs.size() / 2).getId();
                    addr3Id = allAddrs.get(allAddrs.size() - 1).getId();
                }
                return;
            }

            List<Member> all = memberMapper.selectList(new LambdaQueryWrapper<>());
            int count = 0;
            String baseUrl = "http://localhost:" + port;
            RestTemplate rt = new RestTemplate();

            for (Member m : all) {
                String token = jwtToken(m.getId());
                Map<String, Object> addr = new LinkedHashMap<>();
                addr.put("receiver", "收货人" + m.getMobile().substring(m.getMobile().length() - 4));
                addr.put("contact", m.getMobile());
                addr.put("provinceCode", "110000");
                addr.put("cityCode", "110100");
                addr.put("countyCode", "110101");
                addr.put("address", "测试街道" + (count + 1) + "号");
                addr.put("isDefault", 1);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Tenant", TENANT);
                headers.set("Authorization", "Bearer " + token);

                ResponseEntity<Map> resp = rt.exchange(baseUrl + "/frontend/member/address",
                        HttpMethod.POST, new HttpEntity<>(addr, headers), Map.class);

                assertEquals("200", String.valueOf(resp.getBody().get("code")),
                        "Create address failed for " + m.getMobile());

                // Save member-1/2/3 address IDs
                Map<String, Object> result = (Map<String, Object>) resp.getBody().get("result");
                if (m.getId().equals(member1Id)) addr1Id = (String) result.get("id");
                if (m.getId().equals(member2Id)) addr2Id = (String) result.get("id");
                if (m.getId().equals(member3Id)) addr3Id = (String) result.get("id");
                count++;
            }
            System.out.println("Created " + count + " addresses");
            assertNotNull(addr1Id, "Member1 address should exist");
            assertNotNull(addr2Id, "Member2 address should exist");
            assertNotNull(addr3Id, "Member3 address should exist");
        } finally {
            TenantContext.clear();
        }
    }

    // ============================================================
    // STEP 2: Member1 adds products to cart, creates order from cart
    // ============================================================
    @Test
    @Order(3)
    @DisplayName("Member1: add 2 products to cart, create order from cart")
    void member1CartOrder() {
        String baseUrl = "http://localhost:" + port;
        RestTemplate rt = new RestTemplate();

        // Step 2a: Add items to cart via PUT /frontend/member/cart (full replacement)
        List<Map<String, Object>> cartItems = new ArrayList<>();

        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("skuId", product1SkuId);
        item1.put("name", product1Name);
        item1.put("count", 2);
        item1.put("price", product1OldPrice != null ? product1OldPrice : product1Price);
        item1.put("nowPrice", product1Price);
        item1.put("picture", "");
        item1.put("selected", true);
        cartItems.add(item1);

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("skuId", product2SkuId);
        item2.put("name", product2Name);
        item2.put("count", 1);
        item2.put("price", product2Price);
        item2.put("nowPrice", product2Price);
        item2.put("picture", "");
        item2.put("selected", true);
        cartItems.add(item2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Tenant", TENANT);
        headers.set("Authorization", "Bearer " + member1Token);

        ResponseEntity<Map> cartResp = rt.exchange(baseUrl + "/frontend/member/cart",
                HttpMethod.PUT, new HttpEntity<>(cartItems, headers), Map.class);
        assertEquals("200", String.valueOf(cartResp.getBody().get("code")));
        System.out.println("[Member1] Cart updated with 2 items");

        // Verify cart in DB
        TenantContext.set(TENANT);
        try {
            List<Cart> cart = cartMapper.selectList(new LambdaQueryWrapper<>());
            assertEquals(2, cart.size(), "Member1 should have 2 cart items");
        } finally { TenantContext.clear(); }

        // Step 2b: Pre-order from cart (GET /frontend/member/order/pre)
        ResponseEntity<Map> preResp = rt.exchange(baseUrl + "/frontend/member/order/pre",
                HttpMethod.GET, new HttpEntity<>(null, headers), Map.class);
        assertEquals("200", String.valueOf(preResp.getBody().get("code")));
        System.out.println("[Member1] Pre-order OK");

        // Step 2c: Submit order
        Map<String, Object> orderReq = new LinkedHashMap<>();
        orderReq.put("addressId", addr1Id);
        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> op1 = new LinkedHashMap<>();
        op1.put("skuId", product1SkuId);
        op1.put("count", 2);
        products.add(op1);
        Map<String, Object> op2 = new LinkedHashMap<>();
        op2.put("skuId", product2SkuId);
        op2.put("count", 1);
        products.add(op2);
        orderReq.put("products", products);

        ResponseEntity<Map> orderResp = rt.exchange(baseUrl + "/frontend/member/order",
                HttpMethod.POST, new HttpEntity<>(orderReq, headers), Map.class);
        assertEquals("200", String.valueOf(orderResp.getBody().get("code")));
        Map<String, Object> orderResult = (Map<String, Object>) orderResp.getBody().get("result");
        order1Id = (String) orderResult.get("id");
        assertNotNull(order1Id);
        System.out.println("[Member1] Order created: " + order1Id);

        // Verify order in DB
        TenantContext.set(TENANT);
        try {
            app.xinqianmao.com.frontend.common.entity.Order order = orderMapper.selectById(order1Id);
            assertNotNull(order);
            assertEquals(1, order.getOrderStatus());

            // Verify order products
            List<OrderProduct> oProducts = orderProductMapper.selectList(
                    new LambdaQueryWrapper<OrderProduct>().eq(OrderProduct::getOrderNo, order1Id));
            assertEquals(2, oProducts.size());

            // Verify order SKUs
            List<OrderProductSku> oSkus = orderProductSkuMapper.selectList(
                    new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderNo, order1Id));
            assertEquals(2, oSkus.size());

            // Verify receiver snapshot
            OrderReceiver receiver = orderReceiverMapper.selectById(order1Id);
            assertNotNull(receiver);

            // Verify cart cleared
            List<Cart> cart = cartMapper.selectList(new LambdaQueryWrapper<>());
            assertEquals(0, cart.size(), "Cart should be cleared after order");

            System.out.println("[VERIFY] Order " + order1Id + " verified: "
                    + oProducts.size() + " products, " + oSkus.size() + " SKUs, receiver OK, cart cleared");
        } finally { TenantContext.clear(); }
    }

    // ============================================================
    // STEP 3: Member2 adds 1 product to cart, creates order from cart
    // ============================================================
    @Test
    @Order(4)
    @DisplayName("Member2: add 1 product to cart, create order")
    void member2CartOrder() {
        String baseUrl = "http://localhost:" + port;
        RestTemplate rt = new RestTemplate();

        // Add to cart
        List<Map<String, Object>> cartItems = new ArrayList<>();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("skuId", product1SkuId);
        item.put("name", product1Name);
        item.put("count", 1);
        item.put("price", product1OldPrice != null ? product1OldPrice : product1Price);
        item.put("nowPrice", product1Price);
        item.put("picture", "");
        item.put("selected", true);
        cartItems.add(item);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Tenant", TENANT);
        headers.set("Authorization", "Bearer " + member2Token);

        ResponseEntity<Map> cartResp = rt.exchange(baseUrl + "/frontend/member/cart",
                HttpMethod.PUT, new HttpEntity<>(cartItems, headers), Map.class);
        assertEquals("200", String.valueOf(cartResp.getBody().get("code")));
        System.out.println("[Member2] Cart updated");

        // Submit order from cart
        Map<String, Object> orderReq = new LinkedHashMap<>();
        orderReq.put("addressId", addr2Id);
        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("skuId", product1SkuId);
        op.put("count", 1);
        products.add(op);
        orderReq.put("products", products);

        ResponseEntity<Map> orderResp = rt.exchange(baseUrl + "/frontend/member/order",
                HttpMethod.POST, new HttpEntity<>(orderReq, headers), Map.class);
        assertEquals("200", String.valueOf(orderResp.getBody().get("code")));
        order2Id = (String) ((Map<String, Object>) orderResp.getBody().get("result")).get("id");
        assertNotNull(order2Id);
        System.out.println("[Member2] Order created: " + order2Id);

        // Verify
        TenantContext.set(TENANT);
        try {
            assertNotNull(orderMapper.selectById(order2Id));
            List<OrderProductSku> oSkus = orderProductSkuMapper.selectList(
                    new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderNo, order2Id));
            assertEquals(1, oSkus.size());
            List<Cart> cart = cartMapper.selectList(new LambdaQueryWrapper<>());
            assertEquals(0, cart.size());
            System.out.println("[VERIFY] Member2 order OK");
        } finally { TenantContext.clear(); }
    }

    // ============================================================
    // STEP 4: Member3 direct buy (skip cart) — pre/now + submit
    // ============================================================
    @Test
    @Order(5)
    @DisplayName("Member3: direct buy product (skip cart)")
    void member3DirectBuy() {
        String baseUrl = "http://localhost:" + port;
        RestTemplate rt = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Tenant", TENANT);
        headers.set("Authorization", "Bearer " + member3Token);

        // Pre-order now (direct buy)
        ResponseEntity<Map> preResp = rt.exchange(
                baseUrl + "/frontend/member/order/pre/now?skuId=" + product2SkuId + "&count=1",
                HttpMethod.GET, new HttpEntity<>(null, headers), Map.class);
        assertEquals("200", String.valueOf(preResp.getBody().get("code")));
        System.out.println("[Member3] Pre-order now OK");

        // Submit order
        Map<String, Object> orderReq = new LinkedHashMap<>();
        orderReq.put("addressId", addr3Id);
        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("skuId", product2SkuId);
        op.put("count", 1);
        products.add(op);
        orderReq.put("products", products);

        ResponseEntity<Map> orderResp = rt.exchange(baseUrl + "/frontend/member/order",
                HttpMethod.POST, new HttpEntity<>(orderReq, headers), Map.class);
        assertEquals("200", String.valueOf(orderResp.getBody().get("code")));
        order3Id = (String) ((Map<String, Object>) orderResp.getBody().get("result")).get("id");
        assertNotNull(order3Id);
        System.out.println("[Member3] Direct order created: " + order3Id);

        // Verify
        TenantContext.set(TENANT);
        try {
            assertNotNull(orderMapper.selectById(order3Id));
            app.xinqianmao.com.frontend.common.entity.OrderReceiver receiver = orderReceiverMapper.selectById(order3Id);
            assertNotNull(receiver);
            assertNotNull(receiver.getOrderNo());
            System.out.println("[VERIFY] Member3 direct buy order OK");
        } finally { TenantContext.clear(); }
    }

    // ============================================================
    // STEP 5: Final verification — all data consistent
    // ============================================================
    @Test
    @Order(6)
    @DisplayName("Final DB consistency check")
    void finalCheck() {
        TenantContext.set(TENANT);
        try {
            long memberCount = memberMapper.selectCount(new LambdaQueryWrapper<>());
            long addrCount = receiverMapper.selectCount(new LambdaQueryWrapper<>());
            long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<>());
            long orderProductCount = orderProductMapper.selectCount(new LambdaQueryWrapper<>());
            long orderSkuCount = orderProductSkuMapper.selectCount(new LambdaQueryWrapper<>());
            long receiverCount = orderReceiverMapper.selectCount(new LambdaQueryWrapper<>());
            long cartCount = cartMapper.selectCount(new LambdaQueryWrapper<>());

            System.out.println("===== FINAL DB STATE =====");
            System.out.println("Members:        " + memberCount);
            System.out.println("Addresses:      " + addrCount);
            System.out.println("Orders:         " + orderCount);
            System.out.println("Order Products: " + orderProductCount);
            System.out.println("Order SKUs:     " + orderSkuCount);
            System.out.println("Order Receivers:" + receiverCount);
            System.out.println("Cart items:     " + cartCount);

            assertEquals(30, memberCount, "Should have 30 members (seeded)");
            assertTrue(addrCount >= 30, "Should have at least 30 addresses");
            assertTrue(orderCount >= 3, "Should have at least 3 orders");
            assertTrue(orderProductCount >= 4, "Should have at least 4 order products");
            assertTrue(orderSkuCount >= 4, "Should have at least 4 order SKUs");
            assertTrue(receiverCount >= 3, "Should have at least 3 order receivers");
            assertEquals(0, cartCount, "All carts cleared after order submission");
        } finally {
            TenantContext.clear();
        }
    }

    // ---- Helpers ----

    private String jwtToken(String userId) {
        return jwtUtil.generateToken(userId, TENANT, false);
    }
}
