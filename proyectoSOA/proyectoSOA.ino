// Definición de pines
int potenciometro = A0;
int buzzer = 8;
int ventilador = 7;
int led = 9;
//TODO: ver donde lo ponemos
int sensorPulsasiones = 6; 
int LED13=13;

// Definicion de valores de sensores
int valorPotenciometro = 0;
int valorPotenciometroAnterior = 0;
int diferenciaValorPotenciometro;
int senialPulso;


// Definición de estados
int huboMovimiento = 1;
int estadoAlarma = 0;
int sonarAlarma = 1;

// Definición de tiempos
long tiempoAnterior;
long tiempoAnteriorLectura;
long tiempoAnteriorAlarma;
long tiempoPulsaciones;

// Definición de constantes
int potenciometroEnRespiracion = 25;
int toleranciaPulso = 550;   
int tiempoLecturaPulso = 0.01;

void setup() {
  pinMode(LED13,OUTPUT);
  pinMode(buzzer, OUTPUT);
  pinMode(ventilador, OUTPUT);
  pinMode(led, OUTPUT);
  tiempoAnterior = millis();
  tiempoAnteriorLectura = millis();
  tiempoPulsaciones=millis();
  //Serial.begin(9600);
}

void loop() {
  valorPotenciometro = analogRead(potenciometro);
  diferenciaValorPotenciometro = abs(valorPotenciometro - valorPotenciometroAnterior);
  valorPotenciometroAnterior = valorPotenciometro;
  //Serial.println(diferenciaValorPotenciometro);
  //CADA MEDIO SEGUNDO CONTROLO SI EL POTENCIOMETRO SUFRE MOVIMIENTO
  if (lapsoTiempo(tiempoAnteriorAlarma, 0.5)) {
    //SE CONTEMPLA EL ERROR QUE HAY EN LA LECTURA DEL POTENCIOMETRO
    //AUN CUANDO ESTA QUIETO
    if (respira(diferenciaValorPotenciometro)) {
      //Serial.println("Respira");
      //RESPIRA, Y DESACTIVO LA ALARMA
      cambiarEstadoActuador(estadoAlarma, 0);
      huboMovimiento = 1;
      actualizaMarcaTiempo(tiempoAnterior);
    } else {
      //NO RESPIRA Y NO HUBO MOVIMIENTO
      //Serial.println("No respira");
      huboMovimiento = 0;
    }
    actualizaMarcaTiempo(tiempoAnteriorLectura);
  }

  //CONTROLO CADA 10 SEG SI HUBO MOVIMIENTO EN EL POTENCIOMETRO
  if (lapsoTiempo(tiempoAnterior, 10) && estadoAlarma == 0) {
    //Serial.println("Entro al if de tiempo");
    //SI LA PERSONA NO RESPIRA
    if (huboMovimiento == 0) {
      //ACTIVO ALARMA
      estadoAlarma = 1;
      actualizaMarcaTiempo(tiempoAnteriorAlarma);
      //Serial.println("asignar tiempoAnteriorAlarma1");
    } else {
      //DESACTIVO ALARMA
      estadoAlarma = 0;
    }
    actualizaMarcaTiempo(tiempoAnterior);
  }
  
  if (estadoAlarma) {
    activarActuadores();
  } else {
    desactivarActuadores();
  }
  leePulsaciones();
}


// Verifica la respiración comparando con una constante preestablecida
// @var valorActual es el valor actual del potenciómetro
boolean respira(int valorActual) {
  return valorActual > potenciometroEnRespiracion;
}

// Activa el ventilador, pone la alarma en intermitencia, enciende el led 
void activarActuadores() {
  //Serial.println("Alarma activa = 1");
  //PRENDO EL VENTILADOR
  digitalWrite(ventilador, HIGH);
  //Serial.println(abs(millis() - tiempoAnteriorAlarma));
 
  //EL FLAG "sonarAlarma" PASA DE 1 A -1 CADA 700 MILISEGUNDOS
  //PARA QUE EL PITIDO Y LA LUZ DEL LED SEA INTERMITENTE
  if (lapsoTiempo(tiempoAnteriorAlarma, 0.7)) {
    sonarAlarma = sonarAlarma * (-1);
    actualizaMarcaTiempo(tiempoAnteriorAlarma);
    //Serial.println("asignar tiempoAnteriorAlarma2");
  }
  
  // INTERMITENCIA
  if (sonarAlarma > 0) {
    tone(buzzer, 100);
    digitalWrite(led, HIGH);
    //Serial.println("Suena");
  } else {
    noTone(buzzer);
    digitalWrite(led, LOW);
    //Serial.println("No suena");
  }  
}

// Se desactivan el led, el ventilador y el buzzer
void desactivarActuadores() {
  //Serial.println("Alarma activa = 0");
  //SI LA ALARMA NO ESTA ACTIVA, APAGO EL BUZZER, EL LED
  //Y EL VENTILADOR
  digitalWrite(led, LOW);
  digitalWrite(ventilador, LOW);
  noTone(buzzer);  
}

// Actualiza una marca de tiempo
// @var tiempo es el tiempo a actualizar
void actualizaMarcaTiempo(long &tiempo) {
  tiempo = millis();
}

// Devuelve true cuando se llega al lapso de tiempo establecido para un tiempo específico
// @var tiempo la variable de tiempo a actualizar
// @var lapso se utiliza en segundos o fracción de segundos
boolean lapsoTiempo(long tiempo, float lapso) {
  return abs(tiempo - millis()) >= (lapso * 1000);
}

// Cambia el estado de un actuador para que entre o no en funcionamiento
// @var actuador el actuador a modificar
// @var estado el estado que se asignará
void cambiarEstadoActuador(int &actuador, int estado) {
  actuador = estado;
}


void leePulsaciones(){
  senialPulso = analogRead(sensorPulsasiones);
  Serial.println(senialPulso);                    
  if(lapsoTiempo(tiempoPulsaciones,tiempoLecturaPulso)) {
    if(senialPulso > toleranciaPulso){
      digitalWrite(LED13,HIGH);  
      tiempoPulsaciones=millis();
    } else {
      digitalWrite(LED13,LOW); 
      tiempoPulsaciones=millis();    
    }
  }
}



