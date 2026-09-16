package com.mfx.sign;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 固定向量：保证与 backend {@code com.demo.aksk.AkSkSigner} 规则一致。
 */
class AkSkSignerTest {

    @Test
    void canonicalQuery_sortsKeys() {
        assertEquals("a=1&b=2", AkSkSigner.canonicalQuery("b=2&a=1"));
        assertEquals("", AkSkSigner.canonicalQuery(null));
        assertEquals("", AkSkSigner.canonicalQuery(""));
    }

    @Test
    void canonicalQueryFromMap_sortsKeys() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("name", "world");
        query.put("age", 18);
        assertEquals("age=18&name=world", AkSkSigner.canonicalQueryFromMap(query));
    }

    @Test
    void emptyBodySm3_isStable() {
        // SM3 of empty bytes
        assertEquals("1ab21d8355cfa17f8e61194831e81a8f22bec8c728fefb747ed035eb5082aa2b",
                AkSkSigner.bodySm3Hex(new byte[0]));
        assertEquals(AkSkSigner.bodySm3Hex(new byte[0]), AkSkSigner.bodySm3Hex(null));
    }

    @Test
    void buildStringToSign_andSign_matchKnownVector() {
        String method = "GET";
        String path = "/api/open/echo";
        String query = "name=world";
        String timestamp = "1700000000";
        String nonce = "abc123nonce";
        byte[] body = new byte[0];
        String sk = "demo-sk-please-change-me";

        String stringToSign = AkSkSigner.buildStringToSign(method, path, query, timestamp, nonce, body);
        assertEquals(
                "GET\n/api/open/echo\nname=world\n1700000000\nabc123nonce\n1ab21d8355cfa17f8e61194831e81a8f22bec8c728fefb747ed035eb5082aa2b",
                stringToSign
        );

        String signature = AkSkSigner.sign(sk, stringToSign);
        // 再次签名应稳定；matches 大小写不敏感
        assertEquals(64, signature.length());
        assertTrue(AkSkSigner.matches(signature, signature.toUpperCase()));
    }

    @Test
    void postBody_participatesInSm3() {
        byte[] body = "{\"title\":\"ping\",\"content\":\"aksk-sm3-demo\"}".getBytes(StandardCharsets.UTF_8);
        String stringToSign = AkSkSigner.buildStringToSign(
                "POST", "/api/open/message", "", "1700000000", "n1", body);
        String[] lines = stringToSign.split("\n", -1);
        assertEquals(6, lines.length);
        assertEquals("POST", lines[0]);
        assertEquals("/api/open/message", lines[1]);
        assertEquals("", lines[2]);
        assertEquals(AkSkSigner.bodySm3Hex(body), lines[5]);
    }
}
