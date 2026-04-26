package com.trace.dto;

import lombok.Data;
import java.math.BigInteger;

/**
 * 养殖群数据传输对象
 */
@Data
public class BatchDTO {
    /**
     * 养殖群ID（原 batchId）
     * 一窝鸡 / 一圈牛羊 / 一批蜜蜂 等
     */
    private String groupId;

    /**
     * 所属养殖场ID
     */
    private String farmId;

    /**
     * 品种名称（如：白羽鸡、荷斯坦奶牛、小尾寒羊）
     */
    private String species;

    /**
     * 品种分类（用于区分养殖类型）
     * POULTRY - 禽类（鸡鸭鹅）
     * LIVESTOCK - 牲畜（牛羊猪）
     * AQUATIC - 水产
     * OTHER - 其他
     */
    private String speciesCategory;

    /**
     * 存栏数量
     */
    private BigInteger quantity;

    /**
     * 所属圈舍/区域（如：A区3号圈）
     */
    private String location;

    private String metaHash;
    private Long createdAt;
    private String creator;
    private Integer status;

    // 作废相关字段
    private Boolean invalidated;
    private String invalidateReason;
    private Long invalidatedAt;

    // 状态说明（与前端 STATUS_MAP 一致）
    public String getStatusText() {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "在养";
            case 1: return "出栏";
            case 2: return "加工中";
            case 3: return "加工完成";
            case 4: return "送至零售商";
            case 5: return "上架";
            case 6: return "销售完成";
            default: return "未知";
        }
    }

    // 品种分类文本
    public String getSpeciesCategoryText() {
        if (speciesCategory == null) return "未知";
        switch (speciesCategory) {
            case "POULTRY": return "禽类";
            case "LIVESTOCK": return "牲畜";
            case "AQUATIC": return "水产";
            default: return speciesCategory;
        }
    }
}



