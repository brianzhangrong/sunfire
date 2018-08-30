package com.ihomefnt.sunfire.admin.model;

import com.google.common.collect.Lists;
import com.ihomefnt.sunfire.hbase.model.Regular;
import java.util.List;
import lombok.Data;

@Data
public class UpdateRegularParams extends BaseParams {

    List <Regular> regularList = Lists.newArrayList();

}
