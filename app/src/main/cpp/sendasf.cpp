//
// Created by 1 on 26.11.2019.
//

#include "sendasf.h"

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <errno.h>

#include <string>

#include "GenASFPkts.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_example_webcam_CameraService_senddata(JNIEnv* env, jclass obj, jbyteArray arr, jstring ip, jint port)
{
    sendasf asf;

    jboolean  copy = 0;
    jint len = env->GetArrayLength(arr);
    jbyte* data = env->GetByteArrayElements(arr, &copy);

    const char* sip = env->GetStringUTFChars(ip, &copy);

    asf.send_to_ip((char*)data, len, sip, port);

    env->ReleaseStringUTFChars(ip, sip);

    return len;
}

//////////////////////////////////////////////

sendasf::sendasf()
: m_port(8000)
, m_host(inet_addr("10.0.3.2"))
{

}

void sendasf::send_to_ip(int sock, const char *data, int len)
{
    sockaddr_in local{};
    local.sin_family = AF_INET;
    local.sin_addr.s_addr = (m_host);
    local.sin_port = htons(m_port);

    int size = sendto(sock, data, len, 0, (sockaddr*)&local, sizeof(local));
    int err = errno;
}


void sendasf::send_to_ip(const char *data, int len, const char* ip, unsigned short port)
{
    int sock = socket(AF_INET, SOCK_DGRAM, 0);
    int err = errno;

    m_host = inet_addr(ip);
    m_port = port;

    GenASFPkts asf;
    std::vector<bytearray> pkts = asf.getPkts(data, len);

    for(const bytearray& ba: pkts){
        send_to_ip(sock, ba.data(), ba.size());
    }

    close(sock);
}
