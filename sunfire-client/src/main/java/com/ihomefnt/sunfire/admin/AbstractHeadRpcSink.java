package com.ihomefnt.sunfire.admin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ihomefnt.sunfire.admin.utils.NetUtils;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.flume.Channel;
import org.apache.flume.ChannelException;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.FlumeException;
import org.apache.flume.Transaction;
import org.apache.flume.api.RpcClient;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractRpcSink;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHeadRpcSink extends AbstractSink implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRpcSink.class);
    private final int DEFAULT_CXN_RESET_INTERVAL = 0;
    private final ScheduledExecutorService cxnResetExecutor = Executors
            .newSingleThreadScheduledExecutor(
                    (new ThreadFactoryBuilder()).setNameFormat("Rpc Sink Reset Thread").build());
    private String hostname;
    private Integer port;
    private RpcClient client;
    private Properties clientProps;
    private SinkCounter sinkCounter;
    private int cxnResetInterval;
    private AtomicBoolean resetConnectionFlag;

    public AbstractHeadRpcSink() {
    }

    @Override
    public void configure(Context context) {
        clientProps = new Properties();
        hostname = context.getString("hostname");
        port = context.getInteger("port");
        Preconditions.checkState(hostname != null, "No hostname specified");
        Preconditions.checkState(port != null, "No port specified");
        clientProps.setProperty("hosts", "h1");
        clientProps.setProperty("hosts.h1", hostname + ":" + port);
        UnmodifiableIterator var2 = context.getParameters().entrySet().iterator();

        while (var2.hasNext()) {
            Entry <String, String> entry = (Entry) var2.next();
            clientProps.setProperty((String) entry.getKey(), (String) entry.getValue());
        }

        if (sinkCounter == null) {
            sinkCounter = new SinkCounter(getName());
        }

        cxnResetInterval = context.getInteger("reset-connection-interval", 0);
        if (cxnResetInterval == 0) {
            logger.info("Connection reset is set to " + String.valueOf(0)
                    + ". Will not reset connection to next hop");
        }

    }

    protected abstract RpcClient initializeRpcClient(Properties var1);

    private void createConnection() throws FlumeException {
        if (client == null) {
            logger.info("Rpc sink {}: Building RpcClient with hostname: {}, port: {}",
                    new Object[]{getName(), hostname, port});

            try {
                resetConnectionFlag = new AtomicBoolean(false);
                client = initializeRpcClient(clientProps);
                Preconditions.checkNotNull(client,
                        "Rpc Client could not be initialized. " + getName()
                                + " could not be started");
                sinkCounter.incrementConnectionCreatedCount();
                if (cxnResetInterval > 0) {
                    cxnResetExecutor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            resetConnectionFlag.set(true);
                        }
                    }, (long) cxnResetInterval, TimeUnit.SECONDS);
                }
            } catch (Exception var2) {
                sinkCounter.incrementConnectionFailedCount();
                if (var2 instanceof FlumeException) {
                    throw (FlumeException) var2;
                }

                throw new FlumeException(var2);
            }

            logger.debug("Rpc sink {}: Created RpcClient: {}", getName(), client);
        }

    }

    private void resetConnection() {
        try {
            destroyConnection();
            createConnection();
        } catch (Throwable var2) {
            logger.error("Error while trying to expire connection", var2);
        }

    }

    private void destroyConnection() {
        if (client != null) {
            logger.debug("Rpc sink {} closing Rpc client: {}", getName(), client);

            try {
                client.close();
                sinkCounter.incrementConnectionClosedCount();
            } catch (FlumeException var2) {
                sinkCounter.incrementConnectionFailedCount();
                logger.error("Rpc sink " + getName()
                        + ": Attempt to close Rpc client failed. Exception follows.", var2);
            }
        }

        client = null;
    }

    private void verifyConnection() throws FlumeException {
        if (client == null) {
            createConnection();
        } else if (!client.isActive()) {
            destroyConnection();
            createConnection();
        }

    }

    @Override
    public void start() {
        logger.info("Starting {}...", this);
        sinkCounter.start();

        try {
            createConnection();
        } catch (FlumeException var2) {
            logger.warn(
                    "Unable to create Rpc client using hostname: " + hostname + ", port: " + port,
                    var2);
            destroyConnection();
        }

        super.start();
        logger.info("Rpc sink {} started.", getName());
    }

    @Override
    public void stop() {
        logger.info("Rpc sink {} stopping...", getName());
        destroyConnection();
        cxnResetExecutor.shutdown();

        try {
            if (cxnResetExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
                cxnResetExecutor.shutdownNow();
            }
        } catch (Exception var2) {
            logger.error("Interrupted while waiting for connection reset executor to shut down");
        }

        sinkCounter.stop();
        super.stop();
        logger.info("Rpc sink {} stopped. Metrics: {}", getName(), sinkCounter);
    }

    @Override
    public String toString() {
        return "RpcSink " + getName() + " { host: " + hostname + ", port: " + port + " }";
    }

    @Override
    public Status process() throws EventDeliveryException {
        Status status = Status.READY;
        Channel channel = getChannel();
        Transaction transaction = channel.getTransaction();
        if (resetConnectionFlag.get()) {
            resetConnection();
            resetConnectionFlag.set(false);
        }

        try {
            transaction.begin();
            verifyConnection();
            List <Event> batch = Lists.newLinkedList();

            int size;
            for (size = 0; size < client.getBatchSize(); ++size) {
                Event event = channel.take();
                if (event == null) {
                    break;
                }
                event.getHeaders().put("ip", NetUtils.getLocalHostLANAddress().getHostAddress());
                System.out.println(
                        "agent apppendip---------------" + NetUtils.getLocalHostLANAddress()
                                .getHostAddress());
                batch.add(event);
            }

            size = batch.size();
            int batchSize = client.getBatchSize();
            if (size == 0) {
                sinkCounter.incrementBatchEmptyCount();
                status = Status.BACKOFF;
            } else {
                if (size < batchSize) {
                    sinkCounter.incrementBatchUnderflowCount();
                } else {
                    sinkCounter.incrementBatchCompleteCount();
                }

                sinkCounter.addToEventDrainAttemptCount((long) size);
                client.appendBatch(batch);
            }

            transaction.commit();
            sinkCounter.addToEventDrainSuccessCount((long) size);
        } catch (Throwable var10) {
            transaction.rollback();
            if (var10 instanceof Error) {
                throw (Error) var10;
            }

            if (!(var10 instanceof ChannelException)) {
                destroyConnection();
                throw new EventDeliveryException("Failed to send events", var10);
            }

            logger.error("Rpc Sink " + getName() + ": Unable to get event from channel " + channel
                    .getName() + ". Exception follows.", var10);
            status = Status.BACKOFF;
        } finally {
            transaction.close();
        }

        return status;
    }

    @VisibleForTesting
    RpcClient getUnderlyingClient() {
        return client;
    }
}
