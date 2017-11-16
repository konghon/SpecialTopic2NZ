#include "Wire.h"
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h"
#include "math.h"
#include "NewPing.h"
#include "SoftwareSerial.h"
#include "PololuQik.h"
#include <AutoPID.h>
/*
  Digital Pin 5 - TX
  Digital Pin 6 - RX
  Digital Pin 7 - RESET
*/
PololuQik2s12v10 qik(5, 6, 7);

#define TRIGGER_PIN A0
#define ECHO_PIN A1
#define MAX_DISTANCE 75

#define Kp  8
#define Kd  0.1
#define Ki  3
#define sampleTime  0.005
#define targetAngle -2

MPU6050 mpu;
NewPing sonar(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);

/****************************
**  Junk needed for DMP******
*****************************/
int FifoAlive = 0; // test if the interrupt is triggering
int IsAlive = -20;     // counts interrupt start at -20 to get 20+ good values before assuming connected
// MPU control/status vars
uint8_t mpuIntStatus;   // holds actual interrupt status byte from MPU
uint8_t devStatus;      // return status after each device operation (0 = success, !0 = error)
uint16_t packetSize;    // expected DMP packet size (default is 42 bytes)
uint16_t fifoCount;     // count of all bytes currently in FIFO
uint8_t fifoBuffer[64]; // FIFO storage buffer

Quaternion q;           // [w, x, y, z]         quaternion container
float Yaw, Pitch, Roll; // in degrees
float ypr[3];           // [yaw, pitch, roll]   yaw/pitch/roll container and gravity vector
VectorFloat gravity;    // [x, y, z]            gravity vector
/**********************************************************************************************************/

//for other PID
double motorPower2, targetAngle2, currentAngle2;

/*********************************************************************
** Offset needed for the MPU6050                                ******
** RUN  IMU_ZERO and note down the values                       ******
**********************************************************************/
int MPUOffsets[6] = {  -3493,  195,   1431,    173,    38,    45}; //MPU6050 on balanceing bot

/*********************************************************************
**  PID                                                         ******
**                                                              ******
**********************************************************************/
int16_t accY, accZ, gyroX;
volatile int motorPower, gyroRate;
volatile float accAngle, gyroAngle, currentAngle, prevAngle = 0, error, prevError = 0, errorSum = 0;
volatile byte count = 0;
/******************************************************************************************************/

//PingSensor 
int distanceCm;

/*********************************************************************
**  other PID test                                              ******
**                                                              ******
**********************************************************************/
AutoPID myPID(&currentAngle2, &targetAngle2, &motorPower2,-255,255, Kp, Ki, Kd);


/*********************************************************************
**  Enabeling DMP interrupt                                     ******
*  when the dmp has new info ready interrupt will be triggered  ******
**********************************************************************/
volatile bool mpuInterrupt = false;     // indicates whether MPU interrupt pin has gone high
void dmpDataReady() {
  mpuInterrupt = true;
}

/*********************************************************************
**  Getting usefull data from the MPU                           ******
**                                                              ******
**********************************************************************/
void MPUMath() {
  mpu.dmpGetQuaternion(&q, fifoBuffer);
  mpu.dmpGetGravity(&gravity, &q);
  mpu.dmpGetYawPitchRoll(ypr, &q, &gravity);
  Yaw = (ypr[0] * 180 / M_PI);
  Pitch = (ypr[1] * 180 / M_PI);
  Roll = (ypr[2] * 180 / M_PI);
  //for other PID
  currentAngle2= Pitch;
}
/********************************************************************/


void i2cSetup() {
  // join I2C bus (I2Cdev library doesn't do this automatically)
#if I2CDEV_IMPLEMENTATION == I2CDEV_ARDUINO_WIRE
  Wire.begin();
  TWBR = 24; // 400kHz I2C clock (200kHz if CPU is 8MHz)
#elif I2CDEV_IMPLEMENTATION == I2CDEV_BUILTIN_FASTWIRE
  Fastwire::setup(400, true);
#endif
}

/*********************************************************************
**  connect to the mpu6050                                      ******
**                                                              ******
**********************************************************************/
void MPU6050Connect() {
  static int MPUInitCntr = 0;
  // initialize device
  mpu.initialize(); // same
  // load and configure the DMP
  devStatus = mpu.dmpInitialize();// same

  if (devStatus != 0) {
    // ERROR!
    // 1 = initial memory load failed
    // 2 = DMP configuration updates failed
    // (if it's going to break, usually the code will be 1)

    char * StatStr[5] { "No Error", "initial memory load failed", "DMP configuration updates failed", "3", "4"};

    MPUInitCntr++;

    Serial.print(F("MPU connection Try #"));
    Serial.println(MPUInitCntr);
    Serial.print(F("DMP Initialization failed (code "));
    Serial.print(StatStr[devStatus]);
    Serial.println(F(")"));

    if (MPUInitCntr >= 10) return; //only try 10 times
    delay(1000);
    MPU6050Connect(); // Lets try again
    return;
  }

  mpu.setXAccelOffset(MPUOffsets[0]);
  mpu.setYAccelOffset(MPUOffsets[1]);
  mpu.setZAccelOffset(MPUOffsets[2]);
  mpu.setXGyroOffset(MPUOffsets[3]);
  mpu.setYGyroOffset(MPUOffsets[4]);
  mpu.setZGyroOffset(MPUOffsets[5]);

  Serial.println(F("Enabling DMP..."));
  mpu.setDMPEnabled(true);
  // enable Arduino interrupt detection

  Serial.println(F("Enabling interrupt detection (Arduino external interrupt pin 2 on the Uno)..."));
  attachInterrupt(0, dmpDataReady, FALLING); //pin 2 on the Uno

  mpuIntStatus = mpu.getIntStatus(); // Same
  // get expected DMP packet size for later comparison
  packetSize = mpu.dmpGetFIFOPacketSize();
  delay(1000); // Let it Stabalize
  mpu.resetFIFO(); // Clear fifo buffer
  mpu.getIntStatus();
  mpuInterrupt = false; // wait for next interrupt
}
/*************************************************************************************/

void setup() {
  Serial.begin(9600);
  //init motordriver
  qik.init();
  //i2c init
  i2cSetup();
  // initialize the MPU6050
  MPU6050Connect();
  // Set TimeStep test pid
  //myPID.setTimeStep(5);
}

void loop() {
  // read acceleration and gyroscope values
  if (mpuInterrupt ) { // wait for MPU interrupt or extra packet(s) available
    GetDMP(); // Gets the MPU Data and canculates angles
  }
  // set motor power after constraining it
  //motorPower2 = constrain(motorPower2, -255, 255);
  motorPower = constrain(motorPower, -255, 255);
  
  //setMotors(motorPower2, motorPower2);
  setMotors(motorPower, motorPower);
  
  // measure distance every 100 milliseconds
  if ((count % 20) == 0) {
    distanceCm = sonar.ping_cm();
  }
  if ((distanceCm < 20) && (distanceCm != 0)) {
    //setMotors(-motorPower2, motorPower2);
    setMotors(-motorPower, motorPower);
  }
}


/*********************************************************************
**  Set the motors using the Pololu motor driver                ******
**                                                              ******
**********************************************************************/
void setMotors(int leftMotorSpeed, int rightMotorSpeed) {
  qik.setM0Speed(leftMotorSpeed);
  qik.setM1Speed(rightMotorSpeed);
}
/*********************************************************************/

/*********************************************************************
**  PID Controller                                              ******
**                                                              ******
**********************************************************************/
void PIDController() {
  //test Pid//
  //myPID.run(); 
  
  currentAngle = Pitch;

  error = currentAngle - targetAngle;
  errorSum = errorSum + error;
  errorSum = constrain(errorSum, -300, 300);
  //calculate output from P, I and D values
  motorPower = Kp * (error) + Ki * (errorSum) * sampleTime - Kd * (currentAngle - prevAngle) / sampleTime;
  prevAngle = currentAngle;
}
/******************************************************************************/


/*********************************************************************
**  Get data out of the DMP   (Digital Motion Processor)         ******
**  Data has been fused   based on code by Jeff Rowberg          ******
**********************************************************************/
void GetDMP() {
  mpuInterrupt = false;
  FifoAlive = 1;
  fifoCount = mpu.getFIFOCount();

  uint16_t MaxPackets = 20;
  if ((fifoCount % packetSize) || (fifoCount > (packetSize * MaxPackets)) || (fifoCount < packetSize)) { // we have failed Reset and wait till next time!
    Serial.println(F("Reset FIFO"));
    if (fifoCount % packetSize) Serial.print(F("\t Packet corruption")); // fifoCount / packetSize returns a remainder... Not good! This should never happen if all is well.
    Serial.print(F("\tfifoCount ")); Serial.print(fifoCount);
    Serial.print(F("\tpacketSize ")); Serial.print(packetSize);

    mpuIntStatus = mpu.getIntStatus(); // reads MPU6050_RA_INT_STATUS       0x3A
    Serial.print(F("\tMPU Int Status ")); Serial.print(mpuIntStatus , BIN);
    // MPU6050_RA_INT_STATUS       0x3A
    //
    // Bit7, Bit6, Bit5, Bit4          , Bit3       , Bit2, Bit1, Bit0
    // ----, ----, ----, FIFO_OFLOW_INT, I2C_MST_INT, ----, ----, DATA_RDY_INT

    /*
      Bit4 FIFO_OFLOW_INT: This bit automatically sets to 1 when a FIFO buffer overflow interrupt has been generated.
      Bit3 I2C_MST_INT: This bit automatically sets to 1 when an I2C Master interrupt has been generated. For a list of I2C Master interrupts, please refer to Register 54.
      Bit1 DATA_RDY_INT This bit automatically sets to 1 when a Data Ready interrupt is generated.
    */
    if (mpuIntStatus & B10000) { //FIFO_OFLOW_INT
      Serial.print(F("\tFIFO buffer overflow interrupt "));
    }
    if (mpuIntStatus & B1000) { //I2C_MST_INT
      Serial.print(F("\tSlave I2c Device Status Int "));
    }
    if (mpuIntStatus & B1) { //DATA_RDY_INT
      Serial.print(F("\tData Ready interrupt "));
    }
    Serial.println();
    //I2C_MST_STATUS
    //PASS_THROUGH, I2C_SLV4_DONE,I2C_LOST_ARB,I2C_SLV4_NACK,I2C_SLV3_NACK,I2C_SLV2_NACK,I2C_SLV1_NACK,I2C_SLV0_NACK,
    mpu.resetFIFO();// clear the buffer and start over
    mpu.getIntStatus(); // make sure status is cleared we will read it again.
  } else {
    while (fifoCount  >= packetSize) { // Get the packets until we have the latest!
      if (fifoCount < packetSize) break; // Something is left over and we don't want it!!!
      mpu.getFIFOBytes(fifoBuffer, packetSize); // lets do the magic and get the data
      fifoCount -= packetSize;
    }
    MPUMath(); // <<<<<<<<<<<<<<<<<<<<<<<<<<<< On success MPUMath() <<<<<<<<<<<<<<<<<<<
    PIDController(); //calling the pid
    if (fifoCount > 0) mpu.resetFIFO(); // clean up any leftovers Should never happen! but lets start fresh if we need to. this should never happen.
  }
}
/***********************************************************************************************************************************************************/
