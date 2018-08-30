package com.ihomefnt.sunfire.hbase.utils;

import static com.ihomefnt.sunfire.config.utils.StringUtils.searchIndexInMessage;

import com.ihomefnt.sunfire.hbase.model.Regular;
import java.util.List;

public class RuleUtils {

    public static boolean isFixedRule(List <Regular> regularList, String message) {
        boolean fixedRule = Boolean.FALSE;
        for (Regular regular : regularList) {
            fixedRule &= regular.getValue().equals(message.substring(
                    searchIndexInMessage(message, regular.getBeginSplitSymbol(),
                            regular.getBeginPosition()),
                    searchIndexInMessage(message, regular.getEndSplitSymbol(),
                            regular.getEndPosition())));
        }
        return fixedRule;
    }

}
