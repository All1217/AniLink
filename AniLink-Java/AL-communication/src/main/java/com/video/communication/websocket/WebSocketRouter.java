package com.video.communication.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/** WebSocket 路由器：只接受 /chat 私信握手；/video 视频房间已迁到 al-danmaku（8030）。 */
@Slf4j
public class WebSocketRouter extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            String uri = req.uri();
            // 根据路径路由到聊天处理器
            if (uri.startsWith("/chat")) {
                handleWebSocketHandshake(ctx, req, "/chat");
                // 移除当前处理器，避免重复处理
                ctx.pipeline().remove(this);
                ctx.pipeline().addLast(new HandlerChat("/chat"));
            } else {
                log.warn("不支持的 WebSocket 路径: {}（/video 请连接 danmakuservice 的 8030 端口）", uri);
                ReferenceCountUtil.release(msg);
                ctx.close();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleWebSocketHandshake(ChannelHandlerContext ctx, FullHttpRequest req, String path) {
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req, path), null, true);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            // 保存 handshaker 到 Channel 属性，供后续使用
            ctx.channel().attr(AttributeKey.valueOf("handshaker")).set(handshaker);
        }
    }

    private String getWebSocketLocation(FullHttpRequest req, String path) {
        String location = req.headers().get("Host") + path;
        return "ws://" + location;
    }
}
