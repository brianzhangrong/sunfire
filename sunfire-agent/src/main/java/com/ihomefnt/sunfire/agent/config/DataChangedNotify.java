package com.ihomefnt.sunfire.agent.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.ihomefnt.sunfire.agent.config.WatchDataNotify.DataEvent;

public class DataChangedNotify {

    Multimap <DataEvent, WatchDataNotify> notifyList = ArrayListMultimap.create();

    public void addWatchDataNotify(DataEvent event, WatchDataNotify notify) {
        notifyList.put(event, notify);
    }

    public void notify(DataEvent event, String data) {

        notifyList.get(event).forEach(action -> action.dataNotify(data));
    }

}
