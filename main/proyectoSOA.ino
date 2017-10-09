int potenciometro = A0;
int buzzer = 8;
int ventilador = 7;
int led = 9;
int valorPot = 0;
int valorPotAnt = 0;
int diferenciaValorPot;
int respira = 1;
int huboMovimiento = 1;
int alarmaActiva = 0;
long timeAnt;
long timeAntLectura;
long timeAntAlarma;
int sonarAlarma = 1;

void setup() {
  pinMode(buzzer, OUTPUT);
  pinMode(ventilador, OUTPUT);
  pinMode(led, OUTPUT);
  timeAnt = millis();
  timeAntLectura = millis();
  //Serial.begin(9600);
}

void loop() {
  valorPot = analogRead(potenciometro);

  diferenciaValorPot = abs(valorPot - valorPotAnt);


  valorPotAnt = valorPot;
  //Serial.println(diferenciaValorPot);
  //CADA MEDIO SEGUNDO CONTROLO SI EL POTENCIOMETRO SUFRE MOVIMIENTO
  if (abs(timeAntLectura - millis()) > 500) {
    
    //SE CONTEMPLA EL ERROR QUE HAY EN LA LECTURA DEL POTENCIOMETRO
    //AUN CUANDO ESTA QUIETO
    if (diferenciaValorPot > 25) {
      //Serial.println("Respira");
      //RESPIRA, Y DESACTIVO LA ALARMA
      alarmaActiva = 0;
      huboMovimiento = 1;
      timeAnt = millis();
    } else {
      //NO RESPIRA Y NO HUBO MOVIMIENTO
      //Serial.println("No respira");
      huboMovimiento = 0;
    }
    timeAntLectura = millis();
  }

  //CONTROLO CADA 10 SEG SI HUBO MOVIMIENTO EN EL POTENCIOMETRO
  
  if ((abs(millis() - timeAnt)) > 10000 && alarmaActiva == 0) {

    //Serial.println("Entro al if de tiempo");
    //SI LA PERSONA NO RESPIRA
    if (huboMovimiento == 0) {
      //ACTIVO ALARMA
      alarmaActiva = 1;
      timeAntAlarma = millis();
      //Serial.println("asignar timeAntAlarma1");
    } else {
      //DESACTIVO ALARMA
      alarmaActiva = 0;
    }
    timeAnt = millis();

  }
  
  //SI LA ALARMA ESTA ACTIVA
  if (alarmaActiva) {
    //Serial.println("Alarma activa = 1");
    //PRENDO EL VENTILADOR
    digitalWrite(ventilador, HIGH);
    //Serial.println(abs(millis() - timeAntAlarma));
    
    //EL FLAG "sonarAlarma" PASA DE 1 A -1 CADA 700 MILISEGUNDOS
    //PARA QUE EL PITIDO Y LA LUZ DEL LED SEA INTERMITENTE
    if (abs(millis() - timeAntAlarma) >= 700 ) {
      sonarAlarma = sonarAlarma * (-1);
      timeAntAlarma = millis();
      //Serial.println("asignar timeAntAlarma2");
    }
    
    //EN ESTE IF SE HACE EL EFECTO "INTERMITENTE"
    if (sonarAlarma > 0) {
      tone(buzzer, 100);
      digitalWrite(led, HIGH);
      //Serial.println("Suena");
    } else {
      noTone(buzzer);
      digitalWrite(led, LOW);
      //Serial.println("No suena");
    }
  } else {
    //Serial.println("Alarma activa = 0");
    //SI LA ALARMA NO ESTA ACTIVA, APAGO EL BUZZER, EL LED
    //Y EL VENTILADOR
    digitalWrite(led, LOW);
    digitalWrite(ventilador, LOW);
    noTone(buzzer);
  }
}


