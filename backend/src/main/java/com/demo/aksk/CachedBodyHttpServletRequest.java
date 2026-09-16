package com.demo.aksk;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 把请求 body 读进内存，之后可多次读取。
 * <p>
 * 为什么需要它？Servlet 的 InputStream <b>只能读一次</b>：
 * Filter 验签要读 body 算 SM3；Controller 的 {@code @RequestBody} 还要再读一遍解析 JSON。
 * 不缓存的话，第二次读就是空的，POST 业务会挂。
 * <p>
 * 对照前端：Fetch 的 body 是流，消费后就不能再读；通常要先 {@code await req.arrayBuffer()} 存住再复用。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 一次性读完原始流，后面 getInputStream() 都从这份副本吐数据
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /** 验签时直接拿原始字节做 SM3，避免编码二次转换导致签名不一致。 */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 同步读，不需要异步监听
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = getCharacterEncoding() == null
                ? StandardCharsets.UTF_8
                : Charset.forName(getCharacterEncoding());
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
