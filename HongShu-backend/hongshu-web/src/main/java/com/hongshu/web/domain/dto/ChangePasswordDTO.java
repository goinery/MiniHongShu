package com.hongshu.web.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "修改本人密码")
public class ChangePasswordDTO {
    @NotBlank(message = "请输入原密码")
    @Size(max = 72, message = "原密码长度无效")
    @Schema(description = "原密码", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 72, message = "新密码至少6位，且不能超过72字节")
    @Schema(description = "新密码，至少6位且不超过72字节", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String newPassword;

    @NotBlank(message = "请再次输入新密码")
    @Size(max = 72, message = "确认密码长度无效")
    @Schema(description = "再次输入新密码", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String confirmPassword;
}
