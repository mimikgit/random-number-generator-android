package com.mimik.randomnumbergen;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_get).setOnClickListener(this::onGetClicked);
    }

    private void onGetClicked(View view) {
        Random random = new Random(0);
        Toast.makeText(this, "Got " + random.nextInt(100), Toast.LENGTH_LONG).show();
    }
}