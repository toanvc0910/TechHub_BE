package com.techhub.app.commonservice.sql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class Param {
    public final static String START = "START";
    public final static String END = "END";
    public final static String NONE = "NONE";

    public static String[] getBetweenParam(String value1,String value2) {
        if(value1 == null && value2 == null) return null;
//        return List.of(String.valueOf(value1),String.valueOf(value2));
        return new String[]{value1,value2};
    }

    public abstract static class Relational {
        public final static String AND = "AND";
        public final static String OR = "OR";
        public final static String NONE = "NONE";
    }

    public abstract static class Logical {
        public final static String LIKE = "like";
        public final static String EQUAL = "=";
        public final static String NOT_LIKE = "not like";
        public final static String NOT_EQUAL = "!=";
        public final static String GREATER_THAN = ">";
        public final static String LESS_THAN = "<";
        public final static String GREATER_THAN_EQ = ">=";
        public final static String LESS_THAN_EQ = "<=";
        public final static String ORDER_BY = "order by";
        public final static String LIMIT = "limit";
        public final static String OFFSET = "offset";
        public final static String BETWEEN = "between";
        public final static String IN = "in";
        public final static String NOT_IN = "not in";
        public final static String IS_NULL = "is null";
        public final static String IS_NOT_NULL = "is not null";

    }
}
