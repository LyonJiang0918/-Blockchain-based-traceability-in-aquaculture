package com.trace.entity;

/**
 * 批次状态枚举
 * 0=在养 1=出栏 2=加工中 3=加工完成 4=送至零售商 5=上架 6=销售完成
 */
public enum BatchStatus {
    BREEDING(0, "在养"),
    SLAUGHTERED(1, "出栏"),
    PROCESSING(2, "加工中"),
    PROCESSED(3, "加工完成"),
    TO_RETAILER(4, "送至零售商"),
    ON_SHELF(5, "上架"),
    SOLD(6, "已销售");

    private final int value;
    private final String description;

    BatchStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() { return value; }
    public String getDescription() { return description; }

    public static BatchStatus fromValue(int value) {
        for (BatchStatus status : values()) {
            if (status.value == value) return status;
        }
        return BREEDING;
    }
}
