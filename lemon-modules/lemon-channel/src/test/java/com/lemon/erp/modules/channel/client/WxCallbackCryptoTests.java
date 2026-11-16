package com.lemon.erp.modules.channel.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 消息推送签名与加解密（与微信官方示例一致的算法）
 */
class WxCallbackCryptoTests {

    /** 43 位 EncodingAESKey */
    private static final String KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

    @Test
    void signatureSortsAndHashes() {
        // sha1("1409304348" + "nonce" + "token")：三个参数按字典序排序后拼接
        String sig = WxCallbackCrypto.signature("token", "1409304348", "nonce");
        assertThat(sig).isEqualTo(WxCallbackCrypto.signature("nonce", "token", "1409304348")).hasSize(40);
        assertThat(sig).isEqualTo(sha1("1409304348noncetoken"));
        assertThat(WxCallbackCrypto.verify(sig, "token", "1409304348", "nonce")).isTrue();
        assertThat(WxCallbackCrypto.verify(sig.toUpperCase(), "token", "1409304348", "nonce")).isTrue();
        assertThat(WxCallbackCrypto.verify(sig, "token", "1409304349", "nonce")).isFalse();
        assertThat(WxCallbackCrypto.verify(null, "token", "1409304348", "nonce")).isFalse();
    }

    @Test
    void encryptDecryptRoundTrip() {
        String msg = "{\"Event\":\"channels_ec_order_pay\",\"order_info\":{\"order_id\":\"3705115058471208928\"}}";
        String encrypted = WxCallbackCrypto.encrypt(KEY, "wx1234567890", msg);
        assertThat(WxCallbackCrypto.decrypt(KEY, encrypted, "wx1234567890")).isEqualTo(msg);
        // 不校验 AppID
        assertThat(WxCallbackCrypto.decrypt(KEY, encrypted, null)).isEqualTo(msg);
        // AppID 不符、密钥不对
        assertThatThrownBy(() -> WxCallbackCrypto.decrypt(KEY, encrypted, "wxother")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WxCallbackCrypto.decrypt("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefg", encrypted, null))
                .isInstanceOf(IllegalArgumentException.class);
        // 中文、长消息（多个分组）
        String long_ = "订单".repeat(200);
        assertThat(WxCallbackCrypto.decrypt(KEY, WxCallbackCrypto.encrypt(KEY, "wx1", long_), "wx1")).isEqualTo(long_);
    }

    @Test
    void validKey() {
        assertThat(WxCallbackCrypto.validKey(KEY)).isTrue();
        assertThat(WxCallbackCrypto.validKey("short")).isFalse();
        assertThat(WxCallbackCrypto.validKey(null)).isFalse();
    }

    @Test
    void parseJsonAndXml() {
        WxCallbackCrypto.Message json = WxCallbackCrypto.parse(
                "{\"ToUserName\":\"gh_1\",\"Event\":\"channels_ec_aftersale_update\",\"finder_shop_aftersale_status_update\":"
                        + "{\"status\":\"USER_WAIT_RETURN\",\"after_sale_order_id\":\"1234\",\"order_id\":\"5678\"}}");
        assertThat(json.event()).isEqualTo("channels_ec_aftersale_update");
        assertThat(json.aftersaleId()).isEqualTo("1234");
        assertThat(json.orderId()).isEqualTo("5678");
        assertThat(json.encrypt()).isNull();

        WxCallbackCrypto.Message xml = WxCallbackCrypto.parse("<xml><ToUserName><![CDATA[gh_1]]></ToUserName>"
                + "<Event><![CDATA[channels_ec_order_new]]></Event><order_info><order_id>3705</order_id></order_info></xml>");
        assertThat(xml.event()).isEqualTo("channels_ec_order_new");
        assertThat(xml.orderId()).isEqualTo("3705");
        assertThat(xml.aftersaleId()).isNull();

        WxCallbackCrypto.Message enc = WxCallbackCrypto.parse("<xml><ToUserName>gh_1</ToUserName><Encrypt><![CDATA[abc+/=]]></Encrypt></xml>");
        assertThat(enc.encrypt()).isEqualTo("abc+/=");
        assertThatThrownBy(() -> WxCallbackCrypto.parse("{not json")).isInstanceOf(IllegalArgumentException.class);
    }

    private static String sha1(String s) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1")
                    .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
