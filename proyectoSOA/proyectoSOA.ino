#include <OneWire.h>                 //Se importan las bibliotecas
#include <DallasTemperature.h>
//////////////////////////////////////////////////////////////////////////////////////////////////////
#include <SoftwareSerial.h>   // Incluimos la librería  SoftwareSerial  
SoftwareSerial BT(10,11);    // Definimos los pines RX y TX del Arduino conectados al Bluetooth
//////////////////////////////////////////////////////////////////////////////////////////////////////

// Definición de pines
int potenciometro = A0;
int buzzer = 8;
int ventilador = 7;
int led = 9;
int termometro = 2;
//TODO: ver donde lo ponemos
int sensorPulsasiones = A1; 
int LED13=13;

// Definicion de valores de sensores
int valorPotenciometro = 0;
int valorPotenciometroAnterior = 0;
int diferenciaValorPotenciometro;
int senialPulso;
double temperatura = 0;


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


OneWire ourWire(termometro);                //Se establece el pin declarado como bus para la comunicación OneWire
DallasTemperature sensors(&ourWire); //Se llama a la librería DallasTemperature


void setup() {
  delay(1000);
  pinMode(LED13,OUTPUT);
  pinMode(buzzer, OUTPUT);
  pinMode(ventilador, OUTPUT);
  pinMode(led, OUTPUT);
  tiempoAnterior = millis();
  tiempoAnteriorLectura = millis();
  tiempoPulsaciones=millis();
  //Serial.begin(9600);
  /////////////////////////////////////bluetooth////////////////////////////////////
   BT.begin(9600);       // Inicializamos el puerto serie BT (Para Modo AT 2)
  Serial.begin(9600);   // Inicializamos  el puerto serie 
  ////////////////////////////////////////////////////////////////////////////////////
  sensors.begin(); 
}

void loop() {
////////////////////////////////bluetooth/////////////////////////////////////////////
 if(BT.available())    // Si llega un dato por el puerto BT se envía al monitor serial
  {
    Serial.write(BT.read());
  }
  if(Serial.available())  // Si llega un dato por el monitor serial se envía al puerto BT
  {
     BT.write(Serial.read());
  }
/////////////////////////////////////////////////////////////////////////////////////

  
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
  leeTemperatura();
  
  BT.print("Tiempo: ");
  BT.print(millis());
  BT.print(", Pote: ");
  BT.print(valorPotenciometro);
  BT.print(", Pulso: ");
  BT.print(senialPulso);
  BT.print(", Temperatura: ");
  BT.println(temperatura); 
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

void leeTemperatura(){
  sensors.requestTemperatures();       //Prepara el sensor para la lectura
  temperatura = sensors.getTempCByIndex(0); //Se lee e imprime la temperatura en grados Centigrados
  }
