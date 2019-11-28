//
// Created by 1 on 26.11.2019.
//

#ifndef WEBCAM_SENDASF_H
#define WEBCAM_SENDASF_H

#include <jni.h>

class sendasf {
public:
    sendasf();

    void send_to_ip(int sock, const char *data, int len);
    void send_to_ip(const char* data, int len, const char* ip, unsigned short port);

private:
    unsigned short m_port;
    unsigned int m_host;

};

extern "C" JNIEXPORT jint JNICALL
Java_com_example_webcam_CameraService_senddata(JNIEnv* env, jclass obj, jbyteArray arr, jstring ip, jint port);

#endif //WEBCAM_SENDASF_H
