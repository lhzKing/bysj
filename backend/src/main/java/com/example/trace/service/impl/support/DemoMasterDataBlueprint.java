package com.example.trace.service.impl.support;

import java.util.List;

/**
 * Demo master-data blueprint.
 *
 * <p>Mirrors the master data set that {@code scripts/seed_extended_data.py}
 * produces, so the HTTP seed endpoint and the offline Python script result in
 * the same business shape:</p>
 *
 * <ul>
 *   <li>8 demo users (producer/producer2/warehouse/warehouse2/logistics/logistics2/user/dealer1)</li>
 *   <li>18 trace nodes covering 4 node types and 14 provinces</li>
 *   <li>15 SPU specifications across 5 part categories</li>
 *   <li>16 user-node bindings expressing realistic operating authority</li>
 * </ul>
 *
 * <p>All collections here are immutable and safe to share across requests.</p>
 */
public final class DemoMasterDataBlueprint {

    private DemoMasterDataBlueprint() {
    }

    public record DemoUser(String username, String plainPassword, String roleCode) {}

    public record DemoNode(String nodeCode, String nodeName, String nodeType,
                           String province, String city, String address) {}

    public record DemoPart(String partCode, String partName, String partType,
                           String model, String manufacturer, String remark) {}

    public record DemoBinding(String username, String nodeCode, boolean defaultNode) {}

    public static final List<DemoUser> DEMO_USERS = List.of(
            new DemoUser("producer",   "producer123456",  "PRODUCER"),
            new DemoUser("producer2",  "producer123456",  "PRODUCER"),
            new DemoUser("warehouse",  "warehouse123456", "WAREHOUSE"),
            new DemoUser("warehouse2", "warehouse123456", "WAREHOUSE"),
            new DemoUser("logistics",  "logistics123456", "LOGISTICS"),
            new DemoUser("logistics2", "logistics123456", "LOGISTICS"),
            new DemoUser("user",       "user123456",      "USER"),
            new DemoUser("dealer1",    "user123456",      "USER")
    );

    public static final List<DemoNode> DEMO_NODES = List.of(
            // FACTORY x 5
            new DemoNode("NODE-FACTORY-BJ", "北京通用电气制造厂", "FACTORY", "北京", "北京市", "朝阳区将台路 5 号"),
            new DemoNode("NODE-FACTORY-SZ", "深圳精密电子厂",     "FACTORY", "广东", "深圳市", "南山区科技园北区学府路"),
            new DemoNode("NODE-FACTORY-TJ", "天津重工铸造基地",   "FACTORY", "天津", "天津市", "滨海新区开发区第二大街"),
            new DemoNode("NODE-FACTORY-QD", "青岛海尔智造工厂",   "FACTORY", "山东", "青岛市", "崂山区海尔路 1 号"),
            new DemoNode("NODE-FACTORY-XA", "西安航空装备厂",     "FACTORY", "陕西", "西安市", "经济技术开发区凤城八路"),

            // WAREHOUSE x 7
            new DemoNode("NODE-WAREHOUSE-SZ", "苏州中央仓储",       "WAREHOUSE", "江苏", "苏州市", "工业园区星湖街 328 号"),
            new DemoNode("NODE-WAREHOUSE-GZ", "广州华南仓储",       "WAREHOUSE", "广东", "广州市", "番禺区市广路 168 号"),
            new DemoNode("NODE-WAREHOUSE-CQ", "重庆西部物流中心仓", "WAREHOUSE", "重庆", "重庆市", "九龙坡区物流大道 8 号"),
            new DemoNode("NODE-WAREHOUSE-CS", "长沙中南区域仓",     "WAREHOUSE", "湖南", "长沙市", "雨花区芙蓉南路三段"),
            new DemoNode("NODE-WAREHOUSE-SY", "沈阳东北中转仓",     "WAREHOUSE", "辽宁", "沈阳市", "铁西区开发大路 21 号"),
            new DemoNode("NODE-WAREHOUSE-NJ", "南京江北仓储中心",   "WAREHOUSE", "江苏", "南京市", "江北新区研创园纬七路"),
            new DemoNode("NODE-WAREHOUSE-CD", "成都西南仓",         "WAREHOUSE", "四川", "成都市", "高新区天府四街"),

            // LOGISTICS x 5
            new DemoNode("NODE-LOGISTICS-SH", "上海顺丰转运中心",     "LOGISTICS", "上海", "上海市", "浦东新区周浦镇沪南公路"),
            new DemoNode("NODE-LOGISTICS-CD", "成都德邦转运中心",     "LOGISTICS", "四川", "成都市", "双流区西航港大道"),
            new DemoNode("NODE-LOGISTICS-JN", "济南京东亚一物流",     "LOGISTICS", "山东", "济南市", "章丘区龙泉路智能产业园"),
            new DemoNode("NODE-LOGISTICS-FZ", "福州海铁联运中转中心", "LOGISTICS", "福建", "福州市", "马尾区港口大道 88 号"),
            new DemoNode("NODE-LOGISTICS-ZZ", "郑州中部转运枢纽",     "LOGISTICS", "河南", "郑州市", "航空港区华夏大道东段"),

            // CUSTOMER x 2
            new DemoNode("NODE-CUSTOMER-WH", "武汉东风汽车整装厂",   "CUSTOMER", "湖北", "武汉市", "经济技术开发区车城大道"),
            new DemoNode("NODE-CUSTOMER-HF", "合肥京东方面板装配线", "CUSTOMER", "安徽", "合肥市", "新站区文忠路与淮海大道交口")
    );

    public static final List<DemoPart> DEMO_PARTS = List.of(
            new DemoPart("SPU-VALVE-001", "气动球阀",     "阀门类",  "V-2024001", "上海阀门厂",     "工业气动球阀，DN50 PN16"),
            new DemoPart("SPU-VALVE-002", "电动蝶阀",     "阀门类",  "V-2024002", "浙江阀门集团",   "法兰式电动蝶阀，DN100"),
            new DemoPart("SPU-VALVE-003", "安全阀",       "阀门类",  "V-2024003", "山东安全设备",   "弹簧式安全阀，DN25"),

            new DemoPart("SPU-BEAR-001",  "深沟球轴承",   "轴承类",  "B-6205",    "SKF中国",        "内径 25mm，外径 52mm"),
            new DemoPart("SPU-BEAR-002",  "圆柱滚子轴承", "轴承类",  "B-NU206",   "哈尔滨轴承",     "内径 30mm，单列"),
            new DemoPart("SPU-BEAR-003",  "调心滚子轴承", "轴承类",  "B-22208",   "洛阳LYC",        "内径 40mm，重载工况"),

            new DemoPart("SPU-MOTOR-001", "三相异步电机", "电机类",  "M-Y160M",   "卧龙电机",       "YE3 高效，11kW"),
            new DemoPart("SPU-MOTOR-002", "永磁同步电机", "电机类",  "M-PMSM150", "大洋电机",       "15kW 伺服级"),
            new DemoPart("SPU-MOTOR-003", "直流伺服电机", "电机类",  "M-DC110",   "佳木斯电机",     "110V 直流，2.4kW"),

            new DemoPart("SPU-SENS-001",  "温度传感器",   "传感器类","S-PT100",   "E+H中国",        "PT100 三线制"),
            new DemoPart("SPU-SENS-002",  "压力传感器",   "传感器类","S-PR250",   "横河中国",       "0-25MPa 4-20mA"),
            new DemoPart("SPU-SENS-003",  "流量传感器",   "传感器类","S-FL80",    "SICK中国",       "电磁式 DN80"),

            new DemoPart("SPU-PIPE-001",  "无缝钢管",     "管件类",  "P-DN100",   "宝钢股份",       "碳钢 Sch40，DN100"),
            new DemoPart("SPU-PIPE-002",  "法兰盘",       "管件类",  "P-FL150",   "重庆法兰",       "PN16 平焊法兰 DN150"),
            new DemoPart("SPU-PIPE-003",  "弯头",         "管件类",  "P-EL90",    "河北管件",       "90 度无缝弯头 DN80")
    );

    public static final List<DemoBinding> DEMO_BINDINGS = List.of(
            new DemoBinding("producer",   "NODE-FACTORY-BJ",    true),
            new DemoBinding("producer",   "NODE-FACTORY-QD",    false),
            new DemoBinding("producer2",  "NODE-FACTORY-SZ",    true),
            new DemoBinding("producer2",  "NODE-FACTORY-TJ",    false),
            new DemoBinding("producer2",  "NODE-FACTORY-XA",    false),
            new DemoBinding("warehouse",  "NODE-WAREHOUSE-SZ",  true),
            new DemoBinding("warehouse",  "NODE-WAREHOUSE-GZ",  false),
            new DemoBinding("warehouse",  "NODE-WAREHOUSE-NJ",  false),
            new DemoBinding("warehouse2", "NODE-WAREHOUSE-CQ",  true),
            new DemoBinding("warehouse2", "NODE-WAREHOUSE-CS",  false),
            new DemoBinding("warehouse2", "NODE-WAREHOUSE-SY",  false),
            new DemoBinding("logistics",  "NODE-LOGISTICS-SH",  true),
            new DemoBinding("logistics",  "NODE-LOGISTICS-CD",  false),
            new DemoBinding("logistics2", "NODE-LOGISTICS-JN",  true),
            new DemoBinding("logistics2", "NODE-LOGISTICS-FZ",  false),
            new DemoBinding("logistics2", "NODE-LOGISTICS-ZZ",  false)
    );
}
