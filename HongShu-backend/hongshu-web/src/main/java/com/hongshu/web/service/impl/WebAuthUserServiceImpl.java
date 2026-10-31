package com.hongshu.web.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hongshu.common.constant.AuthConstant;
import com.hongshu.common.constant.Constants;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.utils.MessageUtils;
import com.hongshu.common.utils.RedisUtils;
import com.hongshu.common.utils.SecurityUtils;
import com.hongshu.common.utils.ip.IpUtils;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.auth.WebPasswords;
import com.hongshu.web.auth.WebSessionService;
import com.hongshu.web.auth.WebUserViews;
import com.hongshu.web.domain.dto.AuthUserDTO;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.manager.AsyncManager;
import com.hongshu.web.manager.factory.AsyncFactory;
import com.hongshu.web.mapper.WebUserMapper;
import com.hongshu.web.service.IWebAuthUserService;
import com.hongshu.web.service.WebClientLocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.Map;

@Service
public class WebAuthUserServiceImpl extends ServiceImpl<WebUserMapper, WebUser> implements IWebAuthUserService {
    private final RedisUtils redis;
    private final WebSessionService sessions;

    public WebAuthUserServiceImpl(RedisUtils redis, WebSessionService sessions) {
        this.redis = redis;
        this.sessions = sessions;
    }

    @Override
    @Transactional
    public Map<String, Object> login(AuthUserDTO dto) {
        if (dto == null || dto.getPhone() == null || !dto.getPhone().matches("1[0-9]{10}") || StringUtils.isBlank(dto.getPassword())) {
            throw new HongshuException("请输入有效的手机号和密码");
        }
        WebUser user = baseMapper.selectList(new QueryWrapper<WebUser>().eq("phone", dto.getPhone()).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        if (user == null) {
            user = new WebUser();
            user.setPhone(dto.getPhone());
            user.setPassword(WebPasswords.encode(dto.getPassword()));
            user.setHsId(Long.valueOf(RandomUtil.randomNumbers(10)));
            user.setUsername("小红薯");
            user.setAvatar(AuthConstant.DEFAULT_AVATAR);
            user.setStatus("0");
            user.setCreateTime(new Date());
            user.setUpdateTime(user.getCreateTime());
            save(user);
        } else {
            sessions.requireEnabled(user);
            if (!WebPasswords.matches(dto.getPassword(), user.getPassword())) throw new HongshuException(ResultCodeEnum.ERROR_PASSWORD);
            if (WebPasswords.legacy(user.getPassword())) user.setPassword(SecurityUtils.encryptPassword(dto.getPassword()));
        }
        user.setLoginDate(new Date());
        user.setLoginIp(IpUtils.getIpAddr());
        user.setAddress(WebClientLocation.resolve(user.getLoginIp()));
        user.setUpdateTime(new Date());
        updateById(user);
        AsyncManager.me().execute(AsyncFactory.recordLoginInfor(user.getId(), user.getUsername(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        return sessions.login(user);
    }

    @Override
    public Map<String, Object> loginByCode(AuthUserDTO dto) {
        String account = StringUtils.defaultIfBlank(dto.getPhone(), dto.getEmail());
        if (StringUtils.isBlank(account) || StringUtils.isBlank(dto.getCode())) throw new HongshuException(AuthConstant.LOGIN_FAIL);
        String key = AuthConstant.CODE + account;
        String code = redis.get(key);
        if (StringUtils.isBlank(code) || !code.equals(dto.getCode())) throw new HongshuException(AuthConstant.LOGIN_FAIL);
        WebUser user = getOne(new QueryWrapper<WebUser>().eq(StringUtils.isNotBlank(dto.getPhone()) ? "phone" : "email", account));
        sessions.requireEnabled(user);
        redis.delete(key);
        user.setLoginDate(new Date());
        user.setLoginIp(IpUtils.getIpAddr());
        user.setAddress(WebClientLocation.resolve(user.getLoginIp()));
        user.setUpdateTime(new Date());
        updateById(user);
        return sessions.login(user);
    }

    @Override
    public WebUser getUserInfoByToken(String token) {
        WebUser user = sessions.requireUser(token, "access");
        if (!user.getId().equals(AuthContextHolder.getUserId())) throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        user.setAddress(WebClientLocation.current());
        return WebUserViews.selfProfile(user);
    }

    @Override
    public void loginOut(String ignored) {
        String uid = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(uid)) throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        sessions.revoke(uid);
        WebUser user = getById(uid);
        if (user != null) AsyncManager.me().execute(AsyncFactory.recordLoginInfor(uid, user.getUsername(), Constants.LOGOUT, MessageUtils.message("user.logout.success")));
    }

    @Override
    public Map<String, Object> refreshToken(String token) {
        return sessions.refresh(token);
    }
}
