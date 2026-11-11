package com.github.tvbox.osc.util;

import static okhttp3.ConnectionSpec.CLEARTEXT;
import static okhttp3.ConnectionSpec.COMPATIBLE_TLS;
import static okhttp3.ConnectionSpec.MODERN_TLS;
import static okhttp3.ConnectionSpec.RESTRICTED_TLS;

import android.graphics.Bitmap;
import android.util.Base64;

import com.github.catvod.net.SSLCompat;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.picasso.MyOkhttpDownLoader;
import com.github.tvbox.osc.util.urlhttp.BrotliInterceptor;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.orhanobut.hawk.Hawk;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.Cache;
import okhttp3.ConnectionSpec;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;
import xyz.doikki.videoplayer.exo.ExoMediaSourceHelper;

public class OkGoHelper {
    public static final long DEFAULT_MILLISECONDS = 10000;      //默认的超时时间

    static void initExoOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkExoPlayer");

        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
        }
        builder.connectionSpecs(getConnectionSpec());
        builder.addInterceptor(new BrotliInterceptor());
        builder.retryOnConnectionFailure(true);
        builder.followRedirects(true);
        builder.followSslRedirects(true);

        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        builder.dns(dynamicDns);

        ExoMediaSourceHelper.getInstance(App.getInstance()).setOkClient(builder.build());
    }

    public static DnsOverHttps dnsOverHttps = null;

    //所有客户端经此持有者间接取DoH;按设置项判断开关,DoH失败回落系统DNS(与旧内嵌补丁行为一致)
    public static final Dns dynamicDns = new Dns() {
        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            if (Hawk.get(HawkConfig.DOH_URL, 0) == 0) return Dns.SYSTEM.lookup(hostname);
            DnsOverHttps doh = dnsOverHttps;
            if (doh == null) return Dns.SYSTEM.lookup(hostname);
            try {
                return doh.lookup(hostname);
            } catch (Throwable th) {
                return Dns.SYSTEM.lookup(hostname);
            }
        }
    };

    public static ArrayList<String> dnsHttpsList = new ArrayList<>();

    public static List<ConnectionSpec> getConnectionSpec() {
        return Collections.unmodifiableList(Arrays.asList(RESTRICTED_TLS, MODERN_TLS, COMPATIBLE_TLS, CLEARTEXT));
    }

    public static String getDohUrl(int type) {
        switch (type) {
            case 1: {
                return "https://doh.pub/dns-query";
            }
            case 2: {
                return "https://dns.alidns.com/dns-query";
            }
            case 3: {
                return "https://doh.360.cn/dns-query";
            }
        }
        return "";
    }

    static void initDnsOverHttps() {
        dnsHttpsList.add("关闭");
        dnsHttpsList.add("腾讯");
        dnsHttpsList.add("阿里");
        dnsHttpsList.add("360");
        dnsOverHttps = buildDnsOverHttps(getDohUrl(Hawk.get(HawkConfig.DOH_URL, 0)));
    }

    //官方DnsOverHttps.build()要求url非空;关闭时仍以默认地址构建,保证外部爬虫jar直接引用dnsOverHttps字段不崩
    public static final String DEFAULT_DOH_URL = "https://doh.pub/dns-query";

    public static DnsOverHttps buildDnsOverHttps(String dohUrl) {
        if (dohUrl == null || dohUrl.isEmpty()) dohUrl = DEFAULT_DOH_URL;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkDoh");
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
        }
        builder.addInterceptor(new BrotliInterceptor());
        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        builder.connectionSpecs(getConnectionSpec());
        builder.cache(new Cache(new File(App.getInstance().getCacheDir().getAbsolutePath(), "dohcache"), 10 * 1024 * 1024));
        return new DnsOverHttps.Builder().client(builder.build()).url(HttpUrl.get(dohUrl)).build();
    }

    private static final String DNS_MESSAGE = "application/dns-message";

    //RemoteServer /dns-query 转发端点:返回原始DNS报文(沿用旧内嵌DoH补丁的A+AAAA串联格式)
    public static byte[] lookupHttpsForwardSync(String hostname) throws Throwable {
        DnsOverHttps doh = dnsOverHttps;
        if (doh == null) throw new UnknownHostException(hostname);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(executeDohQuery(hostname, doh, 1)); // A
        if (doh.includeIPv6()) out.write(executeDohQuery(hostname, doh, 28)); // AAAA
        return out.toByteArray();
    }

    private static byte[] executeDohQuery(String hostname, DnsOverHttps doh, int type) throws Throwable {
        ByteArrayOutputStream query = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(query);
        d.writeShort(0xFFFF); // id
        d.writeShort(0x0100); // 递归期望
        d.writeShort(1);      // qdcount
        d.writeShort(0);
        d.writeShort(0);
        d.writeShort(0);
        for (String label : hostname.split("\\.")) {
            byte[] b = label.getBytes(StandardCharsets.UTF_8);
            if (b.length == 0 || b.length > 63) throw new UnknownHostException(hostname);
            d.writeByte(b.length);
            d.write(b);
        }
        d.writeShort(type);
        d.writeShort(1); // IN
        d.flush();
        String encoded = Base64.encodeToString(query.toByteArray(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        HttpUrl url = doh.url().newBuilder().addQueryParameter("dns", encoded).build();
        Request request = new Request.Builder().header("Accept", DNS_MESSAGE).url(url).build();
        Response response = doh.client().newCall(request).execute();
        try {
            return response.body().bytes();
        } finally {
            response.close();
        }
    }
    static OkHttpClient defaultClient = null;
    static OkHttpClient noRedirectClient = null;

    public static OkHttpClient getDefaultClient() {
        return defaultClient;
    }

    public static OkHttpClient getNoRedirectClient() {
        return noRedirectClient;
    }

    public static void init() {
        initDnsOverHttps();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");

        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
        }

        //builder.retryOnConnectionFailure(false);
        builder.connectionSpecs(getConnectionSpec());
        builder.addInterceptor(new BrotliInterceptor());
        builder.readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .dns(dynamicDns);
        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        HttpHeaders.setUserAgent("okhttp/4.12.0");

        OkHttpClient okHttpClient = builder.build();
        OkGo.getInstance().setOkHttpClient(okHttpClient);

        defaultClient = okHttpClient;

        builder.followRedirects(false);
        builder.followSslRedirects(false);
        noRedirectClient = builder.build();

        initExoOkHttpClient();
        initPicasso(okHttpClient);
    }

    static void initPicasso(OkHttpClient client) {
        client.dispatcher().setMaxRequestsPerHost(32);
        MyOkhttpDownLoader downloader = new MyOkhttpDownLoader(client);
        Picasso picasso = new Picasso.Builder(App.getInstance())
                .downloader(downloader)
                .executor(HeavyTaskUtil.getBigTaskExecutorService())
                .defaultBitmapConfig(Bitmap.Config.RGB_565)
                .build();
        Picasso.setSingletonInstance(picasso);
    }

    private static synchronized void setOkHttpSsl(OkHttpClient.Builder builder) {
        try {

            final SSLSocketFactory sslSocketFactory = new SSLCompat();
            builder.sslSocketFactory(sslSocketFactory, SSLCompat.TM);
            builder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
