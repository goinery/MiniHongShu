package com.hongshu.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongshu.web.domain.entity.WebNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 笔记信息 数据层
 *
 * @author hongshu
 */
@Mapper
public interface SysNoteMapper extends BaseMapper<WebNote> {

    /**
     * 获取一年内的笔记贡献数
     *
     */
    @Select("SELECT DISTINCT DATE_FORMAT(create_time, '%Y-%m-%d') DATE, COUNT(uid) COUNT FROM web_note WHERE status = 1 AND create_time >= #{startTime} AND create_time < #{endTime} GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> getNoteContributeCount(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 通过标签获取博客数量
     *
     */
    @Select("SELECT cpid, COUNT(cpid) as count FROM  web_note where status = 1 GROUP BY cpid")
    List<Map<String, Object>> getNoteCountByCategory();

    @Select("SELECT CAST(t.id AS CHAR) AS tagUid, t.title AS name, COUNT(DISTINCT n.id) AS value FROM web_tag t LEFT JOIN web_tag_note_relation r ON r.tid = t.id LEFT JOIN web_note n ON n.id = r.nid AND n.status = 1 GROUP BY t.id, t.title ORDER BY value DESC, t.id")
    List<Map<String, Object>> getNoteCountByTag();
}
