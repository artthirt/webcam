//
// Created by 1 on 29.11.2019.
//

#ifndef WEBCAM_SENDERSINGLETON_H
#define WEBCAM_SENDERSINGLETON_H

#include <vector>
#include <queue>
#include <thread>
#include <mutex>

#include "DataStream.h"

struct Image{
    std::vector<char> planes[3];
    int width       = 0;
    int height      = 0;

    bool done       = false;
    bytearray data;

    Image();
    Image(const Image& image);
    ~Image();

    void copy(int index, char* data, int len);
    void encode();

    void start();

    std::unique_ptr<std::thread> thread;
};

class SenderSingleton {
public:
    SenderSingleton();
    ~SenderSingleton();

    void setNetworkConfig(const std::string &ip, u_short port);

    void addImage(const Image& image);

    void addPkt(const char* data, int len);

    void doAddImage();
    void doSendData();

    static SenderSingleton &instance();
private:
    std::queue<Image> m_packets;
    std::queue<bytearray> m_senderPackets;
    int m_maxImages;
    unsigned short m_port;
    unsigned int m_host;
    std::mutex m_mutex;
    std::mutex m_mutexS;
    bool m_done;
    int m_sock;

    std::unique_ptr<std::thread> m_threadImages;
    std::unique_ptr<std::thread> m_threadSender;

    void send_to_ip(int sock, const char *data, int len);
    void send_to_ip(const bytearray &data);

private:
    static SenderSingleton m_instance;
};


#endif //WEBCAM_SENDERSINGLETON_H
