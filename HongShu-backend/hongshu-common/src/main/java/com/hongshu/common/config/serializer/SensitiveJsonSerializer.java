package com.hongshu.common.config.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import com.hongshu.common.annotation.Sensitive;
import com.hongshu.common.core.domain.model.LoginUser;
import com.hongshu.common.enums.DesensitizedType;
import com.hongshu.common.utils.SecurityUtils;

/**
 * 数据脱敏序列化过滤
 *
 * @author hongshu
 */
public class SensitiveJsonSerializer extends ValueSerializer<String> {

    private final DesensitizedType desensitizedType;

    public SensitiveJsonSerializer() {
        this(null);
    }

    private SensitiveJsonSerializer(DesensitizedType desensitizedType) {
        this.desensitizedType = desensitizedType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext context) {
        if (desensitization()) {
            gen.writeString(desensitizedType.desensitizer().apply(value));
        } else {
            gen.writeString(value);
        }
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext context, BeanProperty property) {
        if (property == null) {
            return context.findValueSerializer(String.class);
        }
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (annotation != null && String.class == property.getType().getRawClass()) {
            // 每个属性保留独立规则，避免缓存的序列化器被其他字段覆盖。
            return new SensitiveJsonSerializer(annotation.desensitizedType());
        }
        return context.findValueSerializer(property.getType());
    }

    /**
     * 是否需要脱敏处理
     */
    private boolean desensitization() {
        try {
            LoginUser securityUser = SecurityUtils.getLoginUser();
            // 管理员不脱敏
            return !securityUser.getUser().isAdmin();
        } catch (Exception e) {
            return true;
        }
    }
}
