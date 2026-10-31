package com.hongshu.web.websocket.im;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;

import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "私信消息参数")
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 2405172041950251807L;

    @Schema(description = "发送用户 ID，由后端以当前登录身份覆盖")
    private String sendUid;

    @Schema(description = "接收用户 ID")
    private String acceptUid;

    @Schema(description = "msgType 为 1 时填写文字（1 至 5000 字），为 2 时填写有权使用的图片 URL")
    private Object content;

    @Schema(description = "消息类型：1 文字，2 图片；0 和 5 为服务端实时通知类型")
    private Integer msgType;

    @Schema(description = "聊天类型：0 一对一私信，当前不支持其他类型")
    private Integer chatType;
}
