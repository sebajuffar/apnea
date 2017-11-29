package com.example.barbie.apnea;


import android.os.StrictMode;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


import java.io.*;

public class Mail {
    private String correo;
    private String contraseña;
    private Session session;
    private File archivo;
    private static final String mensaje = "El paciente ha finalizado el ciclo de sueño, se adjunta el reporte correspondiente";

    public Mail(File reporte) {
        correo="usuario.sleepapnea@gmail.com";
        contraseña="SleepApnea";
        archivo = reporte;
    }

    public void mandarMail() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Properties properties = new Properties();
        // properties.setProperty("mail.transport.protocol", "smtp");
        //properties.setProperty("mail.host", "smtp.gmail.com");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.socketFactory.fallback", "false");
        properties.setProperty("mail.smtp.quitwait", "false");

        try{
            String msg = mensaje;
            session= Session.getDefaultInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(correo, contraseña);
                }
            });
            if (session != null){
                Message messaje = new MimeMessage(session);
                messaje.setFrom( new InternetAddress(correo));
                messaje.setSubject("Reporte Sleep-Apnea");
                messaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse ("medico.sleepapnea@gmail.com"));

                double latitud = Inicio.getDatosSensores().getLatitud();
                double altitud = Inicio.getDatosSensores().getAltitud();
                if ( latitud != 0 && altitud != 0 )
                    msg = msg + System.lineSeparator() + "Ubicación: " + latitud + "°, " + altitud + "°"+ System.lineSeparator();

                // Agrego el texto
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(msg);
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);

                // Agrego el reporte
                if (archivo != null) {
                    messageBodyPart = new MimeBodyPart();
                    String filename = archivo.getPath();
                    DataSource source = new FileDataSource(filename);
                    messageBodyPart.setDataHandler(new DataHandler(source));
                    messageBodyPart.setFileName(archivo.getName());
                    multipart.addBodyPart(messageBodyPart);
                }
                messaje.setContent(multipart);
                Transport.send(messaje);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
