/**
 * 
 */
package net.haibo.spdy.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** It can be Java's Runnable? wait for refining */
interface Executable {
    public String execute() throws Exception;
}

/** Mcs Batchrun is one kind of composited mcs client */
abstract class IMcsBatchrun extends MCSClient {
    public IMcsBatchrun(HttpRequest http) {
        super(http);
    }

    public abstract IMcsBatchrun accept(MCSClient... cs);
}

/**
 * Define a http request client regarding the mcs api spec.
 * The real http request/reponse will be invoked by IHttpRequst
 * interface that's injected by the constructor.
 * <p>
 * You can new a client object direclty with shared http rquest instance.
 * And also, you can inject the relevant http request implementation anytime
 * with through constructor function before call the execute or request method.
 * <p>
 * <h3>Regarding {@linkplain MCSClient#batchrun(HttpRequest)}</h3>
 * <br>Whose logic also be ruled by the mcs api spec.
 * It's a static factory alike method, you can call this method without client
 * instance to create a batchrun instance, since batchrun class is compositor 
 * pattern class, so it provide the accept method to composite muli-clients in it, and 
 * then execute it using the executalbe functionality .
 */
public class MCSClient implements Executable {
    private String endpoint;
    protected LoginInfo login;
    private HttpRequest http;
    private String method; // Mcs method,eg. feed.get
    private Map<String, String> queries;
    private List<File> files;
    private MCSCallback cb;
    
    public MCSClient(HttpRequest http) { 
        // Ensure it has a valid reference
        assert(http != null);
        this.http = http;
    }

    public MCSClient withEndpoint(String url) {
        this.endpoint = url; return this;
    }
    public MCSClient withLogin(LoginInfo info) {
        this.login = info; return this;
    }
    public MCSClient withMethod(String m) {
        this.method = m; return this;
    }
    public MCSClient withQueries(Map<String, String> queries) {
        this.queries = queries; return this;
    }
    /** With files means upload with multipart post  */
    public MCSClient withFiles(List<File> files) {
        this.files = files; return this;
    }
    public MCSClient withCallback(MCSCallback c) {
        this.cb = c; return this;
    }
    
    public String getHost() {
        return endpoint;
    }
    public String getMethod() {
        return method;
    }
    public Map<String, String> getQuery() {
        return queries;
    }
    public MCSCallback getCallback() {
        return cb;
    }
    public HttpRequest getRequest() {
        return http;
    }

    /** The batch run logic is defined in the mcs api spec */
    public static IMcsBatchrun batchrun(HttpRequest http) {
        return new IMcsBatchrun(http) {
            @Override public String execute() throws Exception {
                assert(cs != null);
                
                StringBuilder builder = new StringBuilder();
                builder.append("[");
                for (MCSClient c : cs) {
                    String wrapper = c.buildWraperData(c.getQuery());
                    builder.append("\"");
                    builder.append(wrapper);
                    builder.append("\"");
                    builder.append(",");
                }
                builder.deleteCharAt(builder.length() - 1).append("]");
                String ms = builder.toString();
                
                Map<String, String> query = new HashMap<String, String>();
                query.put("method_feed", ms);
                query.put("method", "batch.run");
                super.withMethod("batch.run");
                String wrapper = super.buildWraperData(query);

                String response = super.getRequest().post(super.getHost() + "batch/run", wrapper);

                // Deserialize HTTP response to concrete type.
                BatchrunInfo batch = new BatchrunInfo(response);
                if (batch.isValid()) {
                    Map<String, String> info = batch.getInfo();
                    for (MCSClient c : cs) {
                        // Notify all clients
                        if (c.getCallback() != null) {
                            c.getCallback().arrives(c, info.get(c.getMethod()));
                        }
                    }
                }
                return response;
            }

            private List<MCSClient> cs = null;
            @Override public IMcsBatchrun accept(MCSClient... cs) {
                this.cs = new ArrayList<MCSClient>(Arrays.asList(cs));
                return this;
            }
        };
    }

    public LoginInfo login(String userName, String password) throws Exception {
        // Globally NOTICE: Do not keep the user name and password in memory.
        // Create request for remote resource.
        Map<String, String> query = UTILS.createMcsDefaultQuery();
        query.put("user", userName);
        query.put("password", UTILS.MD5(password));
        String wrapper = UTILS.bowlingMcsPostData(query, CONTEXT.SECRET_KEY);
        String url = endpoint != null ? endpoint : CONTEXT.API_ENDPOINT;
        url = url + "client/login";
        // Execute the request and retrieve the response.
        String response = http.post(url, wrapper);

        // Deserialize HTTP response to concrete type.
        return new LoginInfo(response);
    }

    protected Object buildMutliparts(Map<String, String> queryData, List<File> files) {
        assert(login != null && login.isValid());
        assert(files != null && files.size() > 0);
        assert(http != null);

        Map<String, String> query = UTILS.createMcsDefaultQuery();
        if (queryData != null) query.putAll(queryData);
        query.put("session_key", login.getInfo().session_key);
        query.put("method", this.method);

        String sig = UTILS.createSig(query, login.getInfo().secret_key);
        query.put("sig", sig);

        return http.buildMutliparts(query, files);
    }

    protected String buildWraperData(Map<String, String> queryData) {
        assert(login != null && login.isValid());
        assert(this.method != null);
        Map<String, String> query = UTILS.createMcsDefaultQuery();
        if (queryData != null) query.putAll(queryData);
        query.put("session_key", login.getInfo().session_key);
        query.put("method", this.method);
        String wrapper = UTILS.bowlingMcsPostData(query, login.getInfo().secret_key);
        return wrapper;
    }

    @Override public String execute() throws Exception {
        assert(login != null && login.isValid());

        String url = endpoint != null ? endpoint + method.replace('.', '/') : CONTEXT.API_ENDPOINT;
        String response;
        // Execute the request and retrieve the response.
        if (files == null) {
            String wrapper = buildWraperData(this.queries);
            response = http.post(url, wrapper);
        } else {
            Object parts = buildMutliparts(this.queries, this.files);
            response = http.mutliPart(url, parts);
        }

        if (cb != null) {
            cb.arrives(this, response);
        }

        // deserializes HTTP response to concrete type.
        return response;
    }
    
    public MCSClient copyWith(HttpRequest http) {
        return new MCSClient(http)
            .withCallback(this.cb)
            .withEndpoint(this.endpoint)
            .withLogin(this.login)
            .withMethod(this.method)
            .withQueries(this.queries)
            .withFiles(this.files);
    }
}
