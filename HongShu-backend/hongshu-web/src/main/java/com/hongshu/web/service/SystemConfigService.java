package com.hongshu.web.service;

import com.hongshu.common.core.domain.entity.SystemConfig;
import com.hongshu.web.domain.vo.SystemConfigVO;

import java.util.List;

/**
 * 系统配置表 服务类
 *
 * @author hongshu
 */
public interface SystemConfigService extends SuperService<SystemConfig> {

    /**
     * 获取系统配置
     *
     */
    SystemConfig getConfig();

    /**
     * 通过Key前缀清空Redis缓存
     *
     */
    String cleanRedisByKey(List<String> key);

    /**
     * 修改系统配置
     *
     */
    String editSystemConfig(SystemConfigVO systemConfigVO);

    /**
     * 获取系统配置中的搜索模式
     *
     */
    String getSearchModel();

}
