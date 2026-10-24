package com.oneplus.fastchg.toggle;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "/data/local/tmp/toggle_fast_charge.sh"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                Toast.makeText(getApplicationContext(), line.trim(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "执行错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
