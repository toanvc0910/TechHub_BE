package com.techhub.app.commonservice.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParameterInfo {
    private String columnName;
    private Object value;
    private String logicalOperator;
    private String relationOperator;
    private String Status;

    public static JSONObject toJson(ParameterInfo parameter) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", parameter.getColumnName());
        if(parameter.getValue() instanceof String[] || parameter.getValue() instanceof Integer[]) {
            obj.put("value", new JSONArray(parameter.getValue()));
        }else{
            obj.put("value", parameter.getValue());
        }

        obj.put("logical", parameter.getLogicalOperator());
        obj.put("rel", parameter.getRelationOperator());
        obj.put("sta", parameter.getStatus());
        return obj;
    }
}