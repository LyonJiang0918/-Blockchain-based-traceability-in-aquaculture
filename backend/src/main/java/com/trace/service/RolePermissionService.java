package com.trace.service;

import com.trace.entity.BatchMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 角色权限服务
 * 统一管理各角色对批次操作的权限校验
 *
 * 流程与权限对照表：
 * 状态0=在养：养殖场/管理员 可创建
 * 状态1=出栏：养殖场/管理员 可操作（需选择加工厂），不可撤回
 * 状态2=加工中：加工厂/管理员 可操作（推进到加工完成），不可撤回
 * 状态3=加工完成：加工厂/管理员 可操作（需选择零售商，推进到送至零售商），不可撤回
 * 状态4=送至零售商：加工厂/管理员 可操作（推进到上架），不可撤回
 * 状态5=上架：零售商/管理员 可操作（推进到销售完成），不可撤回
 * 状态6=已销售：流程结束
 *
 * 只有管理员可以执行"返回上一步"操作
 */
@Slf4j
@Service
public class RolePermissionService {

    /**
     * 检查是否有权限创建养殖群
     */
    public boolean canCreateBatch(String role) {
        return "ADMIN".equals(role) || "FARM".equals(role);
    }

    /**
     * 检查是否有权限执行出栏操作（状态0→1）
     * 规则：管理员可操作所有批次；养殖场只能操作自己farmId的批次
     * 出栏时必须选择目标加工厂
     */
    public boolean canSlaughter(String role, String batchFarmId, String userFarmId) {
        if ("ADMIN".equals(role)) return true;
        if ("FARM".equals(role)) {
            return batchFarmId != null && batchFarmId.equals(userFarmId);
        }
        return false;
    }

    /**
     * 检查是否有权限启动加工（状态1→2）
     * 规则：管理员可操作；加工厂只能操作已出栏的批次（status>=1）
     */
    public boolean canStartProcess(String role, Integer currentStatus) {
        if ("ADMIN".equals(role)) return true;
        if ("PROCESS".equals(role)) {
            return currentStatus != null && currentStatus >= 1;
        }
        return false;
    }

    /**
     * 检查是否有权限标记加工完成（状态2→3）
     * 规则：只有管理员和加工厂可以标记加工完成
     * 标记完成时必须指定目标销售商
     */
    public boolean canMarkProcessed(String role) {
        return "ADMIN".equals(role) || "PROCESS".equals(role);
    }

    /**
     * 检查是否有权限送至零售商（状态3→4）
     * 规则：只有管理员和加工厂可以送至零售商
     */
    public boolean canToRetailer(String role) {
        return "ADMIN".equals(role) || "PROCESS".equals(role);
    }

    /**
     * 检查是否有权限上架（状态4→5）
     * 规则：只有管理员和销售商可以上架
     */
    public boolean canOnShelf(String role) {
        return "ADMIN".equals(role) || "SALES".equals(role);
    }

    /**
     * 检查是否有权限标记已销售（状态5→6）
     * 规则：只有管理员和销售商可以标记已销售
     */
    public boolean canSales(String role) {
        return "ADMIN".equals(role) || "SALES".equals(role);
    }

    /**
     * 检查是否有权限撤回/返回上一步
     * 规则：只有管理员可以撤回任何步骤
     * 非管理员角色（养殖场/加工厂/零售商）不可撤回
     */
    public boolean canRollback(String role) {
        return "ADMIN".equals(role);
    }

    /**
     * 检查是否有权限添加饲料/成长/疫苗记录
     * 规则：管理员可操作所有批次；养殖场只能操作自己farmId的批次
     */
    public boolean canAddRecord(String role, String batchFarmId, String userFarmId) {
        if ("ADMIN".equals(role)) return true;
        if ("FARM".equals(role)) {
            return batchFarmId != null && batchFarmId.equals(userFarmId);
        }
        return false;
    }

    /**
     * 检查是否有权限添加副产品
     * 规则：管理员可操作；养殖场可操作自己farmId的批次；加工厂可操作已出栏的批次
     */
    public boolean canAddByProduct(String role, String batchFarmId, String userFarmId, Integer batchStatus) {
        if ("ADMIN".equals(role)) return true;
        if ("FARM".equals(role)) {
            return batchFarmId != null && batchFarmId.equals(userFarmId);
        }
        if ("PROCESS".equals(role)) {
            return batchStatus != null && batchStatus >= 1;
        }
        return false;
    }

    /**
     * 校验批次状态转换是否合法
     * 0=在养 → 1=出栏
     * 1=出栏 → 2=加工中
     * 2=加工中 → 3=加工完成
     * 3=加工完成 → 4=送至零售商
     * 4=送至零售商 → 5=上架
     * 5=上架 → 6=已销售
     */
    public boolean isValidStatusTransition(Integer fromStatus, Integer toStatus) {
        if (fromStatus == null || toStatus == null) return false;
        if (toStatus == 1) return fromStatus == 0;           // 出栏：只能从在养出栏
        if (toStatus == 2) return fromStatus == 1;           // 加工中：只能从出栏进入加工
        if (toStatus == 3) return fromStatus == 2;           // 加工完成：只能从加工中完成
        if (toStatus == 4) return fromStatus == 3;           // 送至零售商：只能从加工完成送至
        if (toStatus == 5) return fromStatus == 4;           // 上架：只能从送至零售商上架
        if (toStatus == 6) return fromStatus == 5;           // 已销售：只能从上架销售
        return false;
    }

    /**
     * 管理员单步回退：当前状态减 1，且仅在流程链上（1..6 → 0..5）
     * 例如：出栏(1)→在养(0)、加工中(2)→出栏(1)、…、已销售(6)→上架(5)
     */
    public boolean isValidAdminRollbackTransition(Integer fromStatus, Integer toStatus) {
        if (fromStatus == null || toStatus == null) return false;
        return fromStatus >= 1 && fromStatus <= 6 && toStatus == fromStatus - 1;
    }
}
