package com.aliyun.rtcdemo.network;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.aliyun.rtcdemo.utils.ThreadUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * 原生请求封装类
 */
public class AliRtcWebUtils {

    private static final String TAG = AliRtcWebUtils.class.getSimpleName();
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_GET = "GET";
    private static SSLContext ctx = null;
    private static Handler handler;


    private AliRtcWebUtils() {
        handler = new Handler(Looper.getMainLooper());
    }

    public static AliRtcWebUtils getInstance() {
        return HelpHolder.INSTANCE;
    }

    private static class HelpHolder {
        private static AliRtcWebUtils INSTANCE = new AliRtcWebUtils();
    }


    /**
     * @param url
     * @param json
     * @return
     */
    public void doPostStringResponse(String url, String json, HttpCallBack httpCallBack) {
        Log.i(TAG, url + " json : " + json);
        ThreadUtils.runOnSubThread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                BufferedReader br = null;
                String responseBody = "";

                try {
                    conn = getConnection(new URL(url), "POST", "application/json;charset=utf-8");
                    conn.setReadTimeout(5000);
                    conn.setConnectTimeout(5000);
                    conn.connect();
                    int stat = conn.getResponseCode();

                    // post发送json数据
                    if (!TextUtils.isEmpty(json)) {
                        byte[] bytes = json.getBytes();
                        conn.setRequestProperty("Content-Length", bytes.length + "");
                        OutputStream outputStream = conn.getOutputStream();
                        outputStream.write(bytes);
                        outputStream.flush();
                        outputStream.close();
                    }
                    Log.i(TAG, "code : " + stat);
                    if (stat == 200) {
                        //获取请求的资源
                        br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        responseBody = br.readLine();
                        String finalResponseBody = responseBody;
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                httpCallBack.onSuccess(finalResponseBody);
                            }
                        });
                    } else {
                        br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                        responseBody = br.readLine();
                        httpCallBack.onError(responseBody);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    httpCallBack.onError(e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }


    /**
     * @param url          地址
     * @param httpCallBack 回调
     */
    public void doGet(String url, HttpCallBack httpCallBack) {
        innerDoGet(url, null, httpCallBack);
    }

    private void innerDoGet(String url, Map<String, String> params, HttpCallBack httpCallBack) {
        ThreadUtils.runOnSubThread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                BufferedReader br = null;
                String data = "";
                try {
                    conn = getConnection(new URL(getQueryUrl(url, params)), "GET", "*/*;charset=utf-8");
                    conn.setReadTimeout(5000);
                    conn.setConnectTimeout(5000);
                    conn.connect();
                    int stat = conn.getResponseCode();
                    Log.i(TAG, "code : " + stat);
                    if (stat == 200) {
                        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        data = br.readLine();
                        Log.i(TAG, "data : " + data);
                        String finalData = data;
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                httpCallBack.onSuccess(finalData);
                            }
                        });
                    } else {
                        br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        data = br.readLine();
                        Log.i(TAG, "data error : " + data);
                        httpCallBack.onError(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    httpCallBack.onError(e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    /**
     * 带参数的请求
     */
    public void doGet(String url, Map<String, String> params, HttpCallBack httpCallBack) {

        innerDoGet(url, params, httpCallBack);
    }


    private HttpURLConnection getConnection(URL url, String method, String ctype) throws IOException {
        HttpURLConnection conn = null;
        if ("https".equals(url.getProtocol())) {
            try {
                conn = initHttps(url, method);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        conn.setRequestMethod(method);
        conn.setAllowUserInteraction(true);
        conn.setInstanceFollowRedirects(true);
        // post方法必须加上,get方法是不需要这些,否则会失败
        if (method.equalsIgnoreCase("post")) {
            conn.setDoInput(true);
            //指示应用程序要将数据写入URL连接,其值默认为false（是否传参）
            conn.setDoOutput(true);
            conn.setUseCaches(false);
        }
        // cookie也可以在这里设置
        //conn.setRequestProperty("Set-Cookie", "");
        conn.setRequestProperty("User-Agent", "Mozilla/4.0");
        conn.setRequestProperty("Content-Type", ctype);

        return conn;
    }

    /**
     * 拼接get数据
     *
     * @param url    地址
     * @param params get参数
     * @return
     */
    private String getQueryUrl(String url, Map<String, String> params) {
        StringBuilder neoUrl = new StringBuilder(url);
        if (params != null) {
            neoUrl.append("?");
            for (Map.Entry<String, String> stringStringEntry : params.entrySet()) {
                neoUrl.append(stringStringEntry.getKey()).append("=").append(stringStringEntry.getValue()).append("&");
            }
            neoUrl = new StringBuilder(neoUrl.substring(0, neoUrl.length() - 1));
        }
        Log.d(TAG, "getQueryUrl: "+neoUrl);
        return neoUrl.toString();
    }

    /**
     * 初始化https请求参数
     */
    private static HttpsURLConnection initHttps(URL url, String method)
            throws IOException, NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException {
        TrustManager[] tm = {new DefaultTrustManager(null)};
        SSLContext sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(null, tm, new java.security.SecureRandom());
        // 从上述SSLContext对象中得到SSLSocketFactory对象
        SSLSocketFactory ssf = sslContext.getSocketFactory();
        HttpsURLConnection http = (HttpsURLConnection) url.openConnection();
        // 连接超时
        http.setConnectTimeout(5000);
        // 读取超时
        http.setReadTimeout(5000);
        http.setRequestMethod(method);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        http.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.146 Safari/537.36");
        http.setSSLSocketFactory(ssf);
        http.setHostnameVerifier(AliRtcHostnameVerifier.INSTANCE);
        http.setDoOutput(true);
        http.setDoInput(true);
        return http;
    }

    public static class DefaultTrustManager implements X509TrustManager {
        private DefaultTrustManager(Object o) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }

    /**
     * 将请求转到io线程，然后返回数据到主线程
     */
    public interface HttpCallBack {
        /**
         * 错误回调
         * @param error
         */
        void onError(String error);

        /**
         * 成功回调
         * @param result
         */
        void onSuccess(String result);
    }

}
