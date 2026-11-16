package com.lemon.erp.modules.channel.client;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信消息推送的签名与加解密（微信开放平台通用方案，小店消息推送同样适用）：
 * <ul>
 *   <li>签名：token、timestamp、nonce（安全模式再加上密文）按字典序排序后拼接，取 SHA-1 十六进制；</li>
 *   <li>加密：AES-256-CBC，密钥为 Base64(EncodingAESKey + "=")，IV 为密钥前 16 字节，PKCS#7 补位到 32 字节的倍数；
 *       明文 = 16 字节随机数 + 4 字节消息长度（网络字节序）+ 消息 + AppID。</li>
 * </ul>
 * 消息体支持 JSON 与 XML 两种格式（小店后台可选），只提取需要的字段，XML 用正则读取，不解析实体（避免 XXE）
 */
public final class WxCallbackCrypto {

    private static final int BLOCK = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private WxCallbackCrypto() {
    }

    /** SHA-1(排序后拼接) */
    public static String signature(String... parts) {
        String[] sorted = Arrays.stream(parts).map(p -> p == null ? "" : p).sorted().toArray(String[]::new);
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(sha1.digest(String.join("", sorted).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 校验签名（常量时间比较），签名为空时不通过 */
    public static boolean verify(String expected, String... parts) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(signature(parts).getBytes(StandardCharsets.UTF_8), expected.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密
     *
     * @param appId 校验明文末尾的 AppID，为空时不校验
     * @throws IllegalArgumentException 密钥、密文无效或 AppID 不符
     */
    public static String decrypt(String encodingAesKey, String encrypted, String appId) {
        byte[] key = key(encodingAesKey);
        byte[] plain;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(Arrays.copyOfRange(key, 0, 16)));
            plain = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("decrypt failed", e);
        }
        int pad = plain.length == 0 ? 0 : plain[plain.length - 1];
        if (pad < 1 || pad > BLOCK || plain.length < 20 + pad) {
            throw new IllegalArgumentException("invalid padding");
        }
        byte[] content = Arrays.copyOfRange(plain, 0, plain.length - pad);
        int len = ByteBuffer.wrap(content, 16, 4).getInt();
        if (len < 0 || 20 + len > content.length) {
            throw new IllegalArgumentException("invalid length");
        }
        String msg = new String(content, 20, len, StandardCharsets.UTF_8);
        String from = new String(content, 20 + len, content.length - 20 - len, StandardCharsets.UTF_8);
        if (appId != null && !appId.isBlank() && !appId.equals(from)) {
            throw new IllegalArgumentException("appid mismatch");
        }
        return msg;
    }

    /** 加密（模拟推送、测试使用） */
    public static String encrypt(String encodingAesKey, String appId, String msg) {
        byte[] key = key(encodingAesKey);
        byte[] random = new byte[16];
        RANDOM.nextBytes(random);
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);
        byte[] app = (appId == null ? "" : appId).getBytes(StandardCharsets.UTF_8);
        int len = 20 + body.length + app.length;
        int pad = BLOCK - len % BLOCK;
        ByteBuffer buf = ByteBuffer.allocate(len + pad);
        buf.put(random).putInt(body.length).put(body).put(app);
        for (int i = 0; i < pad; i++) {
            buf.put((byte) pad);
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(Arrays.copyOfRange(key, 0, 16)));
            return Base64.getEncoder().encodeToString(cipher.doFinal(buf.array()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** EncodingAESKey 是否有效（43 位，Base64 解码后 32 字节） */
    public static boolean validKey(String encodingAesKey) {
        try {
            key(encodingAesKey);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] key(String encodingAesKey) {
        if (encodingAesKey == null || encodingAesKey.length() != 43) {
            throw new IllegalArgumentException("invalid EncodingAESKey");
        }
        byte[] key = Base64.getDecoder().decode(encodingAesKey + "=");
        if (key.length != 32) {
            throw new IllegalArgumentException("invalid EncodingAESKey");
        }
        return key;
    }

    // ---------------------------------------------------------------- 消息体

    /**
     * 推送消息中使用的字段
     *
     * @param encrypt     密文（安全模式），明文模式为 null
     * @param event       事件，如 channels_ec_order_pay
     * @param orderId     订单号（订单事件、售后事件中的订单号）
     * @param aftersaleId 售后单号（售后事件）
     * @param productId   商品 ID（商品事件）
     */
    public record Message(String encrypt, String event, String orderId, String aftersaleId, String productId) {
    }

    /** 解析 JSON 或 XML 消息体，读取不到的字段为 null */
    public static Message parse(String body) {
        String text = body == null ? "" : body.trim();
        if (text.startsWith("<")) {
            return new Message(xml(text, "Encrypt"), xml(text, "Event"), xml(text, "order_id"), xml(text, "after_sale_order_id"),
                    xml(text, "product_id"));
        }
        try {
            JsonNode root = JSON.readTree(text.isEmpty() ? "{}" : text);
            return new Message(json(root, "Encrypt"), json(root, "Event"), json(root, "order_id"), json(root, "after_sale_order_id"),
                    json(root, "product_id"));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("invalid message", e);
        }
    }

    private static String json(JsonNode root, String field) {
        JsonNode node = root.findValue(field);
        if (node == null || node.isNull() || node.isContainer()) {
            return null;
        }
        String s = node.asString("");
        return s.isBlank() ? null : s;
    }

    private static String xml(String text, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">\\s*(?:<!\\[CDATA\\[(.*?)]]>|([^<]*))\\s*</" + tag + ">", Pattern.DOTALL).matcher(text);
        if (!m.find()) {
            return null;
        }
        String s = m.group(1) != null ? m.group(1) : m.group(2);
        return s == null || s.isBlank() ? null : s.trim();
    }
}
