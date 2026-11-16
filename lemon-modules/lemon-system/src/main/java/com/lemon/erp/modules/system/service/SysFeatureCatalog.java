package com.lemon.erp.modules.system.service;

import java.util.Arrays;
import java.util.List;

/**
 * 功能模块与行业模板：新建租户、编辑套餐时按模块勾选功能，按行业一键选择常用模块。
 * <p>
 * 模块按菜单 ID 定义（sys_menu 中的模块目录、侧边栏分组或页面），包含其下的全部页面和按钮；
 * 新增页面时放在已有分组下会自动归入对应模块，放在新位置时需要在这里登记（{@code FeatureCatalogTests} 检查每个页面都属于某个模块）
 */
final class SysFeatureCatalog {

    private SysFeatureCatalog() {
    }

    /** 功能模块标识 */
    enum Key {
        /** 基础平台 */
        CORE,
        /** 采购 */
        PURCHASE,
        /** 销售 */
        SALES,
        /** 促销 */
        PROMOTION,
        /** 销售提成 */
        COMMISSION,
        /** 进销存报表 */
        JXC_REPORT,
        /** 库存 */
        INVENTORY,
        /** 库存预留 */
        WAREHOUSE_OPS,
        /** 总账 */
        GL,
        /** 应收应付 */
        ARAP,
        /** 资金费用 */
        FUND,
        /** 经营分析 */
        ANALYSIS,
        /** 商业智能 */
        BI,
        /** 数据大屏 */
        SCREEN,
        /** 智能（AI） */
        AI
    }

    /**
     * 名称、分类、说明是消息键（system 资源包），输出时 {@code Messages.get}
     *
     * @param key      标识
     * @param group    分类（页面上分组显示）
     * @param required 必选（基础平台）
     * @param menus    包含的菜单（含其下的全部菜单）
     * @param requires 依赖的模块，勾选时一并勾选
     */
    record Feature(String key, String name, String group, String description, boolean required, List<Long> menus, List<String> requires) {
    }

    /**
     * 名称、说明是消息键
     *
     * @param features 包含的模块（依赖的模块和基础平台自动加入）
     */
    record Industry(String key, String name, String description, List<String> features) {
    }

    private static Feature f(Key key, String name, String group, String description, List<Long> menus, Key... requires) {
        return new Feature(key.name(), name, group, description, false, menus, names(requires));
    }

    private static List<String> names(Key... keys) {
        return Arrays.stream(keys).map(Key::name).toList();
    }

    static final List<Feature> FEATURES = List.of(
            new Feature(Key.CORE.name(), "system.label.corePlatform", "system.label.core", "system.label.systemAdministrationApprovalWorkflowsMasterData", true,
                    List.of(1L, 2L, 3L, 914L, 903L, 17L), List.of()),
            // 供应链
            f(Key.PURCHASE, "system.label.purchasing", "system.label.supplyChain", "system.label.purchaseRequisitionsPurchaseOrdersDeliveryNotices", List.of(901L), Key.INVENTORY),
            f(Key.SALES, "system.label.sales", "system.label.supplyChain", "system.label.quotationsPriceListsSalesOrdersDelivery",
                    List.of(270L, 365L, 368L, 143L, 271L, 144L, 145L, 272L), Key.INVENTORY),
            f(Key.PROMOTION, "system.label.channelDistribution", "system.label.supplyChain", "system.label.promotionsRebatesChannelInventoryConsignmentSettlement", List.of(312L), Key.SALES),
            f(Key.COMMISSION, "system.label.salesCommission", "system.label.supplyChain", "system.label.calculateSalespersonCommissionSalesGrossProfit", List.of(344L), Key.SALES),
            f(Key.JXC_REPORT, "system.label.jxcReport", "system.label.supplyChain", "system.label.jxcReportDesc", List.of(966L), Key.SALES, Key.PURCHASE),
            // 仓储
            f(Key.INVENTORY, "system.label.inventory", "system.label.warehousing", "system.label.inboundOutboundTransfersStockCountsAssembly", List.of(911L, 912L)),
            f(Key.WAREHOUSE_OPS, "system.label.warehouseOperations", "system.label.warehousing", "system.label.stockReservationFreezingMobilePdaScanning", List.of(913L), Key.INVENTORY),
            // 财务
            f(Key.GL, "system.label.generalLedger", "system.label.finance", "system.label.vouchersLedgersFinancialStatementsPeriodClose", List.of(931L, 936L)),
            f(Key.ARAP, "system.label.receivablesPayables", "system.label.finance", "system.label.receivablesPayablesReceiptsPaymentsAdvancesInvoices", List.of(932L), Key.GL),
            f(Key.FUND, "system.label.fundsExpenses", "system.label.finance", "system.label.cashierExpenseReimbursementEmployeeLoansBank", List.of(933L), Key.GL),
            f(Key.ANALYSIS, "system.label.budgetAnalysis", "system.label.finance", "system.label.budgetPreparationExecutionBusinessAnalysis", List.of(935L), Key.GL),
            // 分析
            f(Key.BI, "system.label.businessIntelligence", "system.label.analytics", "system.label.businessCockpitPivotAnalysisCustomReports",
                    List.of(260L, 261L, 262L, 263L, 264L, 265L)),
            f(Key.SCREEN, "system.label.dataScreens", "system.label.analytics", "system.label.dataScreensShopFloorWarehousingLogistics", List.of(349L)),
            f(Key.AI, "system.label.aiFeature", "system.label.analytics", "system.label.aiFeatureDesc", List.of(16L)));

    static final List<Industry> INDUSTRIES = List.of(
            new Industry("RETAIL", "system.label.eCommerceRetail", "system.label.mainlyMultiChannelOrdersWarehousingShipping",
                    names(Key.PURCHASE, Key.SALES, Key.PROMOTION, Key.JXC_REPORT, Key.INVENTORY, Key.WAREHOUSE_OPS, Key.GL, Key.ARAP, Key.FUND,
                            Key.BI, Key.SCREEN)),
            new Industry("TRADING", "system.label.tradingWholesale", "system.label.mainlyPurchasingWarehousingDistributionWithoutProduction",
                    names(Key.PURCHASE, Key.SALES, Key.PROMOTION, Key.COMMISSION, Key.JXC_REPORT, Key.INVENTORY,
                            Key.WAREHOUSE_OPS, Key.GL, Key.ARAP, Key.FUND, Key.ANALYSIS, Key.BI, Key.SCREEN)),
            new Industry("FULL", "system.label.allFeatures", "system.label.enableAllFeatureModules",
                    FEATURES.stream().map(Feature::key).toList()));
}
