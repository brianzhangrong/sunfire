package com.ihomefnt.sunfire;

import static com.ihomefnt.sunfire.agent.utils.StringUtils.dateToRowkey;
import static com.ihomefnt.sunfire.agent.utils.StringUtils.join;
import static com.ihomefnt.sunfire.agent.utils.StringUtils.now;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ihomefnt.sunfire.agent.config.CuratorRegistry;
import com.ihomefnt.sunfire.agent.config.WatchDataNotify.DataEvent;
import com.ihomefnt.sunfire.agent.constant.SunfireConstant;
import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.ringbuffer.LoggerRingBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class SunfireAgent extends AbstractSink  implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(SunfireAgent.class);
    /**
     * habse rowkey 并发防重
     */
    private static final AtomicLong sequence = new AtomicLong(0);

    /**
     * zkclient
     */
    CuratorRegistry registry;
    /**
     * ringbuffer
     */
    LoggerRingBuffer ringBuffer;


    public SunfireAgent() {

    }

    /**
     * @see org.apache.flume.conf.Configurable#configure(org.apache.flume.Context)
     */
    @Override
    public void configure(Context context) {
        registry = new CuratorRegistry(context);
        ringBuffer = new LoggerRingBuffer(registry.initAppHbName());
        registry.addDataChangedNotify(DataEvent.HBTABLE_CHANGED_EVENT, (dbName) -> {
            ringBuffer.setDbName(dbName);
        });

    }
    /**
     * @see org.apache.flume.Sink#process()
     *|2018-08-13 17:28:42.548|http-nio-9999-exec-1|INFO |c.i.i.controller
     * .sunfireController|58f8ea8578ad486fa18a6a5102c3b8c8|-hear end,null
     */
    @Override
    public Status process() throws EventDeliveryException {
        Channel ch = getChannel();
        //get the transaction
        Transaction txn = ch.getTransaction();
        Event event =null;
        //begin the transaction
        txn.begin();
        while(true){
            event = ch.take();
            if (event!=null) {
                break;
            }
        }
        try {
            String body = new String(event.getBody());
            if (StringUtils.isNotBlank(body)) {
                LoggerData data = new LoggerData();
                data.setAppName(registry.getAppName());
                data.setCreateTime(now());
                data.setSplitExpress("");
                //traceId==rowkey作为reginserver的分区管理
                List <String> bodyList = Lists
                        .newArrayList(Splitter.on(SunfireConstant.LOG_SPLIT).split(body));
                if (!CollectionUtils.isEmpty(bodyList) && bodyList.size() > 1) {
                    //yyyy-MM-dd  hh:mm:ss.SSS 作为rowkey
                    data.set$rowKey(join(dateToRowkey(bodyList.get(1)),
                            String.valueOf(sequence.getAndIncrement())));
                    data.setLoggerTime(bodyList.get(1));
                    data.setBizName(bodyList.get(2));
                    List <String> contentList = Lists
                            .newArrayList(Splitter.on(registry.getBodySplit()).split(body));
                    if (!CollectionUtils.isEmpty(contentList) && contentList.size() > 1) {
                        data.setLoggerContent(contentList.get(1));
                    }
                    data.setTraceId(bodyList.get(5));
                    if (sequence.compareAndSet(SunfireConstant.SEQUENCE, 0)) {
                        logger.info("sequence set zeror");
                    }
                }
                data.set$openTSDB(registry.getOpenTSDBUrl());
                data.set$dbName(registry.initAppHbName());
                ringBuffer.publish(data);
            }
            txn.commit();
            return Status.READY;
        } catch (Throwable th) {
            SunfireAgent.logger.error("event process error:{}", ExceptionUtils.getStackTrace(th));
            txn.rollback();
            if (th instanceof Error) {
                throw (Error) th;
            } else {
                throw new EventDeliveryException(th);
            }
        } finally {
            txn.close();
        }
    }

}
