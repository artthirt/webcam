//
// Created by 1 on 26.11.2019.
//

#include "sendasf.h"

#include <string>

#include "SenderSingleton.h"


extern "C" JNIEXPORT jint JNICALL
Java_com_example_webcam_CameraService_senddata(JNIEnv* env, jclass obj, jobjectArray arr,
                                               jint width, jint height, jstring ip, jint port)
{
    sendasf asf;

    jboolean  copy = 0;
    //jint len = env->GetArrayLength(arr);
    jobject  plane1 = env->GetObjectArrayElement(arr, 0);
    jobject  plane2 = env->GetObjectArrayElement(arr, 1);
    jobject  plane3 = env->GetObjectArrayElement(arr, 2);
    jbyteArray* jdata1 = reinterpret_cast<jbyteArray*>(&plane1);
    jbyte* jplane1 = env->GetByteArrayElements(*jdata1, nullptr);
    jint jlen1 = env->GetArrayLength(*jdata1);
    jbyteArray* jdata2 = reinterpret_cast<jbyteArray*>(&plane2);
    jbyte* jplane2 = env->GetByteArrayElements(*jdata2, nullptr);
    jint jlen2 = env->GetArrayLength(*jdata2);
    jbyteArray* jdata3 = reinterpret_cast<jbyteArray*>(&plane3);
    jbyte* jplane3 = env->GetByteArrayElements(*jdata3, nullptr);
    jint jlen3 = env->GetArrayLength(*jdata3);

    const char* sip = env->GetStringUTFChars(ip, &copy);

    Image image;
    image.copy(0, (char*)jplane1, jlen1);
    image.copy(1, (char*)jplane2, jlen2);
    image.copy(2, (char*)jplane3, jlen3);
    image.width = width;
    image.height = height;

    SenderSingleton::instance().setNetworkConfig(sip, port);
    SenderSingleton::instance().addImage(image);

    env->ReleaseStringUTFChars(ip, sip);

    return 1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_webcam_CameraService_senddata_1jpeg(JNIEnv *env, jclass clazz, jbyteArray data,
                                                     jstring ip, jint port)
{
    jboolean  copy = 0;
    //jint len = env->GetArrayLength(arr);
    jbyte* jplane1 = env->GetByteArrayElements(data, nullptr);
    jint jlen1 = env->GetArrayLength(data);

    const char* sip = env->GetStringUTFChars(ip, &copy);

    SenderSingleton::instance().setNetworkConfig(sip, port);
    SenderSingleton::instance().addPkt((char*)jplane1, jlen1);

    env->ReleaseStringUTFChars(ip, sip);

    return 1;
}

//////////////////////////////////////////////

sendasf::sendasf()
{

}


/////////////////////////
