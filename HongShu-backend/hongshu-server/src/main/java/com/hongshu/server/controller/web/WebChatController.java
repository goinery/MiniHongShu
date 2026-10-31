package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.enums.Result;
import com.hongshu.web.domain.entity.WebChat;
import com.hongshu.web.domain.vo.ChatUserRelationVO;
import com.hongshu.web.websocket.im.CountMessage;
import com.hongshu.web.websocket.im.Message;
import com.hongshu.web.service.IWebChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天
 *
 * @author hongshu
 */
@RequestMapping("/web/im/chat")
@Tag(name = "前台·私信会话", description = "一对一文字图片私信、历史会话、未读与个人侧删除")
@RestController
public class WebChatController {

    private final IWebChatService chatService;

    public WebChatController(IWebChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送消息
     *
     * @param message 消息实体
     */
    @Operation(summary = "发送文字或图片私信",
            description = "msgType 为 1 表示文字、2 表示图片，chatType 仅支持 0 单聊；发送人取当前登录身份，事务写入消息和双方摘要后实时推送。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("sendMsg")
    public Result<?> sendMsg(@RequestBody Message message) {
        if (message.getMsgType() != null && message.getMsgType() != 1 && message.getMsgType() != 2) {
            return Result.build(400, "不支持的消息类型");
        }
        chatService.sendMsg(message);
        return Result.ok();
    }

    /**
     * 获取所有的聊天记录
     *
     * @param currentPage 分页
     * @param pageSize    分页数
     * @param acceptUid   接收方用户ID
     */
    @Operation(summary = "分页查询双方聊天记录",
            description = "按 acceptUid 查询当前用户与对方的可见消息，按时间和 ID 倒序；每页 1 至 100 条，不返回本人已删除的记录。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getAllChatRecord/{currentPage}/{pageSize}")
    public Result<?> getAllChatRecord(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize,
            @Parameter(description = "聊天对方的社区用户 ID") String acceptUid
    ) {
        Page<WebChat> page = chatService.getAllChatRecord(currentPage, pageSize, acceptUid);
        return Result.ok(page);
    }

    /**
     * 获取当前用户下所有聊天的用户信息
     */
    @Operation(summary = "获取当前会员私信会话",
            description = "返回会话用户资料、最新消息摘要、时间和未读数。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getChatUserList")
    public Result<?> getChatUserList() {
        List<ChatUserRelationVO> list = chatService.getChatUserList();
        return Result.ok(list);
    }

    /**
     * 获取所有聊天记录数量
     */
    @Operation(summary = "查询社交通知未读数",
            description = "返回点赞收藏、评论和关注的 Redis 未读计数；私信未读数由会话列表提供。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getCountMessage")
    public Result<?> getCountMessage() {
        CountMessage countMessage = chatService.getCountMessage();
        return Result.ok(countMessage);
    }

    /**
     * 删除聊天
     */
    @Operation(summary = "删除本人可见的一条私信",
            description = "根据消息 ID 设置当前用户一侧的删除标记，并刷新本人会话摘要；不撤回或删除对方可见消息。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("deleteMsg")
    public Result<?> deleteMsg(@Parameter(description = "私信 ID") @RequestParam String id) {
        chatService.deleteMessage(id);
        return Result.ok();
    }

    /**
     * 删除所有聊天记录
     */
    @Operation(summary = "清空本人与指定用户的会话",
            description = "隐藏当前用户一侧的全部聊天记录并移除本人会话，不影响对方保留的记录。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("deleteAllChatRecord")
    public Result<?> deleteAllChatRecord(@Parameter(description = "聊天对方的社区用户 ID") @RequestParam String acceptUid) {
        chatService.deleteConversation(acceptUid);
        return Result.ok();
    }

    /**
     * 清除聊天数量
     *
     * @param sendUid 发送方用户ID
     * @param type    类型
     */
    @Operation(summary = "清零指定类型未读数",
            description = "type：0 点赞收藏、1 评论、2 关注、3 私信；清零私信时必须通过 sendUid 指定对方用户。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("clearMessageCount")
    public Result<?> clearMessageCount(
            @Parameter(description = "私信对方用户 ID；type 为 3 时必填") String sendUid,
            @Parameter(description = "通知类型：0 点赞收藏，1 评论，2 关注，3 私信") Integer type
    ) {
        chatService.clearMessageCount(sendUid, type);
        return Result.ok();
    }

    /**
     * 关闭聊天
     *
     * @param sendUid 发送方用户ID
     */
    @Operation(summary = "关闭当前会员实时连接",
            description = "关闭当前认证用户的 WebSocket；sendUid 是兼容路径参数，不用于关闭其他用户连接。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @RequestMapping("closeChat/{sendUid}")
    public boolean closeChat(@Parameter(description = "兼容路径参数，实际关闭当前登录用户的连接") @PathVariable("sendUid") String sendUid) {
        return chatService.closeChat(sendUid);
    }
}
