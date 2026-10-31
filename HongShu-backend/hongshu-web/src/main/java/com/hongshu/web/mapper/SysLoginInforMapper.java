package com.hongshu.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongshu.web.domain.entity.WebLoginLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统访问日志情况信息 数据层
 *
 * @author hongshu
 */
@Mapper
public interface SysLoginInforMapper extends BaseMapper<WebLoginLog> {

    /**
     * 获取IP数目
     *
     */
    @Select("SELECT COUNT(ipaddr) FROM (SELECT ipaddr FROM web_login_log WHERE create_time >= #{startTime} AND create_time <= #{endTime} GROUP BY ipaddr) AS tmp")
    Integer getIpCount(@Param("startTime") String startTime, @Param("endTime") String endTime);

    // 清空日志接口明确要求删除整张日志表的数据。
    @Delete("-- noinspection SqlWithoutWhere\nDELETE FROM web_login_log")
    void cleanLoginInfor();

}
