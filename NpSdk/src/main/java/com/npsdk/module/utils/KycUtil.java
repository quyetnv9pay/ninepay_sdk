package com.npsdk.module.utils;

import androidx.activity.result.ActivityResultLauncher;
import com.npsdk.module.model.HyperKycParams;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import co.hyperverge.hyperkyc.data.models.HyperKycConfig;


public class KycUtil {
    public static void startWorkflow(
            ActivityResultLauncher<HyperKycConfig> hyperKycLauncher,
            HyperKycParams params
    ) {
        HyperKycConfig hyperKycConfig = new HyperKycConfig(
                params.getAppId(),
                params.getAppKey(),
                params.getWorkflowId(),
                params.getTransactionId()
        );
        hyperKycConfig.setInputs(params.getCustomInputs());
        hyperKycLauncher.launch(hyperKycConfig);
    }

    public static HyperKycParams createHyperKycParams(String jsonString) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);

                Map<String, String> paramMap = jsonToMap(jsonObject);

                return new HyperKycParams(paramMap);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to parse JSON: " + e.getMessage());
            }

        return null;
    }


     static Map<String, String> jsonToMap(JSONObject jsonObject) {
        Map<String, String> map = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.opt(key);

            if (value instanceof String) {
                map.put(key, (String) value);
            } else if (value != null) {
                map.put(key, value.toString());
            }
        }

        return map;
    }
}

