/**
 * 
 */
package net.haibo.spdy.client.benchmark;

import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.haibo.spdy.client.CONTEXT;
import net.haibo.spdy.client.HttpRequest;
import net.haibo.spdy.client.RequestVisitor;
import net.haibo.spdy.client.MCSClient;
import net.haibo.spdy.client.SpdyHttpRequest;

import com.google.caliper.Param;

/**
 * Benchmark1
 */
public class Benchmark1 {
    
    /** How many requests to execute. */
    @Param({ "1", "1000" })
    int reqCount = 5;
 
    /** Select the sample client*/
    @Param
    MCSClient client = null;
    private Logger loger = null;

    public Benchmark1(MCSClient client, int reqCount, String logFile) {
        this.reqCount = reqCount;
        this.client = client;
        loger = Logger.getLogger(logFile);

        String logName = CONTEXT.LOG_FILE_PATH + logFile;
        try {
            FileHandler fh = new FileHandler(logName);
            loger.addHandler(fh);
            SimpleFormatter sf = new SimpleFormatter();
            fh.setFormatter(sf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        loger.setLevel(Level.ALL);
        loger.log(Level.INFO, "####$$ Start sequence bench mark with: ");
        loger.log(Level.INFO, "####Requst: " + client.getRequest());
        loger.log(Level.INFO, "####Url: " + client.getHost());
        loger.log(Level.INFO, "####Requst count: " + this.reqCount);
        loger.log(Level.INFO, "####Requst method: " + client.getMethod());
        loger.log(Level.INFO, "####$$\n\n");
    }

    private int failedRequestCount = 0;
    private long totalRequst = 0;
    private int passedRequestCount = 0;
    private int maxSpdyConnCount = 0;
    private int maxHttpConnCount = 0;
    private int maxConnCount = 0;
    public void run() throws Exception {
        long reportStart = System.nanoTime();
        int reports = 0;

        loger.log(Level.INFO, "!!!Start Time (nanoTime): [" + reportStart + "]");
        // Run until we've printed enough reports.
        while (reports < this.reqCount) {
            try {
                ++this.totalRequst;
                client.execute();
                // sampling the connection count through the visit interface
                this.client.getRequest().accept(new RequestVisitor() {
                    @Override public void visit(HttpRequest access) {
                        if (SpdyHttpRequest.class.isInstance(access)) {
                            SpdyHttpRequest req = (SpdyHttpRequest)access;
                            if (req.impl().getConnectionPool() != null) {
                                int total = req.impl().getConnectionPool().getConnectionCount();
                                int http = req.impl().getConnectionPool().getHttpConnectionCount();
                                int spdy = req.impl().getConnectionPool().getSpdyConnectionCount();
                                maxSpdyConnCount = Math.max(maxSpdyConnCount, spdy);
                                maxHttpConnCount = Math.max(maxHttpConnCount, http);
                                maxConnCount = Math.max(maxConnCount, total);
                                loger.info("##Connections>>(spdy,http,total): >>(" + spdy
                                        + ", " + http + ", " + total + ")");
                            }
                        }
                    }
                });
                ++this.passedRequestCount;
            } catch (Exception ex) {
                ++this.failedRequestCount;
            } finally {
                ++reports;
            }
        }

        long reportEnd = System.nanoTime();
        
        this.client.getRequest().accept(new RequestVisitor() { // Stop the connection pool
            @Override public void visit(HttpRequest access) {
                if (SpdyHttpRequest.class.isInstance(access)) {
                    SpdyHttpRequest req = (SpdyHttpRequest)access;
                    if (req.impl().getConnectionPool() != null) {
                        req.impl().getConnectionPool().evictAll();
                    }
                }
            }
        });

        loger.log(Level.INFO, "!!!End Time (nanoTime): [" + reportEnd + "]");

        long period = reportEnd - reportStart;
        loger.log(Level.INFO, "!!!Total use Time (nanoTime) : [" + (period) + "]");
        loger.log(Level.INFO, "!!!Total failed request (count) : [" + (failedRequestCount) + "]");
        loger.log(Level.INFO, "!!!Total passed request (count) : [" + (passedRequestCount) + "]");
        loger.log(Level.INFO, "!!!Total enqueue request (count) : [" + (totalRequst) + "]");
        long periodSeconds = TimeUnit.NANOSECONDS.toSeconds(period);
        periodSeconds = periodSeconds > 0 ? periodSeconds : 1;
        loger.log(Level.INFO, "!!!Average request (count) per second: [" + (totalRequst/periodSeconds) + "]");
        loger.info("!!!Total Connections>>(spdy,http,total): >>("+maxSpdyConnCount+", "
                +maxHttpConnCount+", "+maxConnCount+")");
    }
}