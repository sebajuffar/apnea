package com.example.barbie.apnea;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FoundDeviceList extends AppCompatActivity {

    private ListView listViewDisp;
    private TextView text_no_items;
    private TextView text_titulo;
    private ArrayList<BluetoothDevice> arrayListDisp;
    private Map<String, BluetoothDevice> mapNombreADevice = new HashMap<>();

    private volatile ConexionBluetooth conexionBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_device_list);
        //Para añadir el boton con la flecha para ir a la activity anterior
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Dispositivos encontrados");


        conexionBluetooth = Inicio.getConexionBluetooth();

        listViewDisp = (ListView)findViewById(R.id.listView);
        text_no_items = (TextView)findViewById(R.id.text_no_disp);
        text_titulo = (TextView)findViewById(R.id.text_title);

        Intent intent = getIntent();

        arrayListDisp = intent.getParcelableArrayListExtra("dispositivosEncontrados");

        ArrayList list = new ArrayList();

        if(arrayListDisp.isEmpty()){
            noHayDispositivos();
        } else {
            for (BluetoothDevice bt : arrayListDisp) {
                //if(bt.getName().contains("Philips"))
                String identificador = bt.getName() + "\n" + bt.getAddress();
                list.add(identificador); //Obtenemos los nombres y direcciones MAC de los disp. vinculados
                mapNombreADevice.put(identificador, bt);
            }

            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
            listViewDisp.setAdapter(adapter);
            listViewDisp.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    BluetoothDevice item = mapNombreADevice.get(adapterView.getItemAtPosition(i));
                    conexionBluetooth.vincular(item);
                    conexionBluetooth.conectarDispositivo(item);
                    // Volver a la pantalla de inicio
                    Intent intent = new Intent(FoundDeviceList.this,Inicio.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    FoundDeviceList.this.startActivity(intent);
                    finish();
                }
            });

            hayDispositivos();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // todo: goto back activity from here
                Intent intent = new Intent(FoundDeviceList.this, DeviceList.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void hayDispositivos(){
        text_no_items.setVisibility(View.INVISIBLE);
        listViewDisp.setVisibility(View.VISIBLE);
        text_titulo.setVisibility(View.VISIBLE);
    }

    public void noHayDispositivos(){
        text_no_items.setVisibility(View.VISIBLE);
        listViewDisp.setVisibility(View.INVISIBLE);
        text_titulo.setVisibility(View.INVISIBLE);
    }

}
