package com.hongshu.common.exception.file;

import java.io.Serial;

import com.hongshu.common.exception.base.BaseException;

/**
 * 文件信息异常类
 *
 * @author hongshu
 */
public class FileException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FileException(String code, Object[] args) {
        super("file", code, args, null);
    }

}
