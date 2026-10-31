package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.mapper.WebNoteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 笔记可见性校验及写操作所需的行锁边界。 */
@Service
public class NoteAccessService {

    private static final String AUDIT_APPROVED = "1";

    private final WebNoteMapper notes;

    public NoteAccessService(WebNoteMapper notes) {
        this.notes = notes;
    }

    public WebNote requireReadable(String id) {
        return requireReadable(notes.selectById(id));
    }

    /** 调用方先开启事务，统一按笔记、评论的顺序加锁。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public WebNote requireReadableForUpdate(String id) {
        WebNote note = notes.selectList(new QueryWrapper<WebNote>().eq("id", id).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        return requireReadable(note);
    }

    private WebNote requireReadable(WebNote note) {
        String currentUserId = AuthContextHolder.getUserId();
        if (note == null || (!AUDIT_APPROVED.equals(note.getAuditStatus())
                && (currentUserId == null || !currentUserId.equals(note.getUid())))) {
            throw new HongshuException("笔记不存在或暂不可见", 404);
        }
        return note;
    }
}
