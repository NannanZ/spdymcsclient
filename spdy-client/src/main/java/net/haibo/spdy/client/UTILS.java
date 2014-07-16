/**
 * 
 */
package net.haibo.spdy.client;

import static org.apache.http.HttpVersion.HTTP_1_1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpResponse;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import net.haibo.spdy.client.CONTEXT.MockSetting;

/**
 * @author HAIBO
 * The utility alike functions provided for internal call in this package.
 */
public final class UTILS {
    /** Return a generic mcs kind query pairs for mcs post request */
    public static Map<String, String> createMcsDefaultQuery() {
        Map<String, String> query = new HashMap<String, String>();
        query.put("api_key", CONTEXT.API_KEY);
        query.put("v", CONTEXT.API_VERSION);
        query.put("uniq_id", CONTEXT.UDID);
        query.put("call_id", "" + utcTime());
        return query;
    }

    public static byte[] getBytes(File file){  
        byte[] buffer = null;  
        try {  
            FileInputStream fis = new FileInputStream(file);  
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);  
            byte[] b = new byte[1000];  
            int n;  
            while ((n = fis.read(b)) != -1) {  
                bos.write(b, 0, n);  
            }  
            fis.close();  
            bos.close();  
            buffer = bos.toByteArray();  
        } catch (IOException e) {
            e.printStackTrace();  
        }  
        return buffer;  
    }  
    
    public static String getShortName(String fileName){
        if(fileName != null && fileName.length()>0 && fileName.lastIndexOf(".")>-1){
            return fileName.substring(0, fileName.lastIndexOf("."));
        } 
        return fileName;
    }
    
    public static String getExtention(String fileName){
        if(fileName!=null && fileName.length()>0 && fileName.lastIndexOf(".")>-1){
            return fileName.substring(fileName.lastIndexOf(".")+1);
        }
        return "";
    }

    /** Package all mcs post query pairs according mcs access spec */
    public static String bowlingMcsPostData(Map<String, String> query, String secret) {
        StringBuilder sigBuilder = new StringBuilder();
        StringBuilder argsBuilder = new StringBuilder();
        
        List<String> keys = new ArrayList<String>(query.keySet());
        Collections.sort(keys, new Comparator<String>() {
            @Override public int compare(String arg0, String arg1) {
                return arg0.compareTo(arg1);
            }
        });
        
        for (String key : keys) {
            String value = query.get(key);
            value = value.length() <= 50 ? value : value.substring(0, 50);
            sigBuilder.append(String.format("%s=%s", key, value));
            try {
                argsBuilder.append(String.format("%s=%s&", key, URLEncoder.encode(query.get(key),"UTF-8")));
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Broken VM does not support UTF-8");
            }
        }
        sigBuilder.append(secret);
        // Create SIG
        String sig = MD5(sigBuilder.toString());
        argsBuilder.append("sig=" + sig);

        return argsBuilder.toString();
    }
    
    public static HttpResponse transformResponse(Response response) {
        int code = response.code();
        String message = response.message();
        BasicHttpResponse httpResponse = new BasicHttpResponse(HTTP_1_1, code, message);

        ResponseBody body = response.body();
        InputStreamEntity entity = new InputStreamEntity(body.byteStream(), body.contentLength());
        httpResponse.setEntity(entity);

        Headers headers = response.headers();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            String value = headers.value(i);
            httpResponse.addHeader(name, value);
            if ("Content-Type".equalsIgnoreCase(name)) {
                entity.setContentType(value);
            } else if ("Content-Encoding".equalsIgnoreCase(name)) {
                entity.setContentEncoding(value);
            }
        }

        return httpResponse;
    }

    /**
     * Appends a quoted-string to a StringBuilder.
     *
     * <p>RFC 2388 is rather vague about how one should escape special characters
     * in form-data parameters, and as it turns out Firefox and Chrome actually
     * do rather different things, and both say in their comments that they're
     * not really sure what the right approach is. We go with Chrome's behavior
     * (which also experimentally seems to match what IE does), but if you
     * actually want to have a good chance of things working, please avoid
     * double-quotes, newlines, percent signs, and the like in your field names.
     */
    public static StringBuilder appendQuotedString(StringBuilder target, String key) {
      target.append('"');
      for (int i = 0, len = key.length(); i < len; i++) {
        char ch = key.charAt(i);
        switch (ch) {
          case '\n':
            target.append("%0A");
            break;
          case '\r':
            target.append("%0D");
            break;
          case '"':
            target.append("%22");
            break;
          default:
            target.append(ch);
            break;
        }
      }
      target.append('"');
      return target;
    }
    /** String md5 encoding */
    public static String MD5(String s) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5bytes = messageDigest.digest(s.getBytes("UTF-8"));
//            return ByteString.of(md5bytes).hex();
            return toHex(md5bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
    
    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();
    /** Return hex encoded string from bytes */
    public static String toHex(byte... bs) {
        StringBuilder result = new StringBuilder(bs.length * 2);
        for (byte b : bs) {
            result.append(HEXDIGITS[(b>>4)&0xF]);
            result.append(HEXDIGITS[(b&0xF)]);
        }
        return result.toString();
    }
    
    /** Returns a mocked mcs client by using a mocked mcs method and mock setting */
    public static MCSClient createDefaultMockedClient(LoginInfo login, ICallback cb,
            TheRequest request, String endpoint) {
        MockSetting setting = CONTEXT.DUMMY.mockSetting();
        Map<String, String> query = new HashMap<String, String>();
        query.put("length", ""+setting.length);
        query.put("wait_time", ""+setting.waitTime);

        String method = "voip.allocate";

        MCSClient cl = new MCSClient(request.create())
            .withLogin(login)
            .withEndpoint(endpoint)
            .withMethod(method)
            .withCallback(cb)
            .withQueries(query);
        return cl;
    }
    
    public static MCSClient createDefaultFeedGet(TheRequest request, LoginInfo login, ICallback cb, String endpoint) {
        // Use "feed.get" as the sample command
        Map<String, String> query = new HashMap<String, String>();
        query.put("type", "102,103,104,107,110,501,502,504,"
                + "601,701,709,1101,1104,2002,2003,"
                + "2004,2005,2006,2008,2009,2012,2013,2015,2032");

        String method = "feed.get";

        MCSClient cl = new MCSClient(request.create())
            .withLogin(login)
            .withEndpoint(endpoint)
            .withMethod(method)
            .withCallback(cb)
            .withQueries(query);
        return cl;
    }
    
    public static MCSClient createDefaultUploadCase(TheRequest request,
            LoginInfo login, ICallback cb, String endpoint, List<File> files) {
        // Use "photos.uploadbin" as the sample command
        String method = "photos.uploadbin";

        MCSClient cl = new MCSClient(request.create())
            .withLogin(login)
            .withEndpoint(endpoint)
            .withMethod(method)
            .withCallback(cb)
            .withQueries(null)
            .withFiles(files);

        return cl;
    }
    
    public static LoginInfo getLogin() throws Exception {
        // For Login
        String url = CONTEXT.API_ENDPOINT;
        IHttpRequest req = TheRequest.Apache.create();

        CONTEXT.UserInfo info = CONTEXT.DUMMY.account();
        return new MCSClient(req).withEndpoint(url).login(info.userName, info.password);
    }
    
    public static String uploadBytes(TheRequest request, Map<String, String> query,
            List<File> files, String secret) throws Exception {
        final byte[] body = buildMultipartsBytes(query, files, secret);

        return request.create().post("http://10.4.24.161/api/photos/uploadbin", 
                    body, "multipart/form-data; charset=UTF-8; boundary=FlPm4LpSXsE");
    }
    
    /** Returns current UTC time */
    public static long utcTime() {
        Calendar c =  Calendar.getInstance();
        c.add(Calendar.MILLISECOND, -(c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)));
        long utc = c.getTimeInMillis();
        return utc;
    }
    
    private static final Random RANDOM = new Random();
    private static final String ALPHABET = new String("-abcdefghijklmnokprstuvwxyz");
    
    /** Return a length fixed and content randomed string. */
    public static String randomString(int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; ++i) {
            result[i] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        }
        return new String(result);
    }
    
    /** Returns a gzipped copy of {@code bytes}. */
    public static byte[] gzip(byte[] bytes) throws IOException {
        // decorator pattern
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        OutputStream gzippedOut = new GZIPOutputStream(bytesOut);
        gzippedOut.write(bytes);
        gzippedOut.close();
        return bytesOut.toByteArray();
    }
    
    /** Create a insecure allowed sll context */
    public static SSLContext createInsecureSslContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManager permissive = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException { }
                @Override public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException { }
                @Override public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            context.init(null, new TrustManager[]{ permissive }, null);
            return context;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** Create a insecure allowed sll socket factory through insecured ssl context */
    public static SSLSocketFactory createInsecureSslSocketFactory() {
        SSLContext context = createInsecureSslContext();
        return context.getSocketFactory();
    }

    /** Create an check-ignored host name verifier */
    public static HostnameVerifier createInsecureHostnameVerifier() {
        return new HostnameVerifier() {
            @Override public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
    }
    
    /** Create an check-ignored host name verifier for X509 certification */
    public static X509HostnameVerifier createInsecureX509HostnameVerifier() {
        return new X509HostnameVerifier() {
            @Override public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }

            @Override public void verify(String arg0, SSLSocket arg1) throws IOException { }
            @Override public void verify(String arg0, X509Certificate arg1) throws SSLException { }
            @Override public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException { }
        };
    }

    public static String createSig(Map<String, String> query, String secretKey) {
        StringBuilder sigBuilder = new StringBuilder();
        List<String> keys = new ArrayList<String>(query.keySet());
        Collections.sort(keys, new Comparator<String>() {
            @Override public int compare(String arg0, String arg1) {
                return arg0.compareTo(arg1);
            }
        });

        for (String key : keys) {
            String value = query.get(key);
            value = value.length() <= 50 ? value : value.substring(0, 50);
            sigBuilder.append(String.format("%s=%s", key, value));
        }
        sigBuilder.append(secretKey);
        return MD5(sigBuilder.toString());
    }
    
    
    public static byte[] buildMultipartsBytes(Map<String, String> query, 
            List<File> files, String secret) throws IOException {
        StringBuilder sigBuilder = new StringBuilder();

        List<String> keys = new ArrayList<String>(query.keySet());
        Collections.sort(keys, new Comparator<String>() {
            @Override public int compare(String arg0, String arg1) {
                return arg0.compareTo(arg1);
            }
        });
        
        String BOUNDARY = "FlPm4LpSXsE"; // separate line
        StringBuffer sb = new StringBuffer();
        
        for (String key : keys) {
            String value = query.get(key);
            
            sb.append("--");
            sb.append(BOUNDARY);
            sb.append("\r\n");
            sb.append("Content-Disposition: form-data; name=\""
                    + key + "\"\r\n\r\n");
            sb.append(value);
            sb.append("\r\n");
            
            value = value.length() <= 50 ? value : value.substring(0, 50);
            sigBuilder.append(String.format("%s=%s", key, value));
        }
        
        sigBuilder.append(secret);
        String sig = MD5(sigBuilder.toString());
        
        sb.append("--");
        sb.append(BOUNDARY);
        sb.append("\r\n");
        sb.append("Content-Disposition: form-data; name=\""
                + "sig" + "\"\r\n\r\n");
        sb.append(sig);
        sb.append("\r\n");
        
        byte[] beginData = sb.toString().getBytes("UTF-8");
        byte[] endData = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("UTF-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(beginData);
        
        for (File file : files) {
            StringBuilder sbb = new StringBuilder();
            sbb.append("--");
            sbb.append(BOUNDARY);
            sbb.append("\r\n");
            sbb.append("Content-Disposition: form-data;name=\"data\";filename=\""
                    + file.getName() + "\"\r\n");
            sbb.append("Content-Type: image/" + getExtention(file.getName()) + "\r\n\r\n");
            baos.write(sbb.toString().getBytes("UTF-8"));
            baos.write(getBytes(file));
        }

        baos.write(endData);
        return baos.toByteArray();
    }
}
