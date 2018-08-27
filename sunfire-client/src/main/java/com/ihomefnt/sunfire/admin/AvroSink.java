package com.ihomefnt.sunfire.admin;

import java.util.Properties;
import org.apache.flume.api.RpcClient;
import org.apache.flume.api.RpcClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroSink extends AbstractHeadRpcSink {

    private static final Logger logger = LoggerFactory
            .getLogger(org.apache.flume.sink.AvroSink.class);

    @Override
    protected RpcClient initializeRpcClient(Properties props) {
        logger.info("Attempting to create Avro Rpc client.");
        return RpcClientFactory.getInstance(props);
    }

}
