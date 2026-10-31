package com.hongshu.web.service.impl;

import com.hongshu.common.constant.Constantss;
import com.hongshu.common.global.RedisConf;
import com.hongshu.common.utils.DateUtilss;
import com.hongshu.common.utils.JsonUtils;
import com.hongshu.common.utils.RedisUtil;
import com.hongshu.web.mapper.SysLoginInforMapper;
import com.hongshu.web.mapper.SysVisitMapper;
import com.hongshu.web.service.ISysVisitService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 会员信息 服务层处理
 *
 * @author hongshu
 */
@Service
public class SysVisitServiceImpl implements ISysVisitService {

    private static final DateTimeFormatter DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    private final RedisUtil redisUtil;
    private final SysVisitMapper visitMapper;
    private final SysLoginInforMapper loginInforMapper;

    public SysVisitServiceImpl(RedisUtil redisUtil, SysVisitMapper visitMapper, SysLoginInforMapper loginInforMapper) {
        this.redisUtil = redisUtil;
        this.visitMapper = visitMapper;
        this.loginInforMapper = loginInforMapper;
    }

    @Override
    public int getWebVisitCount() {
        // 获取今日开始和结束时间
        String startTime = DateUtilss.getToDayStartTime();
        String endTime = DateUtilss.getToDayEndTime();
        return loginInforMapper.getIpCount(startTime, endTime);
    }

    @Override
    public Map<String, Object> getVisitByWeek() {
        ZonedDateTime now = ZonedDateTime.now();
        LocalDate today = now.toLocalDate();
        ZonedDateTime nextDay = today.plusDays(1).atStartOfDay(now.getZone());
        // 按日期隔离缓存，跨零点的旧请求不会覆盖新一天的数据。
        String cacheKey = RedisConf.DASHBOARD + Constantss.SYMBOL_COLON + RedisConf.WEEK_VISIT
                + Constantss.SYMBOL_COLON + today;
        String weekVisitJson = redisUtil.get(cacheKey);
        if (weekVisitJson != null) {
            Map<String, Object> cached = JsonUtils.jsonToMap(weekVisitJson);
            if (cached != null) {
                return cached;
            }
        }

        // 查询边界、统计日期和展示标签使用同一个日期基准。
        String todayEndTime = today + " 23:59:59";
        String sevenDays = today.minusDays(6) + " 00:00:00";
        List<LocalDate> sevenDaysList = today.minusDays(6).datesUntil(today.plusDays(1)).toList();
        // 获得最近七天的访问量
        List<Map<String, Object>> pvMap = visitMapper.getPVByWeek(sevenDays, todayEndTime);
        // 获得最近七天的独立用户
        List<Map<String, Object>> uvMap = visitMapper.getUVByWeek(sevenDays, todayEndTime);

        Map<String, Object> countPVMap = new HashMap<>();
        Map<String, Object> countUVMap = new HashMap<>();

        for (Map<String, Object> item : pvMap) {
            countPVMap.put(item.get("DATE").toString(), item.get("COUNT"));
        }
        for (Map<String, Object> item : uvMap) {
            countUVMap.put(item.get("DATE").toString(), item.get("COUNT"));
        }
        // 访问量数组
        List<Integer> pvList = new ArrayList<>();
        // 独立用户数组
        List<Integer> uvList = new ArrayList<>();

        for (LocalDate day : sevenDaysList) {
            Number pvNumber = (Number) countPVMap.getOrDefault(day.toString(), 0);
            Number uvNumber = (Number) countUVMap.getOrDefault(day.toString(), 0);
            pvList.add(pvNumber.intValue());
            uvList.add(uvNumber.intValue());
        }
        Map<String, Object> resultMap = new HashMap<>(Constantss.NUM_THREE);
        // 不含年份的数组格式
        List<String> resultSevenDaysList = sevenDaysList.stream().map(DATE_LABEL_FORMAT::format).toList();
        resultMap.put("date", resultSevenDaysList);
        resultMap.put("pv", pvList);
        resultMap.put("uv", uvList);

        // 最多缓存十分钟，并在下一个零点到期；跨日完成的查询不再写缓存。
        long ttlMillis = Math.min(TimeUnit.MINUTES.toMillis(10), Duration.between(Instant.now(), nextDay.toInstant()).toMillis());
        if (ttlMillis > 0) {
            redisUtil.setEx(cacheKey, JsonUtils.objectToJson(resultMap), ttlMillis, TimeUnit.MILLISECONDS);
        }
        return resultMap;
    }
}
