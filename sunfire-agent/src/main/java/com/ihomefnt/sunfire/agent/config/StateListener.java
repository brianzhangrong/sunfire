package com.ihomefnt.sunfire.agent.config;

public interface StateListener {

  int DISCONNECTED = 0;

  int CONNECTED = 1;

  int RECONNECTED = 2;

  void stateChanged(int connected);
}
