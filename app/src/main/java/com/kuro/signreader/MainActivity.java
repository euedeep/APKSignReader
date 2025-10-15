package com.kuro.signreader;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class MainActivity extends Activity {
    EditText appPkg;
    Button btnGet, btnSave;
    TextView resultBase64, resultCpp;
    private static final int CREATE_FILE_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        appPkg = findViewById(R.id.appPkg);
        resultBase64 = findViewById(R.id.resultBase64);
        resultCpp = findViewById(R.id.resultCpp);

        btnGet = findViewById(R.id.btnGetSign);
        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PackageManager packageManager = getPackageManager();
                    PackageInfo packageInfo = packageManager.getPackageInfo(appPkg.getText().toString(), PackageManager.GET_SIGNATURES);

                    Signature[] signatures = packageInfo.signatures;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeByte(signatures.length);

                    StringBuilder sb = new StringBuilder();
                    sb.append("std::vector<std::vector<uint8_t>> apk_signatures {");
                    for (Signature value : signatures) {
                        sb.append("{");
                        dos.writeInt(value.toByteArray().length);
                        dos.write(value.toByteArray());
                        for (int j = 0; j < value.toByteArray().length; j++) {
                            sb.append(String.format("0x%02X", value.toByteArray()[j]));
                            if (j != value.toByteArray().length - 1) {
                                sb.append(",");
                            }
                        }
                        sb.append("}");
                    }
                    sb.append("};");

                    dos.close();
                    baos.close();

                    resultBase64.setText("Base64: " + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT));
                    resultCpp.setText("C++: " + sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveWithFilePicker();
            }
        });
    }

    private void saveWithFilePicker() {
        String appName = appPkg.getText().toString().trim();
        if (appName.isEmpty()) {
            Toast.makeText(this, "Package name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String fileName = appName + "_signatures.txt";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                saveFileToUri(uri);
            }
        }
    }

    private void saveFileToUri(Uri uri) {
        try {
            String content = resultBase64.getText().toString() + "\n" + 
                            resultCpp.getText().toString();
            java.io.OutputStream os = getContentResolver().openOutputStream(uri);
            if (os != null) {
                os.write(content.getBytes());
                os.close();
                Toast.makeText(this, "✅ Saved successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
