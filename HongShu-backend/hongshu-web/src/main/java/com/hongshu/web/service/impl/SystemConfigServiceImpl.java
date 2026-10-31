package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.core.domain.entity.SystemConfig;
import com.hongshu.common.enums.EOpenStatus;
import com.hongshu.common.enums.EStatus;
import com.hongshu.common.exception.exceptionType.QueryException;
import com.hongshu.common.global.*;
import com.hongshu.common.utils.JsonUtils;
import com.hongshu.common.utils.RedisUtil;
import com.hongshu.common.utils.ResultUtil;
import com.hongshu.common.utils.StringUtilss;
import com.hongshu.web.domain.vo.SystemConfigVO;
import com.hongshu.web.mapper.SystemConfigMapper;
import com.hongshu.web.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置关系表 服务实现类
 *
 * @author hongshu
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends SuperServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private final RedisUtil redisUtil;

    public SystemConfigServiceImpl(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public SystemConfig getConfig() {
        // 从Redis中获取系统配置
        String systemConfigJson = redisUtil.get(RedisConf.SYSTEM_CONFIG);
        if (StringUtilss.isEmpty(systemConfigJson)) {
            QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
            queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
            queryWrapper.last(SysConf.LIMIT_ONE);
            // 保留只读取最新一条，避免 getOne 的 RowBounds 与显式 LIMIT 叠加。
            List<SystemConfig> configs = this.list(queryWrapper);
            SystemConfig systemConfig = configs.isEmpty() ? null : configs.getFirst();
            if (systemConfig == null) {
                throw new QueryException(MessageConf.SYSTEM_CONFIG_IS_NOT_EXIST);
            } else {
                // 将系统配置存入Redis中【设置过期时间24小时】
                redisUtil.setEx(RedisConf.SYSTEM_CONFIG, JsonUtils.objectToJson(systemConfig), 24, TimeUnit.HOURS);
            }
            return systemConfig;
        } else {
            SystemConfig systemConfig = JsonUtils.jsonToPojo(systemConfigJson, SystemConfig.class);
            if (systemConfig == null) {
                throw new QueryException(ErrorCode.QUERY_DEFAULT_ERROR, "系统配置转换错误，请检查系统配置，或者清空Redis后重试！");
            }
            return systemConfig;
        }
    }

    @Override
    public String cleanRedisByKey(List<String> key) {
        if (key == null) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.OPERATION_FAIL);
        }
        key.forEach(item -> {
            // 表示清空所有key
            if (RedisConf.ALL.equals(item)) {
                Set<String> keys = redisUtil.keys(Constants.SYMBOL_STAR);
                redisUtil.delete(keys);
            } else {
                // 获取Redis中特定前缀
                if (item == null || !item.matches("[a-zA-Z0-9:_-]+"))
                    throw new IllegalArgumentException("缓存前缀无效");
                Set<String> keys = redisUtil.keys(item + Constants.SYMBOL_STAR);
                redisUtil.delete(keys);
            }
        });
        return ResultUtil.successWithMessage(MessageConf.OPERATION_SUCCESS);
    }

    @Override
    public String editSystemConfig(SystemConfigVO systemConfigVO) {
        // 开启Email邮件通知时，必须保证Email字段不为空
        if (EOpenStatus.OPEN.equals(systemConfigVO.getStartEmailNotification()) && StringUtilss.isEmpty(systemConfigVO.getEmail())) {
            return ResultUtil.errorWithMessage(MessageConf.MUST_BE_SET_EMAIL);
        }
        if (StringUtilss.isEmpty(systemConfigVO.getUid())) {
            SystemConfig systemConfig = new SystemConfig();
            // 更新邮箱与网站配置；文件存储由后端环境变量统一管理
            BeanUtils.copyProperties(systemConfigVO, systemConfig, SysConf.STATUS);
            systemConfig.insert();
        } else {
            SystemConfig systemConfig = this.getById(systemConfigVO.getUid());
            if (systemConfig == null) {
                return ResultUtil.errorWithMessage("系统配置不存在，请刷新后重试");
            }

            // 更新邮箱与网站配置；文件存储由后端环境变量统一管理
            BeanUtils.copyProperties(systemConfigVO, systemConfig, SysConf.STATUS, SysConf.UID);
            systemConfig.updateById();

        }
        // 更新系统配置成功后，需要删除Redis中的系统配置
        redisUtil.delete(RedisConf.SYSTEM_CONFIG);
        return ResultUtil.successWithMessage(MessageConf.UPDATE_SUCCESS);
    }

    @Override
    public String getSearchModel() {
        SystemConfig systemConfig = this.getConfig();
        return systemConfig.getSearchModel();
    }
}
