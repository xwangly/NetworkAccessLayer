/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xwang.net.http;


import com.xwang.net.Engine;
import com.xwang.net.EngineRequest;
import com.xwang.net.NetException;
import com.xwang.net.NetLog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * An Engine based on {@link HttpURLConnection}.
 */
public abstract class HurlEngine<RESPONSE extends HttpEngineResponse> implements Engine<RESPONSE> {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * An interface for transforming URLs before use.
     */
    public interface UrlRewriter {
        /**
         * Returns a URL to use instead of the provided one, or null to indicate
         * this URL should not be used at all.
         */
        public String rewriteUrl(String originalUrl);
    }

    private final UrlRewriter mUrlRewriter;
    private final SSLSocketFactory mSslSocketFactory;

    public HurlEngine() {
        this(null);
    }

    /**
     * @param urlRewriter Rewriter to use for request URLs
     */
    public HurlEngine(UrlRewriter urlRewriter) {
        this(urlRewriter, null);
    }

    /**
     * @param urlRewriter Rewriter to use for request URLs
     * @param sslSocketFactory SSL factory to use for HTTPS connections
     */
    public HurlEngine(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        mUrlRewriter = urlRewriter;
        mSslSocketFactory = sslSocketFactory;
    }

    @Override
    public RESPONSE performRequest(EngineRequest request) throws NetException {
        long networkTimeMs = System.currentTimeMillis();
        HttpRequest httpRequest = (HttpRequest) request.getRequest();
        String url = request.getURL();
        HashMap<String, String> map = new HashMap<String, String>();
        map.putAll(httpRequest.getHeaders());
        if (mUrlRewriter != null) {
            String rewritten = mUrlRewriter.rewriteUrl(url);
            if (rewritten == null) {
                throw new NetException(NetException.IO_EXCEPTION, new IOException("URL blocked by rewriter: " + url));
            }
            url = rewritten;
        }
        try {
            URL parsedUrl = new URL(url);
            HttpURLConnection connection = openConnection(parsedUrl, request);
            for (String headerName : map.keySet()) {
                connection.addRequestProperty(headerName, map.get(headerName));
            }
            String method = httpRequest.getMethod();
            if (method == null) throw new NetException(NetException.PARAMS_VALID_EXCEPTION, new InvalidParameterException("method is empty"));
            method = method.toUpperCase();
            connection.setRequestMethod(method);

            if ("PUT".equals(method) || "POST".equals(method) || "PATCH".equals(method)) {
                byte[] body = request.getBody();
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.addRequestProperty(HEADER_CONTENT_TYPE, httpRequest.getBodyContentType());
                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.write(body);
                    out.close();
                }
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == -1) {
                // -1 is returned by getResponseCode() if the response code could not be retrieved.
                // Signal to the caller that something was wrong with the connection.
                throw new IOException("Could not retrieve response code from HttpUrlConnection.");
            }
            String responseMessage = connection.getResponseMessage();

            Map<String, String> result = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            String key, value;
            for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                key = header.getKey();
                value = header.getValue().get(0);
                if (key != null) {
                    result.put(key, value);
                }
//                result.put(header.getKey(), header.getValue().get(0));
            }

            InputStream inputStream;
            try {
                inputStream = connection.getInputStream();
            } catch (IOException ioe) {
                inputStream = connection.getErrorStream();
            }
            byte[] datas = entityToBytes(inputStream);

            networkTimeMs = System.currentTimeMillis() - networkTimeMs;
            return createResponse(responseCode, responseMessage, datas, result, networkTimeMs, connection, request);
        } catch (IOException e) {
            e.printStackTrace();
            throw new NetException(NetException.IO_EXCEPTION, e);
        }
    }

    protected abstract RESPONSE createResponse(int responseCode, String responseMessage, byte[] datas, Map<String, String> header, long networkTimeMs, HttpURLConnection connection, EngineRequest request);

    @Override
    public void cancelRequest(EngineRequest request) throws NetException {

    }

    @Override
    public void shutDown() {

    }


    /** Reads the contents of HttpEntity into a byte[]. */
    private byte[] entityToBytes(InputStream entity) throws IOException {
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            InputStream in = entity;
            if (in == null) {
                throw new IOException("InputStream is empty");
            }
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        } finally {
            try {
                // Close the InputStream and release the resources by "consuming the content".
                entity.close();
            } catch (IOException e) {
                // This can happen if there was an exception above that left the entity in
                // an invalid state.
                NetLog.d("Error occured when calling consumingContent");
            }
            bytes.close();
        }
    }

    /**
     * Create an {@link HttpURLConnection} for the specified {@code url}.
     */
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Opens an {@link HttpURLConnection} with parameters.
     * @param url
     * @return an open connection
     * @throws IOException
     */
    private HttpURLConnection openConnection(URL url, EngineRequest request) throws IOException {
        HttpURLConnection connection = createConnection(url);

        int timeoutMs = request.getTimeoutMs();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
        }

        return connection;
    }
}
