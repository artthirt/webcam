//
// Created by 1 on 26.11.2019.
//

#ifndef WEBCAM_SENDASF_H
#define WEBCAM_SENDASF_H

#include <jni.h>
#include <vector>

#include "SenderSingleton.h"

class sendasf {
public:
    sendasf();
};

extern "C" JNIEXPORT jint JNICALL
Java_com_example_webcam_CameraService_senddata(JNIEnv* env, jclass obj, jobjectArray arr,
        jint width, jint height, jstring ip, jint port);

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_webcam_CameraService_senddata_1jpeg(JNIEnv *env, jclass clazz, jbyteArray data,
                                                     jstring ip, jint port);

#endif //WEBCAM_SENDASF_H
