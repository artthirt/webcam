//
// Created by 1 on 26.11.2019.
//

#ifndef WEBCAM_DATASTREAM_H
#define WEBCAM_DATASTREAM_H

#include <vector>

typedef std::vector<char> bytearray;

class DataStream {
public:
    DataStream();
    DataStream(bytearray *input);

    DataStream& operator << (uint8_t val);
    DataStream& operator << (uint16_t val);
    DataStream& operator << (uint32_t val);

    void writeRawData(const char* data, int len);

private:
    bytearray *m_input = nullptr;
};


#endif //WEBCAM_DATASTREAM_H
