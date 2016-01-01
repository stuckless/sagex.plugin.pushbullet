package sagex.plugin.pushbullet;

import sagex.util.ILog;
import sagex.util.LogProvider;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RequestBuilder implements Closeable {
    ILog log = LogProvider.getLogger(this.getClass());
    public class Param {
        public Param(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String key;
        public String value;
    }

    private String url = null;
    private List<Param> parameters = new ArrayList<Param>();
    private Map<String, String> headers = new LinkedHashMap<String, String>();
    private HttpURLConnection connection = null;
    private int timeout = 3000;
    private InputStream inputStream = null;
    private String contentType = "application/x-www-form-urlencoded";

    // used for post
    private String body = null;

    public RequestBuilder(String url) {
        setUrl(url);
    }

    public RequestBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    public RequestBuilder addHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public RequestBuilder addHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public RequestBuilder setContentType(String contentType) {
        this.contentType=contentType;
        return this;
    }

    public RequestBuilder setBody(String body) {
        this.body=body;
        return this;
    }

    public RequestBuilder addParameter(String name, String value) {
        this.parameters.add(new Param(name, value));
        return this;
    }

    public RequestBuilder setConnectTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    private String buildParameters() {
        StringBuilder u = new StringBuilder();
        if (parameters.size() > 0) {
            boolean appendSep = false;
            for (Param me : parameters) {
                if (appendSep) u.append("&");
                try {
                    u.append(me.key).append("=").append(URLEncoder.encode(me.value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                appendSep = true;
            }
        }

        return u.toString();
    }

    public RequestBuilder sendRequest() throws IOException {
        connect();
        if (inputStream==null) {
            inputStream = connection.getInputStream();
        }
        return this;
    }

    public RequestBuilder postRequest() throws IOException {
        long st = System.currentTimeMillis();
        log.debug("HTTP POST: " + url);
        connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(timeout);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);

        if (headers.size() > 0) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                connection.setRequestProperty(h.getKey(), h.getValue());
            }
        }

        connection.setDoInput(true);
        connection.setDoOutput(true);
        if (body!=null) {
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
            wr.close();
        } else {
            if (parameters.size() > 0) {
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(buildParameters());
                wr.flush();
                wr.close();
            }
        }

        connection.setInstanceFollowRedirects(true);

        connection.connect();
        inputStream = connection.getInputStream();

        log.debug("Completed Response in " + (System.currentTimeMillis() - st) + "ms");

        return this;
    }

    public RequestBuilder closeQuietly() {
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (connection != null) {
            connection.disconnect();
            connection = null;
        }

        headers.clear();
        parameters.clear();
        url = null;

        return this;
    }

    public InputStream getInputStream() throws IOException {
        if (connection == null) {
            connect();
        }

        if (inputStream == null) {
            inputStream = connection.getInputStream();
        }
        return inputStream;
    }

    public HttpURLConnection getConnection() throws IOException {
        if (connection == null) {
            sendRequest();
        }
        return connection;
    }

    @Override
    public void close() throws IOException {
        closeQuietly();
    }

    public RequestBuilder connect() throws IOException {
        if (connection == null) {
            StringBuilder u = new StringBuilder(url);
            if (parameters.size() > 0) {
                if (u.indexOf("?") == -1) {
                    u.append("?");
                } else {
                    u.append("&");
                }

                u.append(buildParameters());
            }

            connection = (HttpURLConnection) new URL(u.toString()).openConnection();
            connection.setConnectTimeout(timeout);

            if (headers.size() > 0) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    connection.setRequestProperty(h.getKey(), h.getValue());
                }
            }

            connection.setInstanceFollowRedirects(true);
            connection.connect();


            log.debug("HTTP CODE: " + connection.getResponseCode() + "; " + connection.getResponseMessage());
            if ((connection.getResponseCode() / 100) != 2) {

                log.error("ERROR: " + convertStreamToString(connection.getErrorStream()));
            }
        } else {
            log.warn("Already Have a Connection for " + url);
        }
        return this;
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
