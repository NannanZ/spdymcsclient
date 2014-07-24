package net.haibo.spdy.client;

import static com.squareup.okhttp.internal.Util.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.apache.OkApacheClient;

//import java.io.FileInputStream;

/**
 * Hello MCS!
 */
public class App implements MCSCallback
{
//    static Logger logger = Logger.getLogger("net.haibo.spdy.client.App");
    public static void main(String[] args)
    {
        try {
            System.out.println( "Hello MCS!" );

            // HELP:

            // This configuration is just set for self-signed https host, you can follow below refer:
            //  http://www.oschina.net/code/snippet_12_4092 
            //  and
            //  http://wangrqa.blog.163.com/blog/static/17094682720133954149784/
            // NOTE: This config has been dropped, pls using <code>allowInsecure</code>.
//            System.setProperty("javax.net.ssl.trustStore", CONTEXT.CERTIFICATION_FILE_PATH);

            // This configuration is set for max connection count of okhttp connection pool
            // There are some other relevant setting terms, you can check them from okhttp's connectionpool.java
            System.setProperty("http.maxConnections", "10");

            // PREVIEW:
            // The okHttp by default, just supports the spdy 3.1 and http2.0, so if the host
            // uses spdy 2.0 or spdy 3.0, the okhttp will automaticall turns to http 1.1
            // for data transportation.
            // If you want to preferred one kind of protocol for a single host,
            // you could use config the preferred protocol for each okhttp client:
            /** <code>
            // firstly, we create a okhttp lib client for further using
            OkHttpClient ok = new OkHttpClient();
            // we config a host with a prefered portocol kind
            // actally http://118.186.217.31 hosts with a spdy style http service
            ok.configPreferredHostProtocol("118.186.217.31", Protocol.SPDY_3); 
            */
            // OR you could set the preferred protocol for each request before send it:
            /** <code> 
             // Create request for remote resource.
            Request request = new Request.Builder()
                .url(url)
                .preferredProtocol(isForcedSpdy ? Protocol.SPDY_3 : null)
                .build();
             </code>
             */

            // SAMPLE URLS with spdy host:
//            String url = "https://www.google.com.hk/webhp?hl=zh-CN";
//            String url = "https://www.cloudflare.com"; // spdy 3.1 non-auto to spdy
//            String url  = "https://webtide.com/"; // spdy 3.0 auto to spdy
//            String url = "https://123.125.36.108:443";
//            String url = "http://123.125.36.108:443";
////            String url = "http://118.186.217.31";
//            int count = 4;
////            IHttpRequest request = TheRequest.Spdy.create();
//            for (int i = 0; i < count; ++i) {
//                String the = TheRequest.Apache.create().get(url);
//            }

            // Important note: according the implemenation of okhttp,
            // If you want to share the conncetion in the whole application for 
            // reducing network latency, your requests need qualifying 
            // with the same address(defined by okhttp),
            // which means you should make follow call to be true:
            // Address.equals() note:http and https have different terms
            // So sharing an OkHttpClient instance is a recommanded way but not the least.
            // bollow http get requests share the same OkHttpClient instance
//            IHttpRequest request = TheRequest.OkHttp.create();
//            for (int i = 0; i < 4; ++i) {
//                String the = request.get(url);
//            }
            
            // Important issue (maybe an okhttp's bug):
            // If we don't share the client instance between the different requsts.
            // Which means all request actually don't share the connection including spdy connection.
            // The ALPN call will report a debug error info:
            // INFO: NPN/ALPN callback dropped: SPDY and HTTP/2 are disabled. Is npn-boot or alpn-boot on the boot class path?
            // Follow is the reproduce code
//            for (int i = 0; i < count; ++i) {
//                String the = TheRequest.OkHttp.create().get(url);
//            }
            
            // It's a smoke testing
            sequenceSmokeTesting();
            testMutiPart();
            // It's a apache http alike api sample with okhttp lib's implementation
            apacheAlikeApiSample();
            
            // Ensure the deamon threads exit
            ConnectionPool.getDefault().evictAll();
        } catch (Exception ex) {
            System.out.println("\n\n### APP LEVEL ERROR: \n");
            ex.printStackTrace();
        }
    }

    private static void testMutiPart() throws Exception {
        LoginInfo login = UTILS.getLogin();
        Map<String, String> query = UTILS.createMcsDefaultQuery();
        query.put("session_key", login.getInfo().session_key);
        query.put("method", "photos.uploadbin");
        
        List<File> files = new ArrayList<>();
        files.add(new File("resource/images/1.jpg"));
        UTILS.uploadBytes(TheRequest.Spdy, query, files, login.getInfo().secret_key);

        files.clear(); 
        files.add(new File("resource/images/2.jpg"));
        UTILS.createDefaultUploadCase(TheRequest.Spdy, login, null, "http://118.186.217.31/api/", files)
        .execute();
    }

    private static void sequenceSmokeTesting() throws Exception {
        LoginInfo login = UTILS.getLogin();
//        MCSClient feeds = UTILS.createDefaultFeedGet(TheRequest.Spdy, login, null, "http://118.186.217.31/api/");
        MCSClient feeds = UTILS.createDefaultFeedGet(TheRequest.Spdy, login, null, "http://api.m.renren.com/api/");
        for (int i = 0; i < 20; ++i) {
            System.out.println(">>S"+i);
            feeds.execute();
        }

        TheRequest.OkHttp.create().get("https://118.186.217.31/");
    }

    private static void apacheAlikeApiSample() throws Exception {
        System.out.println("::: It's a apache client alike api usage.");
        // ::: It's a apache client alike api usage.
        // Create a apache kind of http client which you can share in whole application
        OkApacheClient client = new OkApacheClient();
        client.impl().setHostnameVerifier(UTILS.createInsecureHostnameVerifier());
        client.impl().setSslSocketFactory(UTILS.createInsecureSslSocketFactory());
        // Config the preferred protocol for special host you prefer to invoke with special protocol. 
        client.configPreferredHostProtocol("118.186.217.31", Protocol.SPDY_3);
        // http://118.186.217.31 hosts with a spdy style http service, we create a get request for it
        HttpGet request = new HttpGet(new URL("http://118.186.217.31/").toURI());
//        HttpGet request = new HttpGet(new URL("https://www.cloudflare.com/").toURI());
        // then run it.
        HttpResponse response = client.execute(request);
        // should OUTPUT:
        // HTTP/1.1 200 OK [OkHttp-Selected-Protocol: spdy/3.1, server: nginx/1.5.11, date: \
        // Thu, 12 Jun 2014 09:09:34 GMT, content-type: text/html; charset=UTF-8, alternate-protocol:\
        // 443:npn-spdy/3, OkHttp-Sent-Millis: 1402564173672, OkHttp-Received-Millis: 1402564173708]\
        // HTTP/1.1 200 OK [OkHttp-Selected-Protocol: spdy/3.1, server: cloudflare-nginx, date: \
        // Wed, 18 Jun 2014 10:35:44 GMT, content-type: text/html; charset=UTF-8, set-cookie: \
        // __cfduid=dec44ce8ac515d613f7490568f39612f21403087744468; expires=Mon, 23-Dec-2019 23:50:00 GMT; \
        // path=/; domain=.cloudflare.com; HttpOnly, expires: Wed, 18 Jun 2014 14:35:44 GMT, cache-control: \
        // public, max-age=14400, pragma: no-cache, x-frame-options: DENY, vary: Accept-Encoding, \
        // strict-transport-security: max-age=31536000, cf-cache-status: HIT, cf-ray: 13c6d782e0ac0d31-LAX, \
        // OkHttp-Sent-Millis: 1403087740701, OkHttp-Received-Millis: 1403087741183]

        System.out.println(response);
        String actual = EntityUtils.toString(response.getEntity(), UTF_8);
        System.out.println(actual);
        System.out.println(":::\n");

        System.out.println("::: It's a Url connection alike api usage.");
        // ::: It's a Url connection alike api usage
        // firstly, we create a okhttp lib client for further using
        OkHttpClient ok = new OkHttpClient();
        ok.setHostnameVerifier(UTILS.createInsecureHostnameVerifier());
        ok.setSslSocketFactory(UTILS.createInsecureSslSocketFactory());
        // we config a host with a prefered portocol kind
        // actally http://118.186.217.31 hosts with a spdy style http service
//        ok.configPreferredHostProtocol("118.186.217.31", Protocol.SPDY_3);
        // with the customized client, create a url factory
        OkUrlFactory factory = new OkUrlFactory(ok);
        // open an connection from a URL
        HttpURLConnection connection = factory.open(new URL("https://118.186.217.31"));
        connection.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8));
        for (String line = null; (line = reader.readLine()) != null;) System.out.println(line);
        reader.close();
        connection.disconnect();
        System.out.println(":::\n");
    }

    @Override
    public void arrives(MCSClient sender, String response) {
        System.out.println("##=>Mcs reponse arrives: " +  response);
    }
}
