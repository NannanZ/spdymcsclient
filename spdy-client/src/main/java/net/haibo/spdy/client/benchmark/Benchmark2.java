/**
 * 
 */
package net.haibo.spdy.client.benchmark;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import net.haibo.spdy.client.CONTEXT;
import net.haibo.spdy.client.CountedFlyweight;
import net.haibo.spdy.client.IHttpRequest;
import net.haibo.spdy.client.IRequestVisitor;
import net.haibo.spdy.client.MCSClient;
import net.haibo.spdy.client.SpdyHttpRequest;
import net.haibo.spdy.client.TheRequest;

import com.google.caliper.Param;

/**
 * @author HAIBO
 *
 */
public class Benchmark2 {
    
    /** How many concurrent request channels to execute. */
    @Param({ "1", "10" })
    int concurrencyLevel = 5;
    
    ThreadPoolExecutor executor;
 
    /** Select the sample client*/
    @Param
    MCSClient client = null;
    
    /** Select the request round */
    @Param({ "1", "100" })
    int reqRounds = 10;
    
    /** How many requests to enqueue to await threads to execute them. */
    @Param({ "20" })
    int targetBacklog = 500;
    int threadCount = 50;
    TheRequest reqKind = null;

    private Logger logger = null;
    private boolean enableSpdyDifChannels = false;

    public Benchmark2(MCSClient client, TheRequest reqKind,
            int reqRounds, String logFileName, int concurrencyLevel, boolean enableSpdyDifChannels) {
        this.reqKind = reqKind;
        this.enableSpdyDifChannels = enableSpdyDifChannels;
        this.concurrencyLevel = this.targetBacklog = concurrencyLevel;
        this.client = client;
        this.reqRounds = reqRounds;

        executor = new ThreadPoolExecutor(threadCount, threadCount, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        // If enable the spdy multi-channel testing, we will need use the counted factory to generate
        // spdy kind of request that doesn't share the spdy channel through bellow workaround way.
        if (this.enableSpdyDifChannels && this.reqKind != null && this.client != null) {
            this.countedFacotry = new CountedFlyweight<MCSClient>(this.concurrencyLevel, 
                    new CountedFlyweight.Creatable<MCSClient>() {
                @Override public MCSClient create() {
                    return Benchmark2.this.client.copyWith(
                            Benchmark2.this.reqKind.create().accept(new IRequestVisitor() {
                        @Override public void visit(IHttpRequest access) {
                            if (SpdyHttpRequest.class.isInstance(access)) {
                                SpdyHttpRequest req = (SpdyHttpRequest)access;
                                // Note: It's a workaround to customizing a client 
                                // which does not share the spdy connection.
                                // It's usful for both https nd http host, currently.
                                HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                                    @Override public boolean verify(String s, SSLSession session) {
                                        return true;
                                    }
                                };
                                req.impl().setHostnameVerifier(hostnameVerifier);
                            }
                        }
                    }));
                }
            });
        } else { this.countedFacotry = null; }

        enableLogging(logFileName);

        logger.info("---------------------------------------------------------------------------");
        logger.log(Level.INFO, "##>> Start benchmark2 with parameters: ");
        logger.log(Level.INFO, "##Requst: " + client.getRequest());
        logger.log(Level.INFO, "##Url: " + client.getHost());
        logger.log(Level.INFO, "##TargetBacklog: " + targetBacklog);
        logger.log(Level.INFO, "##ThreadCount: " + threadCount);
        logger.log(Level.INFO, "##Requst rounds: " + reqRounds);
        logger.log(Level.INFO, "##Requst method: " + client.getMethod());
        logger.log(Level.INFO, "##ConcurrencyLevel(Channel count): " + concurrencyLevel);
        logger.log(Level.INFO, "##EnableSpdyDifChannels: " + enableSpdyDifChannels);
        logger.log(Level.INFO, "##LogFileName: " + logFileName);
        logger.info("---------------------------------------------------------------------------");
    }
  
    private final CountedFlyweight<MCSClient> countedFacotry;
    private final AtomicInteger failedRequestCount = new AtomicInteger(0);
    private final AtomicInteger totalRequst = new AtomicInteger(0);
    private final AtomicInteger passedRequestCount = new AtomicInteger(0);

    private int maxSpdyConnCount = 0;
    private int maxHttpConnCount = 0;
    private int maxConnCount = 0;
    public void run() throws Exception {
        int requestRoundCount = 0;
        long requestCount = 0;
        long start = System.nanoTime();
        long reportStart = System.nanoTime();
        final long PERIOD = TimeUnit.SECONDS.toNanos(5); // report once every 5s
        int reports = 0;
        double best = 0.0;

        logger.log(Level.INFO, "!!!Start Time (nanoTime): [" + start + "]");
        logger.log(Level.INFO, "!!!We check " + reqRounds + " rounds" + ", and every round need 1 second");
        // Run until we've printed enough reports.
        while (reports < reqRounds) {
            // Print a report if we haven't recently.
            long now = System.nanoTime();
            double reportDuration = now - reportStart;
            if (reportDuration > PERIOD) {
                // p = c / d * t(1)
                double requestsPerSecond = requestRoundCount / reportDuration * TimeUnit.SECONDS.toNanos(1);
                if (true) {
                    logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                    logger.info(String.format("##Requests per second: %.1f with rounds [%d]", requestsPerSecond, reports));
                    logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                }
                best = Math.max(best, requestsPerSecond);
                requestRoundCount = 0;
                reportStart = now;
                reports++;
            }

            // Fill the job queue with work.
            while (this.needMore()) {
                requestCount++;
                logger.log(Level.INFO, "Enqueue task [" + (requestCount) +"]");
                if (enableSpdyDifChannels && this.reqKind != null && this.countedFacotry != null) {
                    // always create counted different requests which does not share the spdy connection.
                    // and they will be shared for next counted round.
                    this.enqueue(countedFacotry.next());
                } else {
                    this.enqueue(this.client);
                }
                requestRoundCount++;
            }
            
            this.client.getRequest().accept(new IRequestVisitor() { // dump the connection pool status
                @Override public void visit(IHttpRequest target) {
                    if (SpdyHttpRequest.class.isInstance(target)) {
                        SpdyHttpRequest req = (SpdyHttpRequest)target;
                        if (req.impl().getConnectionPool() != null) {
                            int total = req.impl().getConnectionPool().getConnectionCount();
                            int http = req.impl().getConnectionPool().getHttpConnectionCount();
                            int spdy = req.impl().getConnectionPool().getSpdyConnectionCount();
                            maxSpdyConnCount = Math.max(maxSpdyConnCount, spdy);
                            maxHttpConnCount = Math.max(maxHttpConnCount, http);
                            maxConnCount = Math.max(maxConnCount, total);
                            logger.info("##Connections >> (spdy,http,total): >> (" + spdy + ", " + http + ", " + total + ")");
                        }
                    }
                }
            });

            // The job queue is full. Take a break.
            sleep(1);
        }
        
        // wait till the totally completed
        while (totalRequst.get() != failedRequestCount.get() + passedRequestCount.get()) {
            logger.log(Level.INFO, "##Waitting for completing: with task count: " + this.executor.getQueue().size());
            sleep(1);
        }
        long end = System.nanoTime();
        this.client.getRequest().accept(new IRequestVisitor() { // Stop the connection pool
            @Override public void visit(IHttpRequest access) {
                if (SpdyHttpRequest.class.isInstance(access)) {
                    SpdyHttpRequest req = (SpdyHttpRequest)access;
                    if (req.impl().getConnectionPool() != null) {
                        req.impl().getConnectionPool().evictAll();
                    }
                }
            }
        });

        this.executor.shutdown();
        long period = end - start;
        logger.info(":::::::::::::::::::::::::::::::::::::::::::::::RESULT::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
        logger.log(Level.INFO, "::End Time (nanoTime): [" + end + "]");
        logger.log(Level.INFO, "::Total use Time (nanoTime) : [" + (period) + "]");
        logger.log(Level.INFO, "::Total failed requests (count) : [" + (failedRequestCount) + "]");
        logger.log(Level.INFO, "::Total passed requests (count) : [" + (passedRequestCount) + "]");
        logger.log(Level.INFO, "::Total enqueue requests (count) : [" + (totalRequst) + "]");
        logger.log(Level.INFO, "::Total max requests (count) per second: [" + (best) + "]");
        long periodSeconds = TimeUnit.NANOSECONDS.toSeconds(period); periodSeconds = periodSeconds > 0 ? periodSeconds : 1;
        logger.log(Level.INFO, "::Average requests (count) per second: [" + (totalRequst.get()/periodSeconds) + "]");
        logger.info("::Max Connections >> (spdy,http,total): >> ("+maxSpdyConnCount+", "+maxHttpConnCount+", "+maxConnCount+")");
        logger.info("::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
        
        sleep(10000); // wait for a moment for next step testing
    }
    
    public synchronized void enqueue(MCSClient client) {
        executor.execute(request(client));
    }

    public synchronized boolean needMore() {
        return executor.getQueue().size() < targetBacklog;
    }

    private Runnable request(MCSClient client) {
        return new ClientRunner(client);
    }
    
    class ClientRunner implements Runnable {
        private final MCSClient client;
        public ClientRunner(MCSClient client) {
            this.client = client;
        }

        @Override public void run() {
            try {
//                sleep(2);
                Benchmark2.this.totalRequst.incrementAndGet();
                this.client.execute();
                Benchmark2.this.passedRequestCount.incrementAndGet();
            } catch (Exception e) {
                Benchmark2.this.failedRequestCount.incrementAndGet();
                e.printStackTrace();
            }
        }
    }

    private void sleep(int millis) {
        try {
          Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
    
    /**
     * @param fileLogName
     */
    private void enableLogging(String fileLogName) {
        logger = Logger.getLogger(fileLogName);
        logger.setLevel(Level.ALL);

        try {
            Handler handler = null;
            if (fileLogName != null && !fileLogName.equals("")) {
                String fullName = CONTEXT.LOG_FILE_PATH + fileLogName;
                handler = new FileHandler(fullName);
            } else {
                handler = new ConsoleHandler();
            }

            handler.setFormatter(new SimpleFormatter() {
              @Override public String format(LogRecord record) {
                return String.format("%s%n", record.getMessage());
              }
            });
            logger.addHandler(handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
