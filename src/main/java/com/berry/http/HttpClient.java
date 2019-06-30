package com.berry.http;

import com.berry.common.Constants;
import com.berry.util.StringUtils;
import okhttp3.*;
import okhttp3.internal.annotations.EverythingIsNonNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.berry.common.Constants.DEFAULT_MIME;

/**
 * Title HttpClient
 * Description
 * Copyright (c) 2019
 *
 * @author berry_cooper
 * @version 1.0
 * @date 2019/6/24 15:29
 */
public class HttpClient {

    /**
     * 默认 http 客户端
     */
    private static final OkHttpClient CLIENT;
    /**
     * 连接超时时间 单位秒(默认10s)
     */
    private static final int CONNECT_TIMEOUT = 10;
    /**
     * 回调超时
     */
    private static final int CALL_TIMEOUT = 10;
    /**
     * 回复超时时间 单位秒(默认30s)
     */
    private static final int READ_TIMEOUT = 30;
    /**
     * 底层HTTP库所有的并发执行的请求数量
     */
    private static final int DISPATCHER_MAX_REQUESTS = 64;
    /**
     * 底层HTTP库对每个独立的Host进行并发请求的数量
     */
    private static final int DISPATCHER_MAX_REQUESTS_PER_HOST = 16;
    /**
     * 底层HTTP库中复用连接对象的最大空闲数量
     */
    private static final int CONNECTION_POOL_MAX_IDLE_COUNT = 32;
    /**
     * 底层HTTP库中复用连接对象的回收周期（单位分钟）
     */
    private static final int CONNECTION_POOL_MAX_IDLE_MINUTES = 5;

    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(DISPATCHER_MAX_REQUESTS);
        dispatcher.setMaxRequestsPerHost(DISPATCHER_MAX_REQUESTS_PER_HOST);

        ConnectionPool pool = new ConnectionPool(CONNECTION_POOL_MAX_IDLE_COUNT,
                CONNECTION_POOL_MAX_IDLE_MINUTES, TimeUnit.MINUTES);

        CLIENT = new OkHttpClient.Builder()
                .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .dispatcher(dispatcher)
                .connectionPool(pool)
                .build();
    }

    /**
     * Get 请求 无参数，无请求头
     *
     * @param url 地址
     * @return 响应
     */
    public static Response get(String url) {
        return get(url, null, null);
    }

    /**
     * Get 请求
     *
     * @param url    基础地址 不带参数
     * @param params url参数 map
     * @param header 请求头 map
     * @return 响应
     */
    public static Response get(String url, @Nullable Map<String, Object> params, Map<String, Object> header) {
        if (params != null) {
            String urlParams = StringUtils.parseUrlParams(params);
            url = url + "?" + urlParams;
        }
        Request.Builder requestBuilder = new Request.Builder().get().url(url);
        return send(requestBuilder, header);
    }

    /**
     * Form 表单请求 （application/x-www-form-urlencoded）
     *
     * @param url     地址
     * @param params  请求参数 map
     * @param headers 请求头 map
     * @return 响应
     */
    public Response post(String url, Map<String, Object> params, Map<String, Object> headers) {
        final FormBody.Builder fb = new FormBody.Builder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            fb.add(entry.getKey(), entry.getValue().toString());
        }
        return post(url, fb.build(), headers);
    }

    /**
     * 请求体为 字符串， 默认媒体类型-二进制流
     */
    public Response post(String url, String body, Map<String, Object> header) {
        return post(url, StringUtils.utf8Bytes(body), header, DEFAULT_MIME);
    }

    /**
     * 请求体为 字节数组，默认媒体类型-二进制流
     */
    public Response post(String url, byte[] body, Map<String, Object> header) {
        return post(url, body, header, DEFAULT_MIME);
    }

    /**
     * 请求体为 字节数组，指定 媒体类型
     */
    public Response post(String url, byte[] body, Map<String, Object> header, String contentType) {
        RequestBody requestBody = RequestBody.create(MediaType.parse(contentType), body);
        return post(url, requestBody, header);
    }

    /**
     * 文件上传 文件体为 字节数组
     */
    public Response multipartPost(String url,
                                  Map<String, Object> fields,
                                  String name,
                                  String fileName,
                                  byte[] fileBody,
                                  String mimeType,
                                  Map<String, Object> headers) {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        return multipartPost(url, fields, name, fileName, file, headers);
    }

    /**
     * 文件上传 文件体为 file
     */
    public Response multipartPost(String url,
                                  Map<String, Object> fields,
                                  String name,
                                  String fileName,
                                  File fileBody,
                                  String mimeType,
                                  Map<String, Object> headers) {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        return multipartPost(url, fields, name, fileName, file, headers);
    }


    /**
     * 异步post
     *
     * @param url         地址
     * @param body        请求体字节数组
     * @param offset      请求体偏移
     * @param size        请求体实际需要读取大小
     * @param header      请求头map
     * @param contentType 请求体类型
     * @param cb          异步回调
     */
    public void asyncPost(String url, byte[] body, int offset, int size, Map<String, Object> header, String contentType, AsyncCallback cb) {
        RequestBody requestBody = RequestBody.create(MediaType.parse(contentType), body, offset, size);
        Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
        asyncSend(requestBuilder, header, cb);
    }

    /**
     * 异步文件上传 文件体为 字节数组
     */
    public void asyncMultipartPost(String url,
                                   Map<String, Object> fields,
                                   String name,
                                   String fileName,
                                   byte[] fileBody,
                                   String mimeType,
                                   Map<String, Object> headers,
                                   AsyncCallback cb) {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        asyncMultipartPost(url, fields, name, fileName, file, headers, cb);
    }

    /**
     * 异步文件上传 文件体为 file
     */
    public void asyncMultipartPost(String url,
                                   Map<String, Object> fields,
                                   String name,
                                   String fileName,
                                   File fileBody,
                                   String mimeType,
                                   Map<String, Object> headers,
                                   AsyncCallback cb) {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        asyncMultipartPost(url, fields, name, fileName, file, headers, cb);
    }

    // ～ private
    //=======================================================================================================================

    /**
     * 异步文件上传
     *
     * @param url      地址
     * @param fields   字段信息 data part
     * @param name     文件接受字段名
     * @param fileName 文件名 可为空
     * @param file     已包装的文件请求体
     * @param header   请求头 可为空
     * @param cb       异步回调
     */
    private void asyncMultipartPost(String url,
                                    Map<String, Object> fields,
                                    String name,
                                    String fileName,
                                    RequestBody file,
                                    Map<String, Object> header,
                                    AsyncCallback cb) {
        Request.Builder requestBuilder = getBuilder(url, fields, name, fileName, file);
        asyncSend(requestBuilder, header, cb);
    }

    /**
     * 构建 post 请求
     *
     * @param url    地址
     * @param body   已包装的请求体
     * @param header 请求头
     * @return 响应
     */
    private Response post(String url, RequestBody body, Map<String, Object> header) {
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        return send(requestBuilder, header);
    }

    /**
     * 文件上传
     *
     * @param url      地址
     * @param fields   字段信息 data part
     * @param name     文件接受字段名
     * @param fileName 文件名 可为空
     * @param file     已包装的文件请求体
     * @param headers  请求头 可为空
     * @return 响应
     */
    private Response multipartPost(String url,
                                   Map<String, Object> fields,
                                   String name,
                                   @Nullable String fileName,
                                   RequestBody file,
                                   Map<String, Object> headers) {
        Request.Builder requestBuilder = getBuilder(url, fields, name, fileName, file);
        return send(requestBuilder, headers);
    }

    /**
     * 后去文件上传类型 build
     *
     * @param url      地址
     * @param fields   字段信息 data part
     * @param name     文件接受字段名
     * @param fileName 文件名 可为空
     * @param file     已包装的文件请求体
     * @return builder
     */
    private Request.Builder getBuilder(String url, Map<String, Object> fields, String name, @Nullable String fileName, RequestBody file) {
        final MultipartBody.Builder mb = new MultipartBody.Builder();
        mb.addFormDataPart(name, fileName, file);

        if (fields != null) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                mb.addFormDataPart(entry.getKey(), entry.getValue().toString());
            }
        }
        mb.setType(MediaType.get("multipart/form-data"));
        RequestBody body = mb.build();
        return new Request.Builder().url(url).post(body);
    }

    /**
     * 发送请求，返回结果
     *
     * @param requestBuilder 请求构建器
     * @param header         请求头
     * @return 响应
     */
    private static Response send(final Request.Builder requestBuilder, @Nullable Map<String, Object> header) {
        if (header != null) {
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue().toString());
            }
        }
        requestBuilder.header("User-Agent", userAgent());
        okhttp3.Response response = null;
        try {
            response = CLIENT.newCall(requestBuilder.build()).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.create(response, null);
    }

    /**
     * 发送异步请求
     *
     * @param requestBuilder 请求构建器
     * @param header         请求头 map
     * @param cb             异步回调
     */
    private void asyncSend(final Request.Builder requestBuilder, @Nullable Map<String, Object> header, final AsyncCallback cb) {
        if (header != null) {
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue().toString());
            }
        }
        requestBuilder.header("User-Agent", userAgent());
        CLIENT.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            @EverythingIsNonNull
            public void onFailure(Call call, IOException e) {
                cb.complete(Response.create(null, e.getMessage()));
            }

            @Override
            @EverythingIsNonNull
            public void onResponse(Call call, okhttp3.Response response) {
                cb.complete(Response.create(response, null));
            }
        });
    }

    /**
     * 获取本地代理信息
     *
     * @return string info
     */
    private static String userAgent() {
        String javaVersion = "Java/" + System.getProperty("java.version");
        String os = System.getProperty("os.name") + " "
                + System.getProperty("os.arch") + " " + System.getProperty("os.version");
        String sdk = "OSSJava/" + Constants.VERSION;
        return sdk + " (" + os + ") " + javaVersion;
    }
}