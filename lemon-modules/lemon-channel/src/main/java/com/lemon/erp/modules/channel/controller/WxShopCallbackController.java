package com.lemon.erp.modules.channel.controller;

import com.lemon.erp.modules.channel.service.WxCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信小店消息推送地址（免登录，SecurityConfig 放行）：/api/channel/wx-shop/callback/{店铺ID}。
 * GET 为推送地址校验（返回 echostr），POST 为事件（校验通过立即返回 success，异步处理）；签名不对返回 403
 */
@RestController
@RequestMapping("/channel/wx-shop/callback")
@RequiredArgsConstructor
public class WxShopCallbackController {

    private static final String SUCCESS = "success";

    private final WxCallbackService callbackService;

    @GetMapping(value = "/{shopId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(@PathVariable Long shopId,
                                         @RequestParam(required = false) String signature,
                                         @RequestParam(required = false) String timestamp,
                                         @RequestParam(required = false) String nonce,
                                         @RequestParam(required = false) String echostr,
                                         @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        String result = callbackService.verify(shopId, signature, timestamp, nonce, echostr, msgSignature);
        return result == null ? forbidden() : ResponseEntity.ok(result);
    }

    @PostMapping(value = "/{shopId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receive(@PathVariable Long shopId,
                                          @RequestParam(required = false) String signature,
                                          @RequestParam(required = false) String timestamp,
                                          @RequestParam(required = false) String nonce,
                                          @RequestParam(name = "msg_signature", required = false) String msgSignature,
                                          @RequestBody(required = false) String body) {
        return callbackService.receive(shopId, signature, timestamp, nonce, msgSignature, body) ? ResponseEntity.ok(SUCCESS) : forbidden();
    }

    private static ResponseEntity<String> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN).body("invalid signature");
    }
}
