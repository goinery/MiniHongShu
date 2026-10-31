package com.hongshu.web.service.impl;

import com.hongshu.web.auth.WebPasswords;
import com.hongshu.web.auth.WebSessionService;
import com.hongshu.common.exception.web.HongshuException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.core.domain.Query;
import com.hongshu.common.constant.AuthConstant;
import com.hongshu.common.global.BaseSQLConf;
import com.hongshu.common.utils.RandomUtil;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.common.utils.ip.AddressUtils;
import com.hongshu.common.utils.ip.IpUtils;
import com.hongshu.common.validator.ValidatorUtil;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.mapper.SysMemberMapper;
import com.hongshu.web.service.ISysMemberService;
import com.hongshu.web.service.IWebOssService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 会员信息 服务层处理
 *
 * @author hongshu
 */
@Slf4j
@Service
public class SysMemberServiceImpl implements ISysMemberService {

    private final SysMemberMapper memberMapper;
    private final com.hongshu.web.service.FileAssetService assets;
    private final IWebOssService ossService;
    private final WebSessionService sessions;

    public SysMemberServiceImpl(SysMemberMapper memberMapper, com.hongshu.web.service.FileAssetService assets, IWebOssService ossService, WebSessionService sessions) {
        this.memberMapper = memberMapper;
        this.assets = assets;
        this.ossService = ossService;
        this.sessions = sessions;
    }

    /**
     * 查询会员信息集合
     *
     * @param query 会员信息
     */
    @Override
    public List<WebUser> selectMemberList(Query query) {
        QueryWrapper<WebUser> qw = new QueryWrapper<>();
        qw.lambda().like(ValidatorUtil.isNotNull(query.getHsId()), WebUser::getHsId, query.getHsId());
        qw.lambda().like(ValidatorUtil.isNotNull(query.getUsername()), WebUser::getUsername, query.getUsername());
        qw.lambda().like(ValidatorUtil.isNotNull(query.getPhone()), WebUser::getPhone, query.getPhone());
        qw.lambda().like(ValidatorUtil.isNotNull(query.getStatus()), WebUser::getStatus, query.getStatus());
        qw.lambda().ge(ValidatorUtil.isNotNull(query.get("params[beginTime]")), WebUser::getCreateTime, query.get("params[beginTime]"));
        qw.lambda().le(ValidatorUtil.isNotNull(query.get("params[endTime]")), WebUser::getCreateTime, query.get("params[endTime]"));
        qw.orderByDesc("create_time");
        List<WebUser> list = memberMapper.selectList(qw);
        list.forEach(this::hidePassword);
        return list;
    }

    /**
     * 查询所有会员
     */
    @Override
    public List<WebUser> selectMemberAll() {
        return memberMapper.selectList(new QueryWrapper<>());
    }

    /**
     * 通过会员ID查询会员信息
     *
     * @param id 会员ID
     */
    @Override
    public WebUser selectMemberById(Long id) {
        WebUser user = memberMapper.selectById(id);
        hidePassword(user);
        return user;
    }

    /**
     * 新增会员信息
     *
     * @param user 会员信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertMember(WebUser user, MultipartFile file) {
        // 上传头像
        if (ObjectUtils.isNotEmpty(file)) {
            String avatar = ossService.save(file);
            user.setAvatar(avatar);
            if (user.getId() != null) assets.assignOwner(avatar, user.getId());
        } else if (user.getAvatar() == null || user.getAvatar().isBlank()) {
            user.setAvatar(AuthConstant.DEFAULT_AVATAR);
        }
        user.setHsId(Long.valueOf(RandomUtil.randomNumbers(10)));
        user.setPassword(WebPasswords.encode(user.getPassword()));
        user.setLoginIp(IpUtils.getIpAddr());
        user.setAddress(AddressUtils.getRealAddressByIP(user.getLoginIp()));
        user.setCreator("System");
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        int inserted = memberMapper.insert(user);
        if (inserted != 1) throw new HongshuException("会员保存失败");
        if (ObjectUtils.isNotEmpty(file)) assets.assignOwner(user.getAvatar(), user.getId());
        return inserted;
    }

    /**
     * 修改保存会员信息
     *
     * @param user 会员信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateMember(WebUser user, MultipartFile file) {
        WebUser webUser = memberMapper.selectList(new QueryWrapper<WebUser>().eq("id", user.getId()).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        if (webUser == null) throw new HongshuException("用户不存在");
        // 更新头像
        if (ObjectUtils.isNotEmpty(file)) {
            String avatar = ossService.save(file);
            user.setAvatar(avatar);
            if (user.getId() != null) assets.assignOwner(avatar, user.getId());
        }
        String submitted = user.getPassword();
        if (StringUtils.isEmpty(submitted) || submitted.equals(webUser.getPassword())) {
            user.setPassword(webUser.getPassword());
        } else {
            user.setPassword(WebPasswords.encode(submitted));
        }
        user.setLoginIp(IpUtils.getIpAddr());
        user.setAddress(AddressUtils.getRealAddressByIP(user.getLoginIp()));
        user.setUpdater("System");
        user.setUpdateTime(new Date());
        int updated = memberMapper.updateById(user);
        if (updated != 1) throw new HongshuException("会员保存失败");
        assets.cleanStoredReferencesAfterCommit(webUser.getAvatar());
        if (((!StringUtils.isEmpty(user.getStatus()) && !"0".equals(user.getStatus()))
                || !java.util.Objects.equals(user.getPassword(), webUser.getPassword()))) sessions.revoke(user.getId());
        return updated;
    }

    /**
     * 删除会员信息
     *
     * @param id 会员ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMemberById(Long id) {
        return deleteMemberByIds(new Long[]{id});
    }

    /**
     * 批量删除会员信息
     *
     * @param ids 需要删除的会员ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMemberByIds(Long[] ids) {
        if (ids == null || ids.length == 0) return 0;
        List<WebUser> users = memberMapper.selectList(new QueryWrapper<WebUser>().in("id", Arrays.asList(ids)).last("FOR UPDATE"));
        if (users.isEmpty()) return 0;
        List<String> userIds = users.stream().map(WebUser::getId).toList();
        userIds.forEach(sessions::revoke);
        int deleted = memberMapper.deleteByIds(userIds);
        for (WebUser user : users) {
            assets.cleanStoredReferencesAfterCommit(user.getAvatar());
        }
        return deleted;
    }

    @Override
    public Integer getMemberCount(int status) {
        QueryWrapper<WebUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(BaseSQLConf.STATUS, status);
        return Math.toIntExact(memberMapper.selectCount(queryWrapper));
    }

    private void hidePassword(WebUser user) {
        if (user != null) {
            user.setPassword(null);
        }
    }
}
