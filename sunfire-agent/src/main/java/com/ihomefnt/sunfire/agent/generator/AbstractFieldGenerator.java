package com.ihomefnt.sunfire.agent.generator;

import com.google.common.base.Preconditions;

public abstract class AbstractFieldGenerator implements  Generator {

    String field;

    public AbstractFieldGenerator(String field) {
        this.field=field;
    }

    @Override
    public String generate() {
        Preconditions.checkNotNull(field);
        return humpToUnderline(field);
    }

    public String humpToUnderline(String para){
        Preconditions.checkNotNull(para);
        StringBuilder sb=new StringBuilder(para);
        int temp=0;//定位
        for(int i=0;i<para.length();i++){
            if(Character.isUpperCase(para.charAt(i))){
                sb.insert(i+temp, "_");
                temp+=1;
            }
        }
        return sb.toString().toLowerCase();
    }
}
