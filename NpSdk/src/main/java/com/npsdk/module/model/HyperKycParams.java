package com.npsdk.module.model;

import java.util.HashMap;
import java.util.Map;

public class HyperKycParams {
    private final String appId;
    private final String appKey;
    private final String workflowId;
    private final String transactionId;
    private final Map<String, String> customInputs = new HashMap<>();

    public HyperKycParams(Map<String, String> param) {
        if (param == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        // Lấy giá trị bắt buộc hoặc phát sinh lỗi nếu thiếu
        if (!param.containsKey("appId") || param.get("appId") == null ||
                !param.containsKey("appKey") || param.get("appKey") == null ||
                !param.containsKey("workflowId") || param.get("workflowId") == null ||
                !param.containsKey("transactionId") || param.get("transactionId") == null) {
            throw new IllegalArgumentException("Missing required parameters: appId, appKey, workflowId, or transactionId");
        }

        this.appId = (String) param.get("appId");
        this.appKey = (String) param.get("appKey");
        this.workflowId = (String) param.get("workflowId");
        this.transactionId = (String) param.get("transactionId");

        // Xử lý customInputs
        for (Map.Entry<String, String> entry : param.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!key.equals("appId") && !key.equals("appKey") &&
                    !key.equals("workflowId") && !key.equals("transactionId") &&
                    value != null) {
                customInputs.put(key, value);
            }
        }
    }

    public String getAppId() {
        return appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Map<String, String> getCustomInputs() {
        return customInputs;
    }
}