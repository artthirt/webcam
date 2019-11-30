//
// Created by 1 on 29.11.2019.
//

#include "SenderSingleton.h"

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <errno.h>
#include <chrono>

//#include "jpegenc.h"
#include "GenASFPkts.h"


void Image::copy(int index, char *data, int len)
{
    if(index < 0 || index > 2)
        return;
    planes[index].resize(len);
    std::copy(data, data + len, planes[index].data());
}

void Image::encode()
{
//    jpegenc enc;
//    enc.encode(planes, width, height, data, 100);

    done = true;
}

Image::Image()
{

}

Image::Image(const Image &image)
{
    done = image.done;
    width = image.width;
    height = image.height;
    data = image.data;

    planes[0] = image.planes[0];
    planes[1] = image.planes[1];
    planes[2] = image.planes[2];
}

Image::~Image()
{
    if(thread.get()){
        thread->join();
    }
}

void Image::start()
{
    thread.reset(new std::thread(std::bind(&Image::encode, this)));
}

//////////////////////////////////////////

SenderSingleton SenderSingleton::m_instance;

SenderSingleton::SenderSingleton()
        : m_port(8000)
        , m_host(inet_addr("10.0.2.2"))
        , m_maxImages(5)
        , m_done(false)
{
    m_threadImages.reset(new std::thread(std::bind(&SenderSingleton::doAddImage, this)));
    m_threadSender.reset(new std::thread(std::bind(&SenderSingleton::doSendData, this)));
}

SenderSingleton::~SenderSingleton()
{
    m_done = true;

    if(m_threadImages.get()){
        m_threadImages->join();
    }
    if(m_threadSender.get()){
        m_threadSender->join();
    }
}

SenderSingleton &SenderSingleton::instance()
{
    return m_instance;
}

void SenderSingleton::send_to_ip(int sock, const char *data, int len)
{
    sockaddr_in local{};
    local.sin_family = AF_INET;
    local.sin_addr.s_addr = (m_host);
    local.sin_port = htons(m_port);

    int size = sendto(sock, data, len, 0, (sockaddr*)&local, sizeof(local));
    int err = errno;
}

void SenderSingleton::send_to_ip(const bytearray &data)
{
    int sock = socket(AF_INET, SOCK_DGRAM, 0);
    int err = errno;

    GenASFPkts asf;
    std::vector<bytearray> pkts = asf.getPkts(data.data(), data.size());

    for(const bytearray& ba: pkts){
        send_to_ip(sock, ba.data(), ba.size());
    }

    close(sock);
}

void SenderSingleton::doAddImage()
{
    while(!m_done){
        if(!m_packets.empty()){
            if(m_packets.front().done) {
                m_mutex.lock();
                Image im = m_packets.front();
                m_packets.pop();
                m_mutex.unlock();

                if(!im.data.empty() && m_senderPackets.size() < m_maxImages) {
                    m_mutexS.lock();
                    m_senderPackets.push(im.data);
                    m_mutexS.unlock();
                }
            }
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(2));
    }
}

void SenderSingleton::doSendData()
{
    while(!m_done){
        if(!m_senderPackets.empty()){
            m_mutexS.lock();
            bytearray data = m_senderPackets.front();
            m_senderPackets.pop();
            m_mutexS.unlock();

            send_to_ip(data);
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(2));
    }
}

void SenderSingleton::setNetworkConfig(const std::string &ip, u_short port)
{
    m_host = inet_addr(ip.c_str());
    m_port = port;
}

void SenderSingleton::addImage(const Image &image)
{
    if(m_packets.size() > m_maxImages || m_senderPackets.size() > m_maxImages)
        return;

    m_mutex.lock();
    m_packets.push(image);
    m_mutex.unlock();

    m_packets.back().start();
}

void SenderSingleton::addPkt(const char *data, int len)
{
    if(m_packets.size() > m_maxImages || m_senderPackets.size() > m_maxImages)
        return;

    bytearray d;
    d.resize(len);
    std::copy(data, data + len, d.data());

    m_mutexS.lock();
    m_senderPackets.push(d);
    m_mutexS.unlock();
}
