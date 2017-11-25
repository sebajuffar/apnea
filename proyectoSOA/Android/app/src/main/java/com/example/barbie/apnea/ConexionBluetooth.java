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
        /*
        Looper.prepare();
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:													// if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);					// create string from bytes array
                        sb.append(strIncom);												// append string
                        int endOfLineIndex = sb.indexOf("\r\n");							// determine the end-of-line
                        if (endOfLineIndex > 0) { 											// if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);				// extract string
                            sb.delete(0, sb.length());										// and clear
                            Log.d("Conexión Bluetooth", "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        }
                        break;
                }
            };
        };
        */
        byte[] buffer = new byte[256];  // buffer store for the stream
        int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        Log.d("Thread bt:","Loopea");

        while (true) {
                leeLinea();
        }
    }

    public String leeLinea(){
        String linea = new String("");
        try{
            if (inputStream != null && inputStream.available() > 0) {
                //bytes= inputStream.read(buffer,0,5);        // Get number of bytes and message in "buffer"
                linea= bufferedReader.readLine();
                Log.d("Treadbtlectura:",linea);
            }
            }
        catch (IOException e) {
            return e.getMessage();
        }
        return linea;
    }

    public void dormir(){
        try {
            outputStream.write("d".getBytes());
        }
        catch (IOException e) {
            return;
        }
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

    public void despertar()
    {
        enviarDatos("w");
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

    public void  pedirRespiracion(){
        enviarDatos("r");
    }
    public void  pedirPulso(){
        enviarDatos("p");
    }


    public boolean parsearMensajes() {
        byte[] buffer = new byte[256];
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
