/**
 * 
 */
package net.haibo.spdy.client;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;

import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;


/**
 * <p>It's an implementation of abstract http request using OkHttp's spdy http client.
 * 
 * Reference statement from OkHttpClient:
 * Configures and creates HTTP connections. Most applications can use a single
 * OkHttpClient for all of their HTTP requests - benefiting from a shared
 * response cache, thread pool, connection re-use, etc.
 * 
 * <p><strong>Reference you might need:</strong>
 * <ul>
 *  <li>The <a href="http://square.github.io/okhttp/">OkHttp</a> Http Library.</li>
 *  <li>The <a href="https://httpbin.org/">httpbin</a> http testing host.</li>
 * </ul>
 * 
 * <p><strong>Regarding certification file</strong>
 * <ul>
 *  <li>How to get the self-signed certification, check 
 *  <a href="http://www.oschina.net/code/snippet_12_4092">here</a> and 
 *  <a href="http://wangrqa.blog.163.com/blog/static/17094682720133954149784/">here</a>.
 *  </li>
 * </ul>
 * <p>
 * Actually we recommend you can set this in the initialization stage of overall applicaton,
 * like the main function is good choice.
 * 
 * <p><strong>NOTE:</strong> <br/>
 * Above solution has been dropped, we will use the flag {@link SpdyHttpRequest#allowInsecure}
 * to indicate if allow to connection the self-signed https host, and if true, we 
 * will ignore the security check.
 *  
 */
public class SpdyHttpRequest implements HttpRequest {
    private boolean isForcedSpdy = false;
    public SpdyHttpRequest forceSpdy() {
        this.isForcedSpdy = true;
        return this;
    }

    /** to indicate if allow to connection the self-signed https host, and if true, we 
     *  will ignore the security check. */
    public static boolean allowInsecure = true;

    OkHttpClient client = new OkHttpClient();
    {
         client.setConnectionPool(ConnectionPool.getDefault());
         if (allowInsecure) {
             client.setSslSocketFactory(UTILS.createInsecureSslSocketFactory());
             client.setHostnameVerifier(UTILS.createInsecureHostnameVerifier());
         }
    }

    @Override
    public String get(String url) throws Exception {
        return this.request(url, null);
    }

    @Override
    public String post(String url, String dataWrapper) throws Exception {
        MediaType type = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(type, dataWrapper);

        return this.request(url, body);
    }

    @Override
    public String mutliPart(String url, Object body) throws Exception {
        if (!RequestBody.class.isInstance(body)) throw new InvalidParameterException();

        RequestBody parts = (RequestBody) body;
        return this.request(url, parts);
    }

    @Override
    public String post(String url, final byte[] data, String mediaType) throws Exception {
        RequestBody body = RequestBody.create(MediaType.parse(mediaType), data);
        return this.request(url, body);
    }

    private String request(String url, RequestBody body) throws Exception {
        Request.Builder builder = new Request.Builder()
            .url(url)
            .preferredProtocol(isForcedSpdy ? Protocol.SPDY_3 : null);

        if (body != null) { 
            builder.post(body);
        }

        Request request = builder.build();
        System.out.println("@$<< Executing request: " + request);
        client.setReadTimeout(20, TimeUnit.SECONDS);

        Response response = client.newCall(request).execute();
        HttpResponse rep = UTILS.transformResponse(response);
        System.out.println(rep); // try print the apache alike response

        String result = response.body().string();
        System.out.println("DUMP(" + response.protocol() + "): " + result);

        return result;
    }

    @Override public HttpRequest accept(RequestVisitor v) {
        v.visit(this); return this;
    }

    public OkHttpClient impl() {
        return this.client;
    }

    @Override
    public Object buildMutliparts(Map<String, String> query, List<File> files) {
        MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM);
        for (String key : query.keySet()) {
            String value = query.get(key);
            StringBuilder disposition = new StringBuilder("form-data; name=");
            UTILS.appendQuotedString(disposition, key);
            builder.addPart(Headers.of("Content-Disposition", disposition.toString()), RequestBody.create(null, value));
        }

        for (File file : files) {
            StringBuilder disposition = new StringBuilder("form-data; name=");
            UTILS.appendQuotedString(disposition, "data");
            disposition.append("; filename=");
            UTILS.appendQuotedString(disposition, file.getName());
            
            builder.addPart(Headers.of("Content-Disposition", disposition.toString()),
                    RequestBody.create(MediaType.parse("image/" + UTILS.getExtention(file.getName())), file));
        }

        return builder.build();
    }
}