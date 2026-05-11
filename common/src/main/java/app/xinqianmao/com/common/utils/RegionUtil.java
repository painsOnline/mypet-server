/**
 * File: RegionUtil.java
 * Author: system
 * Date: 2026-05-11
 *
 * Converts Chinese administrative region codes to display names.
 */
package app.xinqianmao.com.common.utils;

import java.util.Map;

public final class RegionUtil {

    private static final Map<String, String> REGIONS = Map.ofEntries(
        Map.entry("110000", "北京市"), Map.entry("110100", "市辖区"),
        Map.entry("120000", "天津市"), Map.entry("120100", "市辖区"),
        Map.entry("130000", "河北省"), Map.entry("130100", "石家庄市"),
        Map.entry("140000", "山西省"), Map.entry("140100", "太原市"),
        Map.entry("210000", "辽宁省"), Map.entry("210100", "沈阳市"),
        Map.entry("220000", "吉林省"), Map.entry("220100", "长春市"),
        Map.entry("310000", "上海市"), Map.entry("310100", "市辖区"),
        Map.entry("320000", "江苏省"), Map.entry("320100", "南京市"),
        Map.entry("330000", "浙江省"), Map.entry("330100", "杭州市"),
        Map.entry("340000", "安徽省"), Map.entry("340100", "合肥市"),
        Map.entry("350000", "福建省"), Map.entry("350100", "福州市"),
        Map.entry("360000", "江西省"), Map.entry("360100", "南昌市"),
        Map.entry("370000", "山东省"), Map.entry("370100", "济南市"),
        Map.entry("410000", "河南省"), Map.entry("410100", "郑州市"),
        Map.entry("420000", "湖北省"), Map.entry("420100", "武汉市"),
        Map.entry("430000", "湖南省"), Map.entry("430100", "长沙市"),
        Map.entry("440000", "广东省"), Map.entry("440100", "广州市"),
        Map.entry("440300", "深圳市"), Map.entry("441300", "惠州市"),
        Map.entry("441900", "东莞市"), Map.entry("442000", "中山市"),
        Map.entry("450000", "广西壮族自治区"), Map.entry("450100", "南宁市"),
        Map.entry("460000", "海南省"), Map.entry("460100", "海口市"),
        Map.entry("500000", "重庆市"), Map.entry("500100", "市辖区"),
        Map.entry("510000", "四川省"), Map.entry("510100", "成都市"),
        Map.entry("520000", "贵州省"), Map.entry("520100", "贵阳市"),
        Map.entry("530000", "云南省"), Map.entry("530100", "昆明市"),
        Map.entry("610000", "陕西省"), Map.entry("610100", "西安市"),
        Map.entry("620000", "甘肃省"), Map.entry("620100", "兰州市"),
        Map.entry("630000", "青海省"), Map.entry("630100", "西宁市"),
        Map.entry("640000", "宁夏回族自治区"), Map.entry("640100", "银川市"),
        Map.entry("650000", "新疆维吾尔自治区"), Map.entry("650100", "乌鲁木齐市"),
        Map.entry("710000", "台湾省"),
        Map.entry("810000", "香港特别行政区"),
        Map.entry("820000", "澳门特别行政区")
    );

    private RegionUtil() {}

    /** Get region name by code, return the code itself if unknown. */
    public static String getName(String code) {
        if (code == null || code.isBlank()) return "";
        return REGIONS.getOrDefault(code.trim(), code.trim());
    }
}
