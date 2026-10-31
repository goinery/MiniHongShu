package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.utils.ConvertUtils;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.auth.WebUserViews;
import com.hongshu.web.auth.WebPasswords;
import com.hongshu.web.auth.WebSessionService;
import com.hongshu.web.domain.dto.ChangePasswordDTO;
import com.hongshu.web.domain.entity.WebLikeOrCollect;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.vo.NoteSearchVO;
import com.hongshu.web.mapper.WebLikeOrCollectMapper;
import com.hongshu.web.mapper.WebNoteMapper;
import com.hongshu.web.mapper.WebUserMapper;
import com.hongshu.web.service.IWebUserService;
import com.hongshu.web.service.WebClientLocation;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

/**
 * 用户
 *
 * @author hongshu
 */
@Service
public class WebUserServiceImpl extends ServiceImpl<WebUserMapper, WebUser> implements IWebUserService {

    private final WebUserMapper userMapper;
    private final com.hongshu.web.service.FileAssetService assets;
    private final com.hongshu.web.service.IWebEsRecordService searchRecords;
    private final WebNoteMapper noteMapper;
    private final WebLikeOrCollectMapper likeOrCollectionMapper;
    private final WebSessionService sessions;

    public WebUserServiceImpl(WebUserMapper userMapper, com.hongshu.web.service.FileAssetService assets, com.hongshu.web.service.IWebEsRecordService searchRecords, WebNoteMapper noteMapper, WebLikeOrCollectMapper likeOrCollectionMapper, WebSessionService sessions) {
        this.userMapper = userMapper;
        this.assets = assets;
        this.searchRecords = searchRecords;
        this.noteMapper = noteMapper;
        this.likeOrCollectionMapper = likeOrCollectionMapper;
        this.sessions = sessions;
    }

    /**
     * 获取当前用户信息
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     * @param userId      用户ID
     * @param type        类型
     */
    @Override
    public Page<NoteSearchVO> getTrendByUser(long currentPage, long pageSize, String userId, Integer type) {
        if (StringUtils.isBlank(userId) || userMapper.selectById(userId) == null)
            throw new HongshuException("用户不存在", 404);
        int kind = type == null ? 1 : type;
        if (kind < 1 || kind > 3) throw new HongshuException("笔记类型无效");
        String currentUid = AuthContextHolder.getUserId();
        QueryWrapper<WebNote> query = new QueryWrapper<>();
        if (kind == 1) query.eq("uid", userId).orderByDesc("pinned", "update_time");
        else
            query.apply("EXISTS (SELECT 1 FROM web_like_or_collect l WHERE l.like_or_collection_id = web_note.id AND l.uid = {0} AND l.type = {1})", userId, kind == 2 ? 1 : 3).orderByDesc("update_time");
        query.and(w -> {
            w.eq("audit_status", "1");
            if (StringUtils.isNotBlank(currentUid)) w.or().eq("uid", currentUid);
        });
        Page<WebNote> page = noteMapper.selectPage(new Page<>(Math.max(1, currentPage), Math.clamp(pageSize, 1, 100)), query);
        Page<NoteSearchVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (page.getRecords().isEmpty()) return result;
        Set<String> uids = page.getRecords().stream().map(WebNote::getUid).collect(Collectors.toSet());
        Map<String, WebUser> authors = userMapper.selectByIds(uids).stream().collect(Collectors.toMap(WebUser::getId, user -> user));
        Set<String> likes = StringUtils.isBlank(currentUid) ? Collections.emptySet() : likeOrCollectionMapper.selectList(
                new QueryWrapper<WebLikeOrCollect>().eq("uid", currentUid).eq("type", 1)).stream().map(WebLikeOrCollect::getLikeOrCollectionId).collect(Collectors.toSet());
        result.setRecords(page.getRecords().stream().map(note -> {
            NoteSearchVO vo = ConvertUtils.sourceToTarget(note, NoteSearchVO.class);
            WebUser author = authors.get(note.getUid());
            if (author != null) vo.setUsername(author.getUsername()).setAvatar(author.getAvatar());
            vo.setIsLike(likes.contains(note.getId())).setTime(note.getUpdateTime() == null ? 0L : note.getUpdateTime().getTime());
            if (!Objects.equals(currentUid, note.getUid())) vo.setViewCount(null);
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public WebUser getUserById(String userId) {
        WebUser user = userMapper.selectById(userId);
        if (ObjectUtils.isEmpty(user)) {
            throw new HongshuException("用户不存在", 404);
        }
        QueryWrapper<WebNote> noteQuery = new QueryWrapper<WebNote>().eq("uid", userId);
        String currentUserId = AuthContextHolder.getUserId();
        if (!Strings.CS.equals(currentUserId, userId)) {
            noteQuery.eq("audit_status", "1");
        }
        Long noteCount = noteMapper.selectCount(noteQuery);
        user.setTrendCount(noteCount == null ? 0L : noteCount);
        if (Objects.equals(currentUserId, user.getId())) user.setAddress(WebClientLocation.current());
        return Objects.equals(currentUserId, user.getId())
                ? WebUserViews.selfProfile(user) : WebUserViews.publicProfile(user);
    }

    /**
     * 更新用户信息
     *
     * @param user 用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebUser updateUser(WebUser user) {
        String currentUid = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(currentUid)) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        // selectOne 会通过 RowBounds 追加 LIMIT；主键行锁查询使用无分页的 selectList。
        WebUser webUser = userMapper.selectList(new QueryWrapper<WebUser>().eq("id", currentUid).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        if (ObjectUtils.isEmpty(webUser)) {
            throw new HongshuException(ResultCodeEnum.FAIL);
        }
        validateProfile(user, currentUid);
        String oldAvatar = webUser.getAvatar();
        if (StringUtils.isNotBlank(user.getAvatar())) {
            if (!Objects.equals(user.getAvatar(), webUser.getAvatar())) assets.requireUsable(user.getAvatar());
            webUser.setAvatar(user.getAvatar());
        }
        if (StringUtils.isNotBlank(user.getUsername())) {
            webUser.setUsername(user.getUsername().trim());
        }
        if (user.getDescription() != null) {
            webUser.setDescription(user.getDescription());
        }
        if (user.getTags() != null) {
            webUser.setTags(user.getTags());
        }
        if (user.getGender() != null) {
            webUser.setGender(user.getGender());
        }
        if (user.getBirthday() != null) {
            webUser.setBirthday(user.getBirthday());
        }
        webUser.setAddress(WebClientLocation.current());
        if (user.getPhone() != null) {
            webUser.setPhone(user.getPhone().trim());
        }
        if (user.getEmail() != null) {
            webUser.setEmail(user.getEmail().trim());
        }
        webUser.setUpdateTime(new Date());
        if (userMapper.updateById(webUser) != 1) throw new HongshuException("资料保存失败，请重试");
        assets.cleanStoredReferencesAfterCommit(oldAvatar);
        return WebUserViews.selfProfile(webUser);
    }

    private void validateProfile(WebUser user, String currentUid) {
        if (user.getUsername() != null && (user.getUsername().isBlank() || user.getUsername().trim().length() > 50)) {
            throw new HongshuException("昵称不能为空，且不能超过50字");
        }
        if (user.getGender() != null && !Set.of("0", "1", "2").contains(user.getGender())) {
            throw new HongshuException("请选择有效的性别");
        }
        if (StringUtils.isNotBlank(user.getBirthday())) {
            try {
                LocalDate birthday = LocalDate.parse(user.getBirthday());
                if (!birthday.toString().equals(user.getBirthday()) || birthday.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))) {
                    throw new HongshuException("生日必须是有效日期，且不能晚于今天");
                }
            } catch (DateTimeParseException e) {
                throw new HongshuException("生日格式应为 YYYY-MM-DD");
            }
        }
        if (user.getPhone() != null) {
            String phone = user.getPhone().trim();
            if (!phone.matches("1[0-9]{10}")) throw new HongshuException("请输入有效的11位手机号");
            if (userMapper.selectCount(new QueryWrapper<WebUser>().eq("phone", phone).ne("id", currentUid)) > 0) {
                throw new HongshuException("该手机号已被其他账号使用");
            }
        }
        if (user.getEmail() != null) {
            String email = user.getEmail().trim();
            if (!email.isEmpty() && (email.length() > 100 || !cn.hutool.core.lang.Validator.isEmail(email))) {
                throw new HongshuException("请输入有效的邮箱地址，且不能超过100字符");
            }
            if (!email.isEmpty() && userMapper.selectCount(new QueryWrapper<WebUser>().eq("email", email).ne("id", currentUid)) > 0) {
                throw new HongshuException("该邮箱已被其他账号使用");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO passwords) {
        String currentUid = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(currentUid)) throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        WebUser user = userMapper.selectList(new QueryWrapper<WebUser>().eq("id", currentUid).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        sessions.requireEnabled(user);
        if (!WebPasswords.matches(passwords.getOldPassword(), user.getPassword())) {
            throw new HongshuException("原密码不正确");
        }
        if (!Objects.equals(passwords.getNewPassword(), passwords.getConfirmPassword())) {
            throw new HongshuException("两次输入的新密码不一致");
        }
        if (WebPasswords.matches(passwords.getNewPassword(), user.getPassword())) {
            throw new HongshuException("新密码不能与原密码相同");
        }
        String encoded = WebPasswords.encode(passwords.getNewPassword());
        int updated = userMapper.update(null, new UpdateWrapper<WebUser>().eq("id", currentUid)
                .set("password", encoded).set("update_time", new Date()).setSql("create_time = create_time"));
        if (updated != 1) throw new HongshuException("密码修改失败，请重试");
        sessions.revoke(currentUid);
    }

    /**
     * 查找用户信息
     *
     * @param keyword 关键词
     */
    @Override
    public Page<WebUser> getUserByKeyword(long currentPage, long pageSize, String keyword) {
        Page<WebUser> resultPage;
        resultPage = userMapper.selectPage(new Page<>((int) currentPage, (int) pageSize), new QueryWrapper<WebUser>().like("username", keyword));
        resultPage.setRecords(resultPage.getRecords().stream().map(WebUserViews::publicProfile).collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 保存用户的搜索记录
     *
     * @param keyword 关键词
     */
    @Override
    public void saveUserSearchRecord(String keyword) {
        com.hongshu.web.domain.dto.EsRecordDTO dto = new com.hongshu.web.domain.dto.EsRecordDTO();
        dto.setKeyword(keyword);
        searchRecords.addRecord(dto);
    }

    /**
     * 根据条件分页查询角色数据
     *
     * @param user 角色信息
     * @return 角色数据集合信息
     */
    @Override
//    @DataScope(deptAlias = "d")
    public List<WebUser> getUserList(WebUser user) {
        QueryWrapper<WebUser> query = new QueryWrapper<>();
        if (user != null) query.like(StringUtils.isNotBlank(user.getUsername()), "username", user.getUsername());
        List<WebUser> users = userMapper.selectList(query.orderByDesc("create_time"));
        users.replaceAll(WebUserViews::publicProfile);
        return users;
    }

}
