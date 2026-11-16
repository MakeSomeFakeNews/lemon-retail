package com.lemon.erp.modules.wms.dto;

/**
 * 下拉选项（仓库、库位）
 *
 * @param storeId 仓库所属门店（库位为空）
 */
public record OptionItem(Long id, String code, String name, String type, Long storeId) {

    public OptionItem(Long id, String code, String name) {
        this(id, code, name, null, null);
    }
}
