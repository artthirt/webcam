//
// Created by 1 on 26.11.2019.
//

#include "DataStream.h"

#include <vector>

using namespace std;

DataStream::DataStream() {

}

DataStream::DataStream(vector<char> *input)
: m_input(input)
{
    if(m_input){
        m_input->reserve(65536);
    }
}

DataStream &DataStream::operator<<(uint8_t val) {
    if(m_input){
        m_input->push_back(val);
    }
    return *this;
}

DataStream &DataStream::operator<<(uint16_t val) {
    if(m_input){
        char* d = (char*)&val;
        m_input->push_back(d[0]);
        m_input->push_back(d[1]);
    }
    return *this;
}

DataStream &DataStream::operator<<(uint32_t val) {
    if(m_input){
        char* d = (char*)&val;
        m_input->push_back(d[0]);
        m_input->push_back(d[1]);
        m_input->push_back(d[2]);
        m_input->push_back(d[3]);
    }
    return *this;
}

void DataStream::writeRawData(const char *data, int len) {
    if(m_input){
        size_t off = m_input->size();
        m_input->resize(off + len);
        for(size_t i = 0; i < len; ++i){
            (*m_input)[off + i] = data[i];
        }
    }
}
