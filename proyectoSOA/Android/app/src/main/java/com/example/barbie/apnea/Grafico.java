package com.example.barbie.apnea;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.androidplot.xy.XYPlot;
import java.util.Random;

public class Grafico extends AppCompatActivity {

    static Double temperatura;
    static Long []pulso;
    private TextView editTextTemp;
    private XYPlot plot;
    private Thread t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ecg);

       /* Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        */
        editTextTemp = (TextView)findViewById(R.id.editTextTemp);
        plot = (XYPlot)findViewById(R.id.plot);
        final Random r = new Random();
        t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //editTextTemp.setText(temperatura + " °C");

                                Double i1 = r.nextDouble() + 65;
                                editTextTemp.setText(round(i1,2).toString() + " °C");
                            }
                        });
                        Thread.sleep(1500);
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();
    }

    //Para que la temperatura muestre 2 digitos decimales nada mas
    public Double round(Double value, int digits) {
        Double scale = Math.pow(10, digits);
        return Math.round(value * scale) / scale;
    }
}
