package com.example.barbie.apnea;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.PowerManager;
import android.support.annotation.MainThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Inicio extends AppCompatActivity implements SensorEventListener, LocationListener {

    public static Boolean appEncendida;
    private Button btn_comenzar;
    private Button btn_detener;
    private Button btn_reportes;
    private static Reporte reporteActual;
    private static volatile ConexionBluetooth conexionBluetooth;
    private static Thread threadBluetooth;
    private static PowerManager powerManager;
    private static Context context;
    private boolean botonDormir;
    private SensorManager mSensorManager;
    private static DatosSensores datosSensores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.appEncendida = false;
        this.powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        setContentView(R.layout.activity_inicio);
        btn_comenzar = (Button)findViewById(R.id.btn_comenzar);
        btn_detener = (Button)findViewById(R.id.btn_detener);
        btn_reportes = (Button)findViewById(R.id.btn_reportes);
        botonDormir = true;
        btn_comenzar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(conexionBluetooth.estaConectado())
                onClickComenzar(view);
            }
        });


        btn_detener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickDetener(view);
            }
        });

        /*
        btn_reportes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickReportes(view);
            }
        });
        */

        context = getApplicationContext();

        configurarThread();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        datosSensores = new DatosSensores();

    }

    @Override
    protected void onDestroy() {
        if (conexionBluetooth != null && conexionBluetooth.estaConectado())
            conexionBluetooth.pedirDesconexion();
        super.onDestroy();
    }

    private void configurarThread() {
        if ( conexionBluetooth == null ) {
            try {
                conexionBluetooth = new ConexionBluetooth();
            } catch (Exception e) {
                Toast.makeText(this,e.getMessage(), Toast.LENGTH_LONG).show();
                if (threadBluetooth != null && !threadBluetooth.isInterrupted()) {
                    threadBluetooth.interrupt();
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_opciones, menu);
       return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        escucharSensores();
        if(conexionBluetooth.estaConectado()) {
            Toast.makeText(this,"Conectado a: " + conexionBluetooth.nombreDispositivo(), Toast.LENGTH_SHORT ).show();
            setTitle("Sleep APNEA [" + conexionBluetooth.nombreDispositivo() + "]");
        } else {
            setTitle("Sleep APNEA");
        }
    }

    @Override
    protected void onPause() {
        pararSensores();
        super.onPause();
    }

    //Se hizo click en alguna de las opciones del menu... dependiendo de cual, se va a proceder
    //con su respectiva accion...
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id==R.id.menuItemSync) {
            Intent intent = new Intent(this, DeviceList.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
        }
        if (id==R.id.menuItemEcg) {
            Intent intent = new Intent(this, ECG.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
        }
        /*if (id==R.id.opcion2) {
            Toast.makeText(this,"Se seleccionó la segunda opción",Toast.LENGTH_LONG).show();
        }
        if (id==R.id.opcion3) {
            Toast.makeText(this,"Se seleccionó la tercer opción", Toast.LENGTH_LONG).show();
        }*/
        return super.onOptionsItemSelected(item);
    }

    @Override
    //Al apretar un boton...
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //y si es el boton "atras" estando en la activity principal...
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if (keyCode == KeyEvent.KEYCODE_BACK) {

                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Salir")
                        .setMessage("¿Está seguro que desea salir?")
                        .setNegativeButton(android.R.string.cancel, null)// sin listener - no hace nada
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {// un listener que al pulsar, cierre la aplicacion
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Salir
                                Inicio.this.finish();
                            }
                        })
                        .show();


                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    //Acciones de los botones principales
    public void onClickComenzar(View view) {
        if ( botonDormir ) {
            botonDormir = false;
            btn_comenzar.setText("DESPERTAR");
            if (!datosSensores.luzAceptable())
                Toast.makeText(this, "Se recomiendan valores de luz más bajos", Toast.LENGTH_LONG).show();
            conexionBluetooth.dormir();
        } else {
            botonDormir = true;
            btn_comenzar.setText("COMENZAR SUEÑO");
            conexionBluetooth.despertar();
        }
    }

    public void onClickDetener(View view){
        conexionBluetooth.pedirDesconexion();
    }

    /*
    public void onClickReportes(View view){
        if(appEncendida == false){
            Toast.makeText(this, "Encienda la aplicacion", Toast.LENGTH_SHORT).show();
        }
        conexionBluetooth.pedirRespiracion();
        conexionBluetooth.pedirPulso();
    }
    */

    public static ConexionBluetooth getConexionBluetooth() {
        return conexionBluetooth;
    }

    public static Context getContext() {
        return context;
    }
    public static PowerManager getPowerManager() {
        return powerManager;
    }

    public static void setReporteActual(Reporte reporte) {
        reporteActual = reporte;
    }

    public static Reporte getReporteActual() {
        return reporteActual;
    }

    private void escucharSensores() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),   SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),           SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void pararSensores() {
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT));
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {
            switch (sensorEvent.sensor.getType() ) {
                case Sensor.TYPE_ACCELEROMETER:
                    datosSensores.setAcelerometro(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                    if (conexionBluetooth.estaConectado() && datosSensores.huboShake()) {
                         Log.d("Sensores","Prendo Ventilador");
                        conexionBluetooth.switchVentilador();
                    }
                    break;
                case Sensor.TYPE_LIGHT:
                    datosSensores.setLuz(sensorEvent.values[0]);
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        return;
    }

    @Override
    public void onLocationChanged(Location location) {
        datosSensores.setLatitud(location.getLatitude());
        datosSensores.setLatitud(location.getAltitude());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public static DatosSensores getDatosSensores() {
        return datosSensores;
    }
}
