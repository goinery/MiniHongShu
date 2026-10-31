package com.hongshu.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.dialect.helper.MySqlDialect;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 两套分页插件在同一 JSqlParser 运行时执行真实 SQL 改写，防止只编译成功但分页报链接错误。
 */
class PaginationCompatibilityTest {

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    @Test
    void pageHelperCountsFilteredAndGroupedQueriesWithTheSelectedParser() {
        MySqlDialect dialect = new MySqlDialect();
        dialect.setProperties(new Properties());
        try (com.github.pagehelper.Page<Object> ignored = PageHelper.startPage(1, 10)) {

            assertEquals("SELECT count(0) FROM web_note n WHERE n.status = 0",
                    pageHelperCount(dialect,
                            "select n.uid, n.title from web_note n where n.status = 0 order by n.create_time desc"));
            String grouped = pageHelperCount(dialect,
                    "select u.user_id, count(n.uid) total from sys_user u left join web_note n "
                            + "on n.user_id = u.user_id group by u.user_id");
            assertTrue(grouped.contains("GROUP BY u.user_id"), grouped);
            assertTrue(grouped.startsWith("SELECT count(0) FROM ("), grouped);
            assertTrue(pageHelperCount(dialect, "select distinct status from web_note where title like ?")
                    .contains("SELECT DISTINCT status"));
        }
    }

    @Test
    void mybatisPlusUsesTheSameParserForCountAndOrdering() {
        PaginationInnerInterceptor interceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        assertEquals("SELECT COUNT(*) AS total FROM web_note n WHERE n.status = 0",
                interceptor.autoCountSql(new Page<>(1, 10),
                        "select n.uid, n.title from web_note n where n.status = 0 order by n.create_time desc"));
        assertTrue(interceptor.autoCountSql(new Page<>(1, 10),
                "select status, count(*) total from web_note group by status").contains("GROUP BY status"));
        assertEquals("SELECT uid FROM web_note ORDER BY uid DESC",
                interceptor.concatOrderBy("select uid from web_note",
                        Collections.singletonList(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("uid"))));
    }

    private String pageHelperCount(MySqlDialect dialect, String sql) {
        BoundSql boundSql = new BoundSql(new Configuration(), sql, Collections.emptyList(), null);
        return dialect.getCountSql(null, boundSql, null, RowBounds.DEFAULT, new CacheKey());
    }
}
