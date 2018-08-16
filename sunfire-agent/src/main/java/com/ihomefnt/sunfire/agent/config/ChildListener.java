package com.ihomefnt.sunfire.agent.config;

import java.util.List;

public interface ChildListener {
  void childChanged(String path, List<String> children);
}
