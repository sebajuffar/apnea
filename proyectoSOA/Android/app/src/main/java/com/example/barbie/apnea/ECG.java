/*
 * Copyright 2016 AndroidPlot.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.example.barbie.apnea;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.*;

import java.lang.ref.*;
import java.util.Random;

/**
 * An example of a real-time plot displaying an asynchronously updated model of ECG data.  There are three
 * key items to pay attention to here:
 * 1 - The model data is updated independently of all other data via a background thread.  This is typical
 * of most signal inputs.
 *
 * 2 - The main render loop is controlled by a separate thread governed by an instance of {@link Redrawer}.
 * The alternative is to try synchronously invoking {@link Plot#redraw()} within whatever system is updating
 * the model, which would severely degrade performance.
 *
 * 3 - The plot is set to render using a background thread via config attr in  R.layout.ecg This ensures that the rest of the app will remain responsive during rendering.
 */
public class ECG extends Activity {
    private XYPlot plot;
    static Double temperatura;
    private TextView editTextTemp;
    private Thread t;

    /**
     * Uses a separate thread to modulate redraw frequency.
     */
    private Redrawer redrawer;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ecg);
        Inicio.getConexionBluetooth().pedirPulsoParaGraficar();
        Inicio.getConexionBluetooth().pedirTemperatura();
        editTextTemp = (TextView)findViewById(R.id.editTextTemp);
        // initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.plot);
        if(!Inicio.getConexionBluetooth().estaConectado())
            Toast.makeText(this, "La aplicacion no esta conectada al dispositivo. Por favor, conectelo", Toast.LENGTH_SHORT).show();
        ECGModel ecgSeries = new ECGModel(2000, 200);

        // add a new series' to the xyplot:
        MyFadeFormatter formatter =new MyFadeFormatter(2000);
        formatter.setLegendIconEnabled(false);
        plot.addSeries(ecgSeries, formatter);
        plot.setRangeBoundaries(0, 1000, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 2000, BoundaryMode.FIXED);

        // reduce the number of range labels
        plot.setLinesPerRangeLabel(3);

        // start generating ecg data in the background:
        ecgSeries.start(new WeakReference<>(plot.getRenderer(AdvancedLineAndPointRenderer.class)));

        // set a redraw rate of 30hz and start immediately:
        redrawer = new Redrawer(plot, 30, true);
        temperatura = 0D;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                while(true){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            editTextTemp.setText(round(temperatura,2).toString() + " °C");
                        }
                    });
                    Thread.sleep(1500);

                }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    /**
     * Special {@link AdvancedLineAndPointRenderer.Formatter} that draws a line
     * that fades over time.  Designed to be used in conjunction with a circular buffer model.
     */
    public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

        private int trailSize;

        public MyFadeFormatter(int trailSize) {
            this.trailSize = trailSize;
        }

        @Override
        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
            // offset from the latest index:
            int offset;
            if(thisIndex > latestIndex) {
                offset = latestIndex + (seriesSize - thisIndex);
            } else {
                offset =  latestIndex - thisIndex;
            }

            float scale = 255f / trailSize;
            int alpha = (int) (255 - (offset * scale));
            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
            return getLinePaint();
        }
    }

    /**
     * Primitive simulation of some kind of signal.  For this example,
     * we'll pretend its an ecg.  This class represents the data as a circular buffer;
     * data is added sequentially from left to right.  When the end of the buffer is reached,
     * i is reset back to 0 and simulated sampling continues.
     */
    public static class ECGModel implements XYSeries {

        private final Number[] data;
        private final long delayMs;
        private final int blipInteral;
        private final Thread thread;
        private boolean keepRunning;
        private int latestIndex;

        private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

        /**
         *
         * @param size Sample size contained within this model
         * @param updateFreqHz Frequency at which new samples are added to the model
         */
        public ECGModel(int size, int updateFreqHz) {
            data = new Number[size];
            for(int i = 0; i < data.length; i++) {
                data[i] = 0;
            }

            // translate hz into delay (ms):
            delayMs = 1000 / updateFreqHz;

            // add 7 "blips" into the signal:
            blipInteral = size / 7;

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latestIndex=0;
                        while (true) {
                            String pulso=Inicio.getConexionBluetooth().leePuslo();
                            if (latestIndex >= data.length) {
                                latestIndex = 0;
                            }
                            // generate some random data:
                            if (latestIndex % blipInteral == 0) {
                                // insert a "blip" to simulate a heartbeat:
                                int valorPulso=Integer.parseInt(pulso);

                                data[latestIndex] = Integer.parseInt(pulso);

                            } else {
                                // insert a random sample:
                                data[latestIndex] = Integer.parseInt(pulso);
                            }

                            if(latestIndex < data.length - 1) {

                                // null out the point immediately following i, to disable
                                // connecting i and i+1 with a line:
                                data[latestIndex +1] = null;
                            }

                            if(rendererRef.get() != null) {
                                rendererRef.get().setLatestIndex(latestIndex);
                                keepRunning = false;
                            }
                            latestIndex++;
                            Thread.sleep(delayMs);

                        }
                    } catch (Exception e) {
                        keepRunning = false;
                    }
                }
            });
        }


        public void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
            this.rendererRef = rendererRef;
            keepRunning = true;
            thread.start();
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            return data[index];
        }

        @Override
        public String getTitle() {
            return "Signal";
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        redrawer.finish();
    }

    //Para que la temperatura muestre 2 digitos decimales nada mas
    public Double round(Double value, int digits) {
        Double scale = Math.pow(10, digits);
        return Math.round(value * scale) / scale;
    }
}
