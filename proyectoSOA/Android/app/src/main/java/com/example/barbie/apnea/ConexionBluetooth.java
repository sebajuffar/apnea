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

    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;


    public ConexionBluetooth() throws Exception {
        //Se inicializa el manejador del adaptador bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            throw new Exception("No se encontró Bluetooth");
        }
    }

    private void empezarComunicacion() {
    }

    private void pararComunicacion() {
    }


    @Override
    public void run() {
        Log.d("ThreadBT","Inicio");
        while (true) {
            Log.d("ThreadBT","Loopea");
            String linea = leeLinea();
            Log.d("ThreadBT","Leyó: " + linea);
        }
    }

    public String leeLinea(){
        String linea = new String("");
        try {
            if (inputStream != null && inputStream.available() > 0) {
                linea = bufferedReader.readLine();
            }
        } catch (IOException e) {
            return e.getMessage();
        }
        return linea;
    }



    public void desconectar()
    {
        try {
            enviarDatos(",");
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
        }
    }

    private void enviarDatos(String mensaje){
        if(outputStream != null) {
            try {
                outputStream.write(mensaje.getBytes());
            } catch (IOException e) {
                return;
            }
        }
    }

    public void dormir(){
        enviarDatos("d");
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
                Log.d("BT", "Se conecto el apnea.");
                break;
            case "DESCONECTADO":
                break;
            case "DORMIR":
                break;
            case "DESPERTAR":
                break;
            case "PULSO":
                break;
            case "TEMPERATURA":
                break;
            case "RESPIRACION":
                break;
            case "CALIBRANDO":
                break;
            case "ALARMA":
                break;
            case "EMERGENCIA":
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
        dispositivoConectado = device;
        try {
            btSocket = dispositivoConectado.createInsecureRfcommSocketToServiceRecord(myUUID);
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            btSocket.connect();
            inputStream = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();
            outputStream.write(".".getBytes());
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        lanzarThreads();
    }

    private void lanzarThreads() {
        Thread threadBluetooth = new Thread(this);
        threadBluetooth.start();
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

    public void send(String msj) throws IOException {
        outputStream.write(msj.toString().getBytes());
    }

    public String receive() throws IOException {
        String resultado = null;
        inputStream.read();
        return resultado;
    }

}
