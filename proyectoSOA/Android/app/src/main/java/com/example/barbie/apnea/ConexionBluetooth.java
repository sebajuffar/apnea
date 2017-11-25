package com.example.barbie.apnea;

import android.bluetooth.*;
import android.os.*;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class ConexionBluetooth implements Runnable {
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice dispositivoConectado;
    private BluetoothSocket btSocket;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private OutputStream outputStream;
    private InputStream inputStream;


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
        byte buffer[] = new byte[500];
        while(true)  {
            Log.d("ThreadBT","Arranco el thread");
            if ( inputStream != null ) {
                try {
                    Log.d("ThreadBT","Leyó algo.");
                    int cant;
                    cant = inputStream.read(buffer, 0, 500);
                    String recibido = new String(buffer, 0, cant);
                    String mensajes[] = recibido.split("\n");
                    for ( String mensaje : mensajes) {
                        Log.d("ThreadBT","Lei " + mensaje);
                        String info[] = mensaje.split(":");
                        switch (info[0]) {
                            case "CONECTADO":
                                Log.d("BT","Se conecto el apnea.");
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
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }


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
