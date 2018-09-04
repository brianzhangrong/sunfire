package com.ihomefnt.sunfire.hbase.utils;

import static com.ihomefnt.sunfire.config.utils.StringUtils.searchIndexInMessage;

import com.ihomefnt.sunfire.hbase.model.Regular;
import java.util.List;
import org.springframework.util.CollectionUtils;

public class RuleUtils {

    public static boolean isFixedRule(List <Regular> regularList, String message) {
        boolean fixedRule = Boolean.FALSE;
        if (CollectionUtils.isEmpty(regularList)) {
            return Boolean.FALSE;
        }
        for (Regular regular : regularList) {
            int begin = searchIndexInMessage(message, regular.getBeginSplitSymbol(),
                    regular.getBeginPosition());
            int end = searchIndexInMessage(message, regular.getEndSplitSymbol(),
                    regular.getEndPosition());
            if (begin == -1 || end == -1 || begin > end) {
                continue;
            }
            //命中一组规则即可录入opentsdb，hbase
            fixedRule |= regular.getValue()
                    .equals(message.substring(begin + regular.getValue().length() - 1, end));
        }
        return fixedRule;
    }

}
