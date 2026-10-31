package com.hongshu.common.constant;


import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 消息通知数量设置
 *
 * @author hongshu
 */
@Schema(description = "消息通知数量")
public interface ImConstant {

    @Schema(description = "消息通知数量")
    String MESSAGE_COUNT_KEY = "messageCountKey:";
}
