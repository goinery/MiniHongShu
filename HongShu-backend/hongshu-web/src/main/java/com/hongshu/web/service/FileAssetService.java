package com.hongshu.web.service;

import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.service.FileStorage;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.repository.FileAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 管理文件归属与生命周期，提交后回收旧文件，回滚后补偿新上传文件。
 */
@Slf4j
@Service
public class FileAssetService {

    private static final String ADMIN_OWNER = "admin";

    private final FileAssetRepository repository;
    private final FileStorage storage;
    private final LocalFileAssetStorage localStorage;
    private final TransactionTemplate cleanupTransaction;

    public FileAssetService(FileAssetRepository repository, FileStorage storage, LocalFileAssetStorage localStorage,
                            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.storage = storage;
        this.localStorage = localStorage;
        this.cleanupTransaction = new TransactionTemplate(transactionManager);
        this.cleanupTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public String normalize(String value) {
        return localStorage.normalize(value);
    }

    public void register(String path) {
        String uid = AuthContextHolder.getUserId();
        try {
            repository.register(normalize(path), uid == null ? ADMIN_OWNER : uid, storage.getStorageType());
        } catch (RuntimeException e) {
            removeUpload(path);
            throw e;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        removeUpload(path);
                    }
                }
            });
        }
    }

    private void removeUpload(String path) {
        try {
            storage.delete(path);
        } catch (RuntimeException e) {
            log.warn("清理失败上传的文件失败：{}", path, e);
        }
    }

    /**
     * 仅供服务端补偿刚上传但未返回给用户的文件。
     */
    public void discardUpload(String value) {
        try {
            String path = normalize(value);
            if (!repository.isReferenced(path)) {
                deleteFile(path);
            }
        } catch (RuntimeException e) {
            log.warn("清理未完成批次的文件失败：{}", value, e);
        }
    }

    public void assignOwner(String path, String uid) {
        if (uid != null && !uid.isBlank()) {
            repository.assignOwner(normalize(path), uid, ADMIN_OWNER);
        }
    }

    public void requireUsable(String value) {
        String path = normalize(value);
        List<String> owners = repository.findOwners(path);
        if (localStorage.isLocal(path) || !owners.isEmpty()) {
            requireOwner(owners);
        }
    }

    private void requireOwner(List<String> owners) {
        String uid = AuthContextHolder.getUserId();
        if (uid == null || owners.isEmpty() || !uid.equals(owners.getFirst())) {
            throw new HongshuException("只能操作自己上传的文件", 403);
        }
    }

    public void deleteOwned(List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Set<String> paths = new LinkedHashSet<>();
        for (String value : values) {
            String path = normalize(value);
            if (!paths.add(path)) {
                continue;
            }
            requireOwner(repository.findOwners(path));
            if (repository.isReferenced(path)) {
                throw new HongshuException("文件仍在使用中，请先移除相关内容");
            }
        }
        paths.forEach(this::deleteFile);
    }

    public void cleanAfterCommit(Collection<String> values, String owner) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> paths = values.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).distinct().toList();
        Runnable cleanup = () -> paths.forEach(value -> cleanUnusedFile(value, owner));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanupTransaction.executeWithoutResult(status -> cleanup.run());
                }
            });
        } else {
            cleanup.run();
        }
    }

    private void cleanUnusedFile(String value, String owner) {
        try {
            String path = normalize(value);
            List<String> owners = repository.findOwners(path);
            if (!owners.isEmpty() && (owner == null || Objects.equals(owner, owners.getFirst()))
                    && !repository.isReferenced(path)) {
                deleteFile(path);
            }
        } catch (RuntimeException e) {
            log.warn("清理未使用的文件失败：{}", value, e);
        }
    }

    /**
     * 仅用于服务端从数据库读取的旧引用；共享资源在最后一个引用移除后回收。
     */
    public void cleanStoredReferencesAfterCommit(String... previousValues) {
        if (previousValues != null) {
            cleanAfterCommit(Arrays.asList(previousValues), null);
        }
    }

    /**
     * JSON 图片列表或富文本中的候选必须来自已保存内容，不能直接传入客户端待保存内容。
     */
    public void cleanStoredContentAfterCommit(String... previousContents) {
        if (previousContents == null) {
            return;
        }
        Set<String> paths = new LinkedHashSet<>();
        for (String content : previousContents) {
            if (content != null && !content.isBlank()) {
                paths.addAll(repository.findPathsInContent(content));
            }
        }
        cleanAfterCommit(paths, null);
    }

    private void deleteFile(String path) {
        List<Integer> types = repository.findStorageTypes(path);
        if (types.isEmpty()) {
            return;
        }
        Integer storageType = types.getFirst();
        try {
            if (Objects.equals(storageType, LocalFileAssetStorage.STORAGE_TYPE)) {
                localStorage.delete(path);
            } else if (Objects.equals(storageType, storage.getStorageType())) {
                storage.delete(path);
            } else {
                throw new HongshuException("旧存储文件需迁移后才能删除");
            }
            repository.delete(path);
        } catch (IOException e) {
            throw new HongshuException("删除本地文件失败", e);
        }
    }
}
