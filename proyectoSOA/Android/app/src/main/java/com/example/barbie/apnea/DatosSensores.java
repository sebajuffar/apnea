package com.example.barbie.apnea;

import java.text.DecimalFormat;

public class DatosSensores {
    private float x;
    private float y;
    private float z;
    private float lum;
    DecimalFormat dosdecimales = new DecimalFormat("###.###");
    private static final float lumAceptables = 20;

    private double aceleracionAnterior = 0;
    private double acumulacionDiferencias = 0;
    private long millisUltimoShake = 0;
    private int countShakes = 0;

    public DatosSensores() {
        x = y = z = lum = 0;
    }

    synchronized public void setLuz(float lum) {

    }
    synchronized public void setAcelerometro(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    synchronized public float getLum() {
        return lum;
    }

    synchronized public float getX() {
        return x;
    }

    synchronized public float getY() {
        return y;
    }

    synchronized public float getZ() {
        return z;
    }

    synchronized public double getNorma() {
        double norma = Math.sqrt(x*x+y*y+z*z);
        return norma;
    }

    synchronized public String toString() {
        return dosdecimales.format(x) + ", " + dosdecimales.format(y) + ", " + dosdecimales.format(z) + " [" + getNorma() + "]";
    }

    public boolean luzAceptable() {
        if (lum > lumAceptables)
            return false;
        return true;
    }

    public boolean huboShake() {
        double aceleracion = getNorma();
        if (aceleracionAnterior == 0)
            aceleracionAnterior = aceleracion;
        else {
            acumulacionDiferencias = Math.abs(aceleracion - aceleracionAnterior);
            countShakes++;
            long ahora = System.currentTimeMillis();
            if (countShakes >= 10 && (millisUltimoShake+5000) < ahora && acumulacionDiferencias > 7) {
                millisUltimoShake = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }
}