package com.ihomefnt.sunfire.agent.config;

@FunctionalInterface
public interface WatchDataNotify {

    void dataNotify(String data);

    static enum DataEvent {
        HBTABLE_CHANGED_EVENT, OSTB_CHANGED_EVENT
    }
}
