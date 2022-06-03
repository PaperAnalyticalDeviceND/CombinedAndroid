# CombinedAndroid

Combinging AndroidPADCaptureBasic and AndroidPADCLassify into a single app, and updating to newer SDK and OpenCV versions.

# Configuring OpenCV and other dependencies

**Configuring OpenCV:**
Copy the contents of the OpenCV/sdk folder into the OpenCV454 folder at the same level as the app folder
 add in the top level build.gradle:  file classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10"
 add in the app level build.gradle:  file  implementation project(path: ':OpenCV454')
 add in the settings.gradle file:  include ':OpenCV454'
 def opencvsdk='<path_to_your_OpenCV454_folder>'
 project(':OpenCV454').projectDir = new File(opencvsdk + '/sdk')
 make the minSdk = 21 in app level build.gradle
 sync gradle to effect changes
EX:

def opencvsdk='/Users/crc/AndroidStudioProjects/CombinedAndroid/OpenCV454'
include ':app'
include ':OpenCV454'
project(':OpenCV454').projectDir = new File(opencvsdk + '/sdk')

**Dependency Conflicts:**

An extra listenablefuture module is pulled by the Jetifier, so it must be excluded
from any dependencies besides guava.
EX:
def work_version = "2.5.0"
    implementation "androidx.work:work-runtime:$work_version", {
        exclude group: 'com.google.guava' , module: 'listenablefuture'
    }

# Data uploads format

New data elements that go beyond the available database fields are stored in JSON format in the "notes" field.   
The format is as below:   

{"Phone ID":"50766914127f64ba",  
"Quantity NN":100,  
"User":"Matt",  
"Neural net":"fhi360_small_lite",  
"App type":"Android",  
"Safe":"Suspected safe",  
"Prediction score":0.99,  
"PLS used":true,  
"Predicted drug":"Albendazole",  
"Build":14,  
"Quantity PLS":8,  
"Notes version":0,  
"Notes":""}

