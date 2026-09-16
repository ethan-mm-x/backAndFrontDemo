package com.demo.aksk;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.digest.SM3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 国密 AK/SK 签名：HMAC-SM3。
 * <p>
 * 待签串：
 * <pre>
 * METHOD\n
 * PATH\n
 * canonicalQuery\n
 * timestamp\n
 * nonce\n
 * sm3Hex(body)
 * </pre>
 * 空 body 的 SM3 按空字节计算。
 */
public final class AkSkSigner {

    private AkSkSigner() {
    }

    public static String canonicalQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return "";
        }
        Map<String, List<String>> sorted = new TreeMap<>();
        for (String pair : queryString.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";
            sorted.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> e : sorted.entrySet()) {
            List<String> values = e.getValue();
            values.sort(String::compareTo);
            for (String v : values) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(e.getKey()).append('=').append(v);
            }
        }
        return sb.toString();
    }

    public static String bodySm3Hex(byte[] body) {
        byte[] data = body == null ? new byte[0] : body;
        return new SM3().digestHex(data);
    }

    public static String buildStringToSign(String method,
                                          String path,
                                          String queryString,
                                          String timestamp,
                                          String nonce,
                                          byte[] body) {
        return String.join("\n",
                method == null ? "" : method.toUpperCase(Locale.ROOT),
                path == null ? "" : path,
                canonicalQuery(queryString),
                timestamp == null ? "" : timestamp,
                nonce == null ? "" : nonce,
                bodySm3Hex(body)
        );
    }

    public static String sign(String secretKey, String stringToSign) {
        HMac hmac = new HMac(HmacAlgorithm.HmacSM3, secretKey.getBytes(StandardCharsets.UTF_8));
        return hmac.digestHex(stringToSign);
    }

    public static boolean matches(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null) {
            return false;
        }
        byte[] a = expectedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] b = actualHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return a.length == b.length && Arrays.equals(a, b);
    }
}
