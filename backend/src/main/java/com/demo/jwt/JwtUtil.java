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
 * 国密 SM2 签名的 JWT（alg=SM2）。claims 含 userId、username。
 */
@Component
public class JwtUtil {

    private final JwtProperties properties;
    private SM2 sm2;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

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
