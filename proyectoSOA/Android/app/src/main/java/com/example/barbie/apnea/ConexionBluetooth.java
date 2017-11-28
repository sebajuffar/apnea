package com.example.barbie.apnea;

import android.bluetooth.*;
import android.os.*;
import android.util.Log;



import java.io.BufferedReader;

import java.io.IOException;
import java.io.*;
import java.util.*;

public class ConexionBluetooth implements Runnable {
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice dispositivoConectado;
    private BluetoothSocket btSocket;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private OutputStream outputStream;
    private InputStream inputStream;
    Handler h;
    final int RECIEVE_MESSAGE = 1;
    private StringBuilder sb = new StringBuilder();
    private static Thread hilo = null;
    private boolean flagConectado = false;
    private Reporte reporte;

    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;


    public ConexionBluetooth() throws Exception {
        //Se inicializa el manejador del adaptador bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            throw new Exception("No se encontró Bluetooth");
        }
    }

    @Override
    public void run() {
        Log.d("ThreadBT","Inicio");
        while (true) {
            Log.d("ThreadBT","Loopea");
            String linea = leeLinea();
            if (Thread.currentThread().isInterrupted())
                return;
            Log.d("ThreadBT","Leyó: " + linea);
            parsearMensaje(linea);
        }
    }

    public String leeLinea(){
        String linea = new String("");
        try {
            if ( inputStream != null ) {
                linea = bufferedReader.readLine();
            }
        } catch (IOException e) {
            desconectar();
            return e.getMessage();
        }
        return linea;
    }


    private void desconectar() {
        try {
            if ( estaConectado() ) {
                inputStream.close();
                outputStream.close();
                btSocket.close();
            }
        } catch (Exception e) {
        } finally {
            inputStream = null;
            outputStream = null;
            btSocket = null;
            dispositivoConectado = null;
            flagConectado = false;
            finalizarThread();
        }
    }

    private void finalizarThread() {
        if (hilo != null) {
            hilo.interrupt();
            hilo = null;
        }
    }

    private void enviarDatos(String mensaje) {
        if(outputStream != null) {
            try {
                outputStream.write(mensaje.getBytes());
            } catch (IOException e) {
                return;
            }
        }
    }

    public void pedirDesconexion() { enviarDatos(","); }
    public void dormir(){
        enviarDatos("d");
        pedirPulso();
        pedirRespiracion();
        pedirTemperatura();
    }
    public void despertar()
    {
        enviarDatos("w");
    }
    public void pedirRespiracion(){
        enviarDatos("r");
    }
    public void pedirPulso(){
        enviarDatos("p");
    }
    public void pedirTemperatura(){
        enviarDatos("t");
    }
    public void switchVentilador() { enviarDatos("v"); }


    public boolean parsearMensaje(String linea) {
        String []args = linea.split(":");
        Log.d("ThreadBT", "Lei " + args[0]);
        switch (args[0]) {
            case "CONECTADO":
                flagConectado = true;
                Log.d("BT", "Se conecto el apnea.");
                break;
            case "DESCONECTADO":
                if ( flagConectado )        //else era basura
                    desconectar();
                Log.d("BT", "Se desconecto el apnea.");
                break;
            case "DORMIR":
                if ( flagConectado ){
                    // TODO: Deberia apagar la pantalla / ir a otro activity
                    reporte = new Reporte();
                    Inicio.setReporteActual(reporte);
                }
                break;
            case "DESPERTAR":
                if ( flagConectado ){
                    reporte.guardar();
                }
                break;
            case "PULSO":
                if ( flagConectado && reporte != null ){
                    reporte.cargarPulso(Long.parseLong(args[1]), Long.parseLong(args[2]));
                }
                break;
            case "TEMPERATURA":
                if ( flagConectado && reporte != null ){
                    reporte.cargarTemperatura(Double.parseDouble(args[1]), Long.parseLong(args[2]));
                }
                break;
            case "RESPIRACION":
                if ( flagConectado && reporte != null ){
                    reporte.cargarRespiracion(Long.parseLong(args[1]), Long.parseLong(args[2]));
                }
                break;
            case "CALIBRANDO":
                if ( flagConectado && reporte != null ){
                    reporte.cargarRespiracion(0L, Long.parseLong(args[1]));
                }
                break;
            case "ALARMA":
                if ( flagConectado ){
                    reporte.cargarAlarma(Long.parseLong(args[1]));
                }
                break;
            case "EMERGENCIA":
                if ( flagConectado ){
                    reporte.cargarEmergencia(Long.parseLong(args[1]));
                }
                break;
            default:
                Log.d("ThreadBT", "Mensaje desconocido: "+ args[0]);
            }
        return true;
    }



    public Set<BluetoothDevice> getDispositivosVinculados() {
        if ( mBluetoothAdapter == null ) {
            return new TreeSet<BluetoothDevice>();
        }
        return  mBluetoothAdapter.getBondedDevices();
    }

    private void postToastMessage(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public boolean desactivarBT() {
        if ( tieneBluetooth() )
            return true;
        return mBluetoothAdapter.disable();
    }


    public void empezarBusqueda() {
        if ( tieneBluetooth() )
            mBluetoothAdapter.startDiscovery();
    }

    public boolean estaActivadoElBT () {
        return mBluetoothAdapter.isEnabled();
    }

    public boolean tieneBluetooth() {
        if (mBluetoothAdapter == null)
            return false;
        return true;
    }

    public boolean vincular(BluetoothDevice device) {
        return device.createBond();
    }

    public void conectarDispositivo(BluetoothDevice device) {
        pedirDesconexion();
        flagConectado = false;
        dispositivoConectado = device;
        try {
            btSocket = dispositivoConectado.createInsecureRfcommSocketToServiceRecord(myUUID);
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            btSocket.connect();
            inputStream = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            lanzarThreads();
            enviarDatos(".");
        } catch (IOException e) {
            desconectar();
            return;
        }
    }

    private void lanzarThreads() {
        hilo = new Thread(this);
        hilo.start();
    }

    public boolean estaConectado() {

        if ( dispositivoConectado != null && (dispositivoConectado.getBondState() == BluetoothDevice.BOND_BONDED || dispositivoConectado.getBondState() == BluetoothDevice.BOND_BONDING))
            return true;
        return false;
    }

    public String nombreDispositivo() {
        if (estaConectado()) {
            return dispositivoConectado.getName();
    }
        return "";
    }


    public String macDispositivo() {
        if (estaConectado()) {
            return dispositivoConectado.getAddress();
        }
        return "";
    }

}
