package com.hongshu.framework.config;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hongshu.common.core.domain.entity.SystemConfig;
import com.hongshu.common.exception.exceptionType.QueryException;
import com.hongshu.common.global.RedisConf;
import com.hongshu.common.utils.JsonUtils;
import com.hongshu.common.utils.RedisUtil;
import com.hongshu.web.mapper.SystemConfigMapper;
import com.hongshu.web.service.impl.SystemConfigServiceImpl;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemConfigCompatibilityTest {

    private SystemConfigMapper mapper;
    private RedisUtil redis;
    private SystemConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SystemConfigMapper.class);
        redis = mock(RedisUtil.class);
        service = new SystemConfigServiceImpl(redis);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void cacheMissKeepsTheNewestSingleRecordAndCachesItFor24Hours() {
        SystemConfig latest = new SystemConfig();
        latest.setUid("latest");
        SystemConfig older = new SystemConfig();
        older.setUid("older");
        when(mapper.selectList(ArgumentMatchers.notNull())).thenReturn(Arrays.asList(latest, older));

        assertSame(latest, service.getConfig());

        ArgumentCaptor<Wrapper<SystemConfig>> query = ArgumentCaptor.captor();
        verify(mapper).selectList(query.capture());
        assertTrue(query.getValue().getSqlSegment().contains("ORDER BY create_time DESC"));
        assertTrue(query.getValue().getSqlSegment().toLowerCase().endsWith("limit 1"));
        verify(mapper, never()).selectOne(ArgumentMatchers.notNull(), anyBoolean());
        verify(mapper, never()).selectList(ArgumentMatchers.notNull(), any(RowBounds.class));
        verify(redis).setEx(eq(RedisConf.SYSTEM_CONFIG), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void missingConfigurationStillReportsTheExistingBusinessError() {
        when(mapper.selectList(ArgumentMatchers.notNull())).thenReturn(Collections.emptyList());
        assertThrows(QueryException.class, () -> service.getConfig());
        verify(redis, never()).setEx(anyString(), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void cacheHitKeepsTheExistingSerializationAndSkipsTheDatabase() {
        SystemConfig cached = new SystemConfig();
        cached.setUid("cached");
        when(redis.get(RedisConf.SYSTEM_CONFIG)).thenReturn(JsonUtils.objectToJson(cached));
        assertEquals("cached", service.getConfig().getUid());
        verifyNoInteractions(mapper);
    }
}
