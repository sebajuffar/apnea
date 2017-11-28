package com.example.barbie.apnea;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.*;

public class Reporte {
    private Context context;
    private long millisInicio = 0;
    private long tiempoInicio = 0;

    private List<Long> listaOffsetMillis;
    private Map<Long,Long> mapRespiracion;
    private Map<Long,Long> mapPulso;
    private Map<Long,Double> mapTemperatura;

    public Reporte() {
        context = Inicio.getContext();
        listaOffsetMillis = new ArrayList<>();
        mapPulso = new HashMap<>();
        mapRespiracion = new HashMap<>();
        mapTemperatura = new HashMap<>();
    }

    public void guardar() {
        GregorianCalendar gCalendar = new GregorianCalendar();
        gCalendar.setTimeInMillis(tiempoInicio);

        String fecha = String.format(new Locale("es","AR"),
                "%d-%02d-%02d",
                gCalendar.get(GregorianCalendar.YEAR),
                gCalendar.get(GregorianCalendar.MONTH) + 1,
                gCalendar.get(GregorianCalendar.DAY_OF_MONTH)
        );

        String filename = fecha + "-REPORTE.csv";
        FileOutputStream outputStream;
        File path = context.getFilesDir();
        try {
            //outputStream = context.openFileOutput(filename, Context.MODE_APPEND);
            //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            File archivo = new File(path, filename);
            FileOutputStream fileOutputStream = new FileOutputStream(archivo);
            for ( Long offset : listaOffsetMillis ) {
                Log.d("Escritura","Escribo linea");
                Double temperatura = mapTemperatura.get(offset);
                Long pulso = mapPulso.get(offset);
                Long respiracion = mapRespiracion.get(offset);
                StringBuilder linea = new StringBuilder();
                linea.append(millisAFechaYHora(offset));
                linea.append("\t");
                if ( temperatura != null ) linea.append(temperatura.toString());
                linea.append("\t");
                if ( pulso != null ) linea.append(pulso.toString());
                linea.append("\t");
                if ( respiracion !=null ) linea.append(respiracion.toString());
                linea.append(System.lineSeparator());
                //outputStreamWriter.write(linea.toString());
                fileOutputStream.write(linea.toString().getBytes());
            }
            //outputStreamWriter.close();
            //outputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Long getOffsetMillis(long millisArduino) {
        long resta = millisArduino - millisInicio;
        Long offset = new Long(resta);
        return offset;
    }

    private String millisAFechaYHora(Long tiempo) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(tiempo.longValue()+tiempoInicio);

        String fecha = String.format(new Locale("es","AR"),
                "%04d/%02d/%02d %02d:%02d:%02d",
                gc.get(GregorianCalendar.YEAR),
                gc.get(GregorianCalendar.MONTH) + 1,
                gc.get(GregorianCalendar.DAY_OF_MONTH),
                gc.get(GregorianCalendar.HOUR_OF_DAY),
                gc.get(GregorianCalendar.MINUTE),
                gc.get(GregorianCalendar.SECOND)
        );
        return fecha;
    }

    private void inicializarTiempo(long millis) {
        this.millisInicio = millis;
        this.tiempoInicio = System.currentTimeMillis();
    }

    public void cargarTemperatura(double temperatura, long millis) {
        if ( tiempoInicio == 0 || millisInicio == 0 )
            inicializarTiempo(millis);
        Long offset = getOffsetMillis(millis);
        if( !listaOffsetMillis.contains(offset) )
            listaOffsetMillis.add(offset);
        mapTemperatura.put(offset,Double.valueOf(temperatura));
    }

    public void cargarPulso(long pulso, long millis) {
        if ( tiempoInicio == 0 || millisInicio == 0 )
            inicializarTiempo(millis);
        Long offset = getOffsetMillis(millis);
        if( !listaOffsetMillis.contains(offset) )
            listaOffsetMillis.add(offset);
        mapPulso.put(offset,Long.valueOf(pulso));
    }

    public void cargarRespiracion(long respiracion, long millis) {
        if ( tiempoInicio == 0 || millisInicio == 0 )
            inicializarTiempo(millis);
        Long offset = getOffsetMillis(millis);
        if( !listaOffsetMillis.contains(offset) )
            listaOffsetMillis.add(offset);
        mapRespiracion.put(offset,Long.valueOf(respiracion));
    }

}
