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
 * 国密 AK/SK 签名工具（纯函数，无 Spring 依赖）。
 * <p>
 * <b>客户端和服务端必须共用这一套规则</b>，差一个换行、query 排序不同，验签就会失败。
 * <p>
 * 待签串（六行，用 {@code \n} 连接）：
 * <pre>
 * METHOD          // 大写，如 GET / POST
 * PATH            // 不含 ?query，如 /api/open/echo
 * canonicalQuery  // query 按 key 排序后的 k=v&amp;...；没有则为空串
 * timestamp       // Unix 秒，字符串形式
 * nonce           // 随机串，防重放
 * sm3Hex(body)    // 原始 body 字节的 SM3 hex；无 body 按空字节
 * </pre>
 * 签名：{@code signature = HMAC-SM3(SK, stringToSign)}，结果为小写 hex。
 * <p>
 * 对照前端：就像约定「请求指纹」的字符串模板；云厂商文档里的 Canonical Request 就是这类东西。
 */
public final class AkSkSigner {

    private AkSkSigner() {
    }

    /**
     * 把 {@code a=1&amp;b=2} 规范成稳定顺序，避免 {@code b=2&amp;a=1} 导致签名不一致。
     * <p>
     * 对照前端：类似自己实现一个稳定的 {@code URLSearchParams} 序列化。
     */
    public static String canonicalQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return "";
        }
        // TreeMap：按 key 字典序；同一 key 多个 value 再各自排序
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

    /** body 的国密 SM3 摘要（hex）。空 body ≠ 不参与签名，而是对 0 字节做 SM3。 */
    public static String bodySm3Hex(byte[] body) {
        byte[] data = body == null ? new byte[0] : body;
        return new SM3().digestHex(data);
    }

    /** 拼出待签串。学习时用 {@code AkSkClientDemo} 打印出来对照最直观。 */
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

    /**
     * 用 SK 对 stringToSign 做 HMAC-SM3。
     * <p>
     * 注意：这里入参是 SK，但返回的是 signature；网络上只传 signature，不传 SK。
     */
    public static String sign(String secretKey, String stringToSign) {
        HMac hmac = new HMac(HmacAlgorithm.HmacSM3, secretKey.getBytes(StandardCharsets.UTF_8));
        return hmac.digestHex(stringToSign);
    }

    /** 常量时间友好的 hex 比对（长度不同直接 false；同长度逐字节比）。 */
    public static boolean matches(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null) {
            return false;
        }
        byte[] a = expectedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] b = actualHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return a.length == b.length && Arrays.equals(a, b);
    }
}
