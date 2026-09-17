/**
 * 调用「对方 WJZ 开放 API」的本地 Demo（我们是客户端，不是对方服务端）。
 * <p>
 * <b>建议阅读顺序（前端同学）</b>
 * <ol>
 *   <li>{@code ../../resources/application-wjz.yml} — 配 baseURL / AK / SK，像 axios 实例配置</li>
 *   <li>{@link com.demo.wjz.WjzRemoteCaller} — 注入 {@code SignApiService}，真正发签名请求</li>
 *   <li>{@link com.demo.wjz.WjzDemoController} — 本机 {@code /demo/*} 入口，方便 curl</li>
 * </ol>
 * 数据流：{@code curl → WjzDemoController → WjzRemoteCaller → SignApiService → https://对方:8443}
 * <p>
 * 对比本仓库自己当「服务端」验签的代码：见 {@code com.demo.aksk} 包。
 */
package com.demo.wjz;
