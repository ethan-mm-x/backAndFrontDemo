package com.demo.jwt;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.demo.common.BizException;
import com.demo.config.JwtProperties;
import com.demo.security.LoginUser;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 国密 SM2 签名的 JWT（header.alg = SM2）。
 * <p>
 * 格式仍是三段式 {@code header.payload.signature}（Base64URL），
 * 但签名算法用 SM2，而不是常见的 HS256/RS256。
 * payload 里固定带 {@code userId}、{@code username}、{@code iat}、{@code exp}。
 */
@Component
public class JwtUtil {

    private final JwtProperties properties;
    private SM2 sm2;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 启动时加载配置里的 SM2 密钥；非法或不存在则临时生成一对（仅适合本地 Demo）。
     */
    @PostConstruct
    public void init() {
        if (StrUtil.isNotBlank(properties.getPrivateKeyHex()) && StrUtil.isNotBlank(properties.getPublicKeyHex())) {
            try {
                this.sm2 = new SM2(properties.getPrivateKeyHex(), properties.getPublicKeyHex());
                // 试签一次，密钥非法则回退生成
                sm2.sign("ping".getBytes(StandardCharsets.UTF_8));
                return;
            } catch (Exception ignored) {
                // fall through
            }
        }
        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        String priv = HexUtil.encodeHexStr(((BCECPrivateKey) pair.getPrivate()).getD().toByteArray());
        String pub = HexUtil.encodeHexStr(((BCECPublicKey) pair.getPublic()).getQ().getEncoded(false));
        properties.setPrivateKeyHex(priv);
        properties.setPublicKeyHex(pub);
        this.sm2 = new SM2(priv, pub);
    }

    /** 登录成功后签发 token。 */
    public String createToken(LoginUser user) {
        long now = System.currentTimeMillis();
        long exp = now + properties.getExpireMs();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "SM2");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getUserId());
        payload.put("username", user.getUsername());
        payload.put("iat", now);
        payload.put("exp", exp);

        String headerPart = Base64.encodeUrlSafe(JSONUtil.toJsonStr(header));
        String payloadPart = Base64.encodeUrlSafe(JSONUtil.toJsonStr(payload));
        String content = headerPart + "." + payloadPart;
        byte[] sign = sm2.sign(content.getBytes(StandardCharsets.UTF_8));
        String signPart = Base64.encodeUrlSafe(sign);
        return content + "." + signPart;
    }

    /**
     * 验签并解析出登录用户；签名错 / 过期 / 缺字段都会抛 401。
     */
    public LoginUser parseToken(String token) {
        String[] parts = split(token);
        String content = parts[0] + "." + parts[1];
        byte[] sign = Base64.decode(parts[2]);
        boolean ok = sm2.verify(content.getBytes(StandardCharsets.UTF_8), sign);
        if (!ok) {
            throw new BizException(401, "无效的登录凭证");
        }
        JSONObject payload = JSONUtil.parseObj(Base64.decodeStr(parts[1]));
        Long exp = payload.getLong("exp");
        if (exp == null || exp < System.currentTimeMillis()) {
            throw new BizException(401, "登录已过期");
        }
        Long userId = payload.getLong("userId");
        String username = payload.getStr("username");
        if (userId == null || StrUtil.isBlank(username)) {
            throw new BizException(401, "无效的登录凭证");
        }
        return new LoginUser(userId, username);
    }

    /**
     * 是否需要续期：仍有效，但剩余时间 ≤ {@code jwt.renew-threshold-ms}。
     */
    public boolean shouldRenew(String token) {
        try {
            String[] parts = split(token);
            JSONObject payload = JSONUtil.parseObj(Base64.decodeStr(parts[1]));
            Long exp = payload.getLong("exp");
            if (exp == null) {
                return false;
            }
            long remain = exp - System.currentTimeMillis();
            return remain > 0 && remain <= properties.getRenewThresholdMs();
        } catch (Exception e) {
            return false;
        }
    }

    private String[] split(String token) {
        if (StrUtil.isBlank(token)) {
            throw new BizException(401, "未登录");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BizException(401, "无效的登录凭证");
        }
        return parts;
    }
}
