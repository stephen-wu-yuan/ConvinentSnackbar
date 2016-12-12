package com.stephen.convinentsnackbar;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button fab = (Button) findViewById(R.id.btn_id);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConvinentSnackbar.make(view, "This is top Convinent Snackbar", ConvinentSnackbar.LENGTH_SHORT)
                       .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark)).setConvinentbarGravity(Gravity.TOP).show();
            }
        });

        Button bbtn = (Button)findViewById(R.id.btn_id2);
        bbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ConvinentSnackbar.make(view, "This is bottom Convinent Snackbar", ConvinentSnackbar.LENGTH_SHORT)
                        .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark)).show();
            }
        });
    }

}
