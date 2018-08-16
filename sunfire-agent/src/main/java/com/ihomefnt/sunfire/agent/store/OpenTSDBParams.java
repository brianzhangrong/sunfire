package com.ihomefnt.sunfire.agent.store;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class OpenTSDBParams<T> {

    List <T> list = Lists.newArrayList();

    /*
   [{
    "metric": "sys.cpu.nice",
    "timestamp": 1499073763145,
    "value": 18,
    "tags": {
        "host": "web01",
        "dc": "lga"
    }
    }]
    * */
    @Data
    public static class OpenTSDBSummary {

        String metric;
        String timestamp;
        String value;
        Map <String, String> tags = Maps.newHashMap();
    }
}
