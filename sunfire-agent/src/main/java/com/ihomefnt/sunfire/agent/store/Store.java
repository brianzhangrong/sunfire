package com.ihomefnt.sunfire.agent.store;

public interface Store<T> {

    void store(String tableName,  String rowName,T t);

}
