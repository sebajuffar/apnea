#include <OneWire.h>                 //Se importan las bibliotecas
#include <DallasTemperature.h>
//============================= BLUETOOTH =================================================
#define BTCONF 0              // Activa el envío de comandos AT por el Serial Monitor hacia el BT
//#include <SoftwareSerial.h>   // Incluimos la librería  SoftwareSerial  
//SoftwareSerial BT(10,11);    // Definimos los pines RX y TX del Arduino conectados al Bluetooth
#include <AltSoftSerial.h>      // Usamos la biblioteca AltSoftSerial porque la que viene por defecto da ciertos problemas al enviar y recibir "al mismo tiempo"
AltSoftSerial BT;             //AltSoftSerial usa 8 para RX y 9 para TX

// Comandos que llegan por BT
enum bt_msg {
  conectar = '.',
  desconectar = ',',
  dormir = 'd',
  despertarse = 'w',
  pedir_pulso = 'p',
  pedir_resp = 'r',
  pedir_temp = 't'
};

// Etiquetas para los mensajes que salen por BT
#define ACK_CONECTAR "CONECTADO"
#define ACK_DESCONECTAR "DESCONECTADO"
#define ACK_DORMIR "DORMIR"
#define ACK_DESPERTAR "DESPERTAR"
#define PULSO "PULSO"
#define TEMP "TEMPERATURA"
#define RESP "RESPIRACION"
#define CALIBRANDO "CALIBRANDO"
#define ALARMA "ALARMA"
#define EMERGENCIA "EMERGENCIA"


// Estados y opciones para BT
bool conectado;
bool durmiendo;
bool reportarPulso;
bool reportarRespiracion;
bool reportarTemperatura;


//============================= fin BLUETOOTH =================================================


// Definición de pines
#define potenciometro A0
#define buzzer 10 //era 8
#define ventilador 7
#define led 11 // era 9
#define termometro 2
#define sensorPulsasiones A1
#define LED13 13

// Definición de constantes
#define intervaloIntensidadBuzzer 5000
#define intervaloSegundosIntensidadBuzzer 2
#define potenciometroEnRespiracion 25
#define toleranciaPulso 550
#define tiempoLecturaPulso 0.01
#define toleranciaPotenciometro 100
#define cantidadMuestrasRespiracion 10
#define maxTiempoCalibracion 40
#define intervaloConfianza 0.05

//Constantes tiempo
#define day  86400000 // 86400000 milliseconds in a day
#define hour  3600000 // 3600000 milliseconds in an hour
#define  minute  60000 // 60000 milliseconds in a minute
#define  second   1000 // 1000 milliseconds in a second

// Definicion de valores de sensores
int valorPotenciometro = 0;
int valorPotenciometroAnterior = 0;
int valorPotenciometroAnteriorAuxiliarMedia;
int diferenciaValorPotenciometro;
int senialPulso;
double temperatura = 0;
long intensidadAlarma = 65535;

// Definición de estados
int huboMovimiento = 1;
int estadoAlarma = 0;
int sonarAlarma = 1;
int esperaBuzzerActiva = 0;
int creceDecrece;
int creceDecreceAnterior;

// Definición de tiempos
long tiempoAnterior;
long tiempoAnteriorLectura;
long tiempoAnteriorAlarma;
long tiempoPulsaciones;
long tiempoEsperaBuzzer;
long tiempoCambioIntensidad;
long tiempoCalibracion;
long marcaTiempoRespira;
long muestrasTiempoRespiracion[cantidadMuestrasRespiracion];
long mediaTiempoRespiracion;
long desvioTiempoRespiracion;
long intervaloInferiorMediaRespiracion;
long intervaloSuperiorMediaRespiracion;

//Definicion Flags
bool sensorCalibrado;

//Definición contadores
int contadorMuestrasRespiracion;

OneWire ourWire(termometro);                //Se establece el pin declarado como bus para la comunicación OneWire
DallasTemperature sensors(&ourWire); //Se llama a la librería DallasTemperature


void setup() {
  pinMode(LED13,OUTPUT);
  pinMode(buzzer, OUTPUT);
  pinMode(ventilador, OUTPUT);
  pinMode(led, OUTPUT);
  tiempoAnterior = millis();
  tiempoAnteriorLectura = millis();
  tiempoPulsaciones=millis();
  BT.begin(9600);       // Inicializamos el puerto serie BT (Para Modo AT 2)
  Serial.begin(115200);   // Inicializamos  el puerto serie 
  sensors.begin();      // Necesario para el sensor de temperatura
}



void loop() {
//==================================== Bluetooth =====================================
 if(BT.available())    
  {
    char msg = BT.read();   // Lee de a un byte
    
    switch(msg) {
      case conectar:
        if ( !conectado ) {
          Serial.println("Conectado.");
          BT.println(ACK_CONECTAR);
          
          conectado = true;
        }
        break;
      
      case desconectar:
        if ( conectado ) {
          Serial.println("Desconectado.");
          BT.println(ACK_DESCONECTAR);
          
          conectado = false;
          reportarPulso = false;
          reportarRespiracion = false;
          reportarTemperatura = false;
        }
        break;

      case dormir:
        if ( conectado && !durmiendo ) {          // Si se confirmó la conexión y todavía no estaba durmiendo, empieza la fase de sueño e inicializa la calibración. Si no, ignora.
          durmiendo = true;
          Serial.println("Comenzar fase de sueño.");
          BT.println(ACK_DORMIR);
          
          inicializaValoresCalibracion(); 
        }
        break;

       case despertarse:
        if ( conectado && durmiendo ) {         // Si se confirmó la conexión y estaba durmiendo, termina la fase de sueño. Si no, ignora.
          durmiendo = false;
          reportarRespiracion = false;
          sensorCalibrado = false;
          desactivarActuadores();
          Serial.println("Finalizar fase de sueño.");
          BT.println(ACK_DESPERTAR);
          
          
        }
        break;
        
      case pedir_pulso:
        if ( conectado && !reportarPulso )
          reportarPulso = true;
        break;
      case pedir_resp:
      if ( conectado && !reportarRespiracion )
          reportarRespiracion = true;
        break;
      case pedir_temp:
      if ( conectado && !reportarTemperatura )
          reportarTemperatura = true;
        break;
      default:
        Serial.print("Comando desconocido: ");
        Serial.println(msg);
        break;
    }
    
    
  }
  
  #if BTCONFIG == 1
    if(Serial.available())  // Si llega un dato por el monitor serial se envía al puerto BT
    {
       BT.write(Serial.read());
    }
  #endif
  unsigned long timestamp = millis();
  if ( conectado ) {
    if ( reportarPulso ) {
      BT.print(PULSO);                // Etiqueta del mensaje
      BT.print(":");                  // Separador
      BT.print(senialPulso);          // Valor de la señal
      BT.print(":");                  // Separador
      BT.println(timestamp);          // Timestamp + fin de linea
      
    }
    
    if ( reportarRespiracion ) {
      if ( sensorCalibrado ) {
        BT.print(RESP);                 // Etiqueta del mensaje
        BT.print(":");                  // Separador
        BT.print(valorPotenciometro);   // Valor de la señal
        BT.print(":");                  // Separador
        BT.println(timestamp);          // Timestamp + fin de linea  
        
      } else if ( durmiendo ) {                            // Sensor todavia no calibrado
        BT.print(CALIBRANDO);           // Etiqueta del mensaje
        BT.print(":");                  // Separador
        BT.println(timestamp);          // Timestamp + fin de linea 
      } else {
        reportarRespiracion = false;
      }
      
      
    }
    
    if ( reportarTemperatura ) {
        BT.print(TEMP);                 // Etiqueta del mensaje
        BT.print(":");                  // Separador
        BT.print(temperatura);          // Valor de la señal
        BT.print(":");                  // Separador
        BT.println(timestamp);          // Timestamp + fin de linea
        
    }
  }
  if ( durmiendo )    // Una vez activado el sueño no hace falta que siga conectado, por eso no pregunta por la conexion
    duerme();
  
}

void duerme(){
  if(!sensorCalibrado){ 
    calibraSensorRespiracion();
    
  }
  else{

    controlaSuenio();
  }
}


void inicializaValoresCalibracion(){
  inicializoVectorInhalaExhala();
  sensorCalibrado=false;
  tiempoCalibracion=0;
  creceDecreceAnterior=0;
  contadorMuestrasRespiracion=0;
  mediaTiempoRespiracion=0;
  actualizaMarcaTiempo(&tiempoCalibracion);
  valorPotenciometroAnteriorAuxiliarMedia=0;
  desvioTiempoRespiracion=0;
  Serial.println("Calibrando Sensor Respiración");
}

//calibra el sensor de respiracion con la media de tiempo entre de inhalar y exhalar se toman x muestras en y minutos. 
void calibraSensorRespiracion(){
  if(!sensorCalibrado){ 
    if(lapsoTiempo(tiempoCalibracion, maxTiempoCalibracion)){
        calculaDatosEstadisticos();
    }
    else{
        leerPotenciometro();
        int diferenciaPotenciometro=valorPotenciometro-valorPotenciometroAnteriorAuxiliarMedia;
        valorPotenciometroAnteriorAuxiliarMedia=valorPotenciometro;
        //Guarda 1 o -1, si crece o decrece   
        creceDecrece=diferenciaPotenciometro!=0?diferenciaPotenciometro/abs(diferenciaPotenciometro):creceDecrece;
        if(creceDecrece!=creceDecreceAnterior){
            Serial.println("Cambio");
            Serial.println(valorPotenciometro);
            Serial.println(diferenciaPotenciometro);
            creceDecreceAnterior=creceDecrece;
            muestrasTiempoRespiracion[contadorMuestrasRespiracion]=millis();
            contadorMuestrasRespiracion++;
            //Serial.println("Contador:");
            //Serial.println(contadorMuestrasRespiracion);
            if(contadorMuestrasRespiracion>=cantidadMuestrasRespiracion)
            {
                calculaDatosEstadisticos();
            }
        }
    } 
  }
}

void calculaDatosEstadisticos()
{
  sensorCalibrado=true;
  calculaMediaTiempoRespiracion();
  calcularDesvioTiempoRespiracion();
  calcularParametrosConfianza();  
  delay(10000);         
}


void calculaMediaTiempoRespiracion(){
  for(int i=0;i<contadorMuestrasRespiracion-1;i++){
    long tiempo1=muestrasTiempoRespiracion[i];
    long tiempo2=muestrasTiempoRespiracion[i+1];
    long diferencia=tiempo2-tiempo1;
    mediaTiempoRespiracion+=diferencia;
  }    
   
    mediaTiempoRespiracion=mediaTiempoRespiracion/contadorMuestrasRespiracion;
    Serial.println("La media de respiración es:");
    int segundos= (((mediaTiempoRespiracion % day) % hour) % minute) / second;
    Serial.println(segundos);
}

void calcularDesvioTiempoRespiracion(){
  long acum=0;
  long diferencia;
  for(int i=0;i<contadorMuestrasRespiracion;i++){
    long tiempo1=muestrasTiempoRespiracion[i];
    diferencia=tiempo1-mediaTiempoRespiracion;
    diferencia=diferencia*diferencia;
    acum+=diferencia;
  }
  
  acum=acum/(contadorMuestrasRespiracion-1);
  acum=sqrt(acum);
  desvioTiempoRespiracion=abs(acum);
  Serial.println("El desvío de respiración es:");
  int segundos= (((desvioTiempoRespiracion % day) % hour) % minute) / second;
  Serial.println(segundos);
}

void calcularParametrosConfianza(){
  float aux=intervaloConfianza/2;
  //z-1
  //TODO ver como sacar de la tabla normal. Existe alguna libreria??
  float zM1 =1.96;
  intervaloInferiorMediaRespiracion=mediaTiempoRespiracion-(zM1*desvioTiempoRespiracion/sqrt(contadorMuestrasRespiracion));
  intervaloSuperiorMediaRespiracion=mediaTiempoRespiracion+(zM1*desvioTiempoRespiracion/sqrt(contadorMuestrasRespiracion));
  Serial.println("Intervalo");
  int segundos= (((intervaloInferiorMediaRespiracion % day) % hour) % minute) / second;
  Serial.println(segundos);
  segundos= (((intervaloSuperiorMediaRespiracion % day) % hour) % minute) / second;
  Serial.println(segundos);
}

void inicializoVectorInhalaExhala(){
  for(int i=0;i<cantidadMuestrasRespiracion;i++)
  {
    muestrasTiempoRespiracion[i]=0;  
  }
}
//controla el sueño determina si activar los actuadores que despiertan 
void controlaSuenio(){
   //leerPotenciometro();
  
  //Serial.println(diferenciaValorPotenciometro);
  //CADA MEDIO SEGUNDO CONTROLO SI EL POTENCIOMETRO SUFRE MOVIMIENTO
  if (lapsoTiempo(tiempoAnteriorAlarma, 0.5)) {
    //SE CONTEMPLA EL ERROR QUE HAY EN LA LECTURA DEL POTENCIOMETRO
    //AUN CUANDO ESTA QUIETO
    //Serial.print("Diferencia potenciometro: ");
    //Serial.println(diferenciaValorPotenciometro);
    if (respira(diferenciaValorPotenciometro)) {
      //Serial.println("Respira");
      //RESPIRA, Y DESACTIVO LA ALARMA
      cambiarEstadoActuador(&estadoAlarma, 0);
      huboMovimiento = 1;
      actualizaMarcaTiempo(&tiempoAnterior);
    } else {
      //NO RESPIRA Y NO HUBO MOVIMIENTO
      Serial.println("No respira");
      huboMovimiento = 0;
    }
   
    actualizaMarcaTiempo(&tiempoAnteriorLectura);
  }

  //CONTROLO CADA 10 SEG SI HUBO MOVIMIENTO EN EL POTENCIOMETRO
  if (lapsoTiempo(tiempoAnterior, 1) && estadoAlarma == 0) {
    //Serial.println("Entro al if de tiempo");
    //SI LA PERSONA NO RESPIRA
    if (huboMovimiento == 0) {
      //ACTIVO ALARMA
      estadoAlarma = 1;
      actualizaMarcaTiempo(&tiempoAnteriorAlarma);
      //Serial.println("asignar tiempoAnteriorAlarma1");
    } else {
      //DESACTIVO ALARMA
      estadoAlarma = 0;
    }
    actualizaMarcaTiempo(&tiempoAnterior);
  }
  
  if (estadoAlarma) {
    activarActuadores();
  } else {
    desactivarActuadores();
  }
  leePulsaciones();
  leeTemperatura();

  /* NO VA EN LA VERSION FINAL
  BT.print("Tiempo: ");
  BT.print(millis());
  BT.print(", Pote: ");
  BT.print(valorPotenciometro);
  BT.print(", Pulso: ");
  BT.print(senialPulso);
  BT.print(", Temperatura: ");
  BT.println(temperatura);
  */
}

// Verifica la respiración comparando con los parametros obtenidos en la calibración
//Se verifica que el tiempo entre Inhalar y Exhalar este dentro del intervalo de confianza calculado para la media muestral
// @var valorActual es el valor actual del potenciómetro
boolean respira(int valorActual) {
  leerPotenciometro();
  int diferenciaPotenciometro=valorPotenciometro-valorPotenciometroAnteriorAuxiliarMedia;
  valorPotenciometroAnteriorAuxiliarMedia=valorPotenciometro;
  //Guarda 1 o -1, si crece o decrece   
  creceDecrece=diferenciaPotenciometro!=0?diferenciaPotenciometro/abs(diferenciaPotenciometro):creceDecrece;
  Serial.println("Crece");
  Serial.println(creceDecrece);
  if(creceDecrece!=creceDecreceAnterior){
     creceDecreceAnterior=creceDecrece;
     actualizaMarcaTiempo(&marcaTiempoRespira);
  }
  

  long diferenciaTiempo=millis()-marcaTiempoRespira;
  Serial.println("Diferencia de tiempo");
  Serial.println(diferenciaTiempo);
  return diferenciaTiempo >= intervaloInferiorMediaRespiracion && diferenciaTiempo <= intervaloSuperiorMediaRespiracion;
  //TODO: evaluar bien que pasa si tarda menos en respirar de lo que indica la cota inferior del intervalo de confianza.
  //Indicaría que se esta agitando. Sería ideal ir recalculando este tiempo. Se necesitaria otro arduino que haga los calculos.
  //return  diferenciaTiempo <= intervaloSuperiorMediaRespiracion;
}

// Activa el ventilador, pone la alarma en intermitencia, enciende el led 
void activarActuadores() {
  //Serial.println("Alarma activa = 1");
  //PRENDO EL VENTILADOR
  digitalWrite(ventilador, HIGH);
  activarEsperaBuzzer();
  
  //Serial.println(abs(millis() - tiempoAnteriorAlarma));
 
  //EL FLAG "sonarAlarma" PASA DE 1 A -1 CADA 700 MILISEGUNDOS
  //PARA QUE EL PITIDO Y LA LUZ DEL LED SEA INTERMITENTE
  if (lapsoTiempo(tiempoAnteriorAlarma, 0.7)) {
    sonarAlarma = sonarAlarma * (-1);
    actualizaMarcaTiempo(&tiempoAnteriorAlarma);
    //Serial.println("asignar tiempoAnteriorAlarma2");
  }
  
  // INTERMITENCIA
  if (lapsoTiempo(tiempoEsperaBuzzer,3)) {
    if (sonarAlarma > 0) {
      tone(buzzer, obtenerIntensidadAlarma());
      digitalWrite(led, HIGH);
      //Serial.println("Suena");
    } else {
      noTone(buzzer);
      digitalWrite(led, LOW);
      //Serial.println("No suena");
    }  
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
  esperaBuzzerActiva = 0;
  intensidadAlarma = 65535;
}

// Actualiza una marca de tiempo
// @var tiempo es el tiempo a actualizar
void actualizaMarcaTiempo(long *tiempo) {
  *tiempo = millis();
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
void cambiarEstadoActuador(int *actuador, int estado) {
  *actuador = estado;
}


void leePulsaciones(){
  senialPulso = analogRead(sensorPulsasiones);
  //Serial.println(senialPulso);                    
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

// Lee el valor del potenciometro y normaliza la señal sobre un valor experimental
void leerPotenciometro() {
  valorPotenciometro = analogRead(potenciometro);
  //Serial.print("Valor Potenciometro: ");
  //Serial.println(valorPotenciometro);
  //Serial.print("Valor Potenciometro Anterior: ");
  //Serial.println(valorPotenciometroAnterior);
  if (abs(valorPotenciometro - valorPotenciometroAnterior) < toleranciaPotenciometro) {
    valorPotenciometro = valorPotenciometroAnterior;
    diferenciaValorPotenciometro = 0;
  } else {
    diferenciaValorPotenciometro = abs(valorPotenciometro - valorPotenciometroAnterior);
    valorPotenciometroAnterior = valorPotenciometro;
  }

}

// Activa la espera del buzzer para que comience a sonar después del tiempo definido
void activarEsperaBuzzer() {
  if (esperaBuzzerActiva == 0) {
    actualizaMarcaTiempo(&tiempoEsperaBuzzer);
    esperaBuzzerActiva = 1;
  }
}

// Incrementa la intensidad de la alarma y devuelve el valor actual
int obtenerIntensidadAlarma() {
  Serial.print("Intensidad Actual: ");
  Serial.println(intensidadAlarma);
  if (intensidadAlarma > intervaloIntensidadBuzzer && lapsoTiempo(tiempoCambioIntensidad, intervaloSegundosIntensidadBuzzer)) {
    intensidadAlarma = intensidadAlarma - intervaloIntensidadBuzzer;
    actualizaMarcaTiempo(&tiempoCambioIntensidad);
    Serial.print("Disminuye la intensidad");
    Serial.println(intensidadAlarma);
  }
  return intensidadAlarma;
}

