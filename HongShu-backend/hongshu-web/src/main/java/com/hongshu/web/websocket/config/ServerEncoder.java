package com.hongshu.web.websocket.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hongshu.web.websocket.im.Message;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

/**
 * WebSocket JSON 编码器，序列化失败交由连接投递层统一处理。
 *
 * @author hongshu
 */
public class ServerEncoder implements Encoder.Text<Message> {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    @Override
    public void destroy() {
        // 编码器不持有需要关闭的资源。
    }

    @Override
    public void init(EndpointConfig config) {
        // 使用默认 JSON 配置。
    }

    @Override
    public String encode(Message message) throws EncodeException {
        try {
            return JSON_MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new EncodeException(message, "实时消息序列化失败", e);
        }
    }
}
