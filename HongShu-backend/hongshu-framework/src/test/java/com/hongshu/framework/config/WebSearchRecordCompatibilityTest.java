package com.hongshu.framework.config;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.dto.EsRecordDTO;
import com.hongshu.web.domain.entity.WebSearchRecord;
import com.hongshu.web.mapper.WebSearchRecordMapper;
import com.hongshu.web.service.impl.WebEsRecordServiceImpl;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSearchRecordCompatibilityTest {

    private WebSearchRecordMapper mapper;
    private WebEsRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(WebSearchRecordMapper.class);
        service = new WebEsRecordServiceImpl(mapper);
        AuthContextHolder.setUserId("regression-user");
    }

    @AfterEach
    void clearUser() {
        AuthContextHolder.removeUserId();
    }

    @Test
    void newKeywordStartsAtOneAndUsesOneSqlLimitWithoutRowBounds() {
        when(mapper.selectList(ArgumentMatchers.notNull())).thenReturn(Collections.emptyList());

        service.addRecord(keyword("  java  "));

        ArgumentCaptor<WebSearchRecord> inserted = ArgumentCaptor.forClass(WebSearchRecord.class);
        verify(mapper).insert(inserted.capture());
        assertEquals("java", inserted.getValue().getContent());
        assertEquals("regression-user", inserted.getValue().getUid());
        assertEquals(1L, inserted.getValue().getSearchCount());
        assertNotNull(inserted.getValue().getCreateTime());
        assertNotNull(inserted.getValue().getUpdateTime());
        assertNotNull(inserted.getValue().getId());
        assertSingleLimitQuery();
    }

    @Test
    void repeatedKeywordIncrementsTheSelectedRowWithoutRejectingHistoricalDuplicates() {
        WebSearchRecord selected = record("first", 4L);
        WebSearchRecord duplicate = record("duplicate", 8L);
        when(mapper.selectList(ArgumentMatchers.notNull())).thenReturn(Arrays.asList(selected, duplicate));

        service.addRecord(keyword("java"));

        verify(mapper).updateById(selected);
        verify(mapper, never()).updateById(duplicate);
        verify(mapper, never()).insert(any(WebSearchRecord.class));
        assertEquals(5L, selected.getSearchCount());
        assertEquals(8L, duplicate.getSearchCount());
        assertNotNull(selected.getUpdateTime());
        assertSingleLimitQuery();
    }

    @Test
    void legacyNullCountContinuesFromZero() {
        WebSearchRecord selected = record("legacy", null);
        when(mapper.selectList(ArgumentMatchers.notNull())).thenReturn(Collections.singletonList(selected));

        service.addRecord(keyword("java"));

        assertEquals(1L, selected.getSearchCount());
        verify(mapper).updateById(selected);
    }

    private void assertSingleLimitQuery() {
        ArgumentCaptor<Wrapper<WebSearchRecord>> wrapper = ArgumentCaptor.captor();
        verify(mapper).selectList(wrapper.capture());
        assertTrue(wrapper.getValue().getSqlSegment().endsWith("limit 1"));
        verify(mapper, never()).selectOne(ArgumentMatchers.notNull());
        verify(mapper, never()).selectOne(ArgumentMatchers.notNull(), anyBoolean());
        verify(mapper, never()).selectList(ArgumentMatchers.notNull(), any(RowBounds.class));
    }

    private EsRecordDTO keyword(String keyword) {
        EsRecordDTO dto = new EsRecordDTO();
        dto.setKeyword(keyword);
        return dto;
    }

    private WebSearchRecord record(String id, Long count) {
        WebSearchRecord record = new WebSearchRecord();
        record.setId(id);
        record.setUid("regression-user");
        record.setContent("java");
        record.setSearchCount(count);
        return record;
    }
}
