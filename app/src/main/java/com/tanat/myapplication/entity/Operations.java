package com.tanat.myapplication.entity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Operations {
    private ArrayList<Operation> operations;

    public Operations() {
        this.operations = new ArrayList<>();
    }

    public ArrayList<Operation> getOperations() {
        return operations;
    }

    public void setOperations(ArrayList<Operation> operations) {
        this.operations = operations;
    }

    public Operations parseJson(String json) {
        Operations operations = new Operations();
        try {
            JSONArray allData = new JSONArray(json);
            for (int i = 0; i < allData.length(); i++) {
                Operation operation = new Operation();
                JSONObject jsonObject = new JSONObject(allData.getString(i));

                operation.setType(jsonObject.getString("type"));
                operation.setTime(jsonObject.getLong("time"));
                operation.setPrice(jsonObject.getDouble("price"));
                operation.setAmount(jsonObject.getDouble("amount"));

                operations.getOperations().add(operation);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return operations;
    }
}
