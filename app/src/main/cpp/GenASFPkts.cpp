//
// Created by 1 on 26.11.2019.
//

#include "GenASFPkts.h"

#include "DataStream.h"

////////////////////////////////////////

//значение ASFDataHeader::ASF_ID
#define PRE_ASF_ID	0x82

//заголовок пакета передачи
struct ASFDataHeader{
    ASFDataHeader(){
        ASF_ID = PRE_ASF_ID;
        Reserved = 0;
        Length_type_flags = 0x01;
        Property_flags = 0x7D;
        Send_time = 0;
        Duration = 0;
        Segment_properties = 0;
    }

    uint8_t ASF_ID;
    uint16_t Reserved;
    uint8_t Length_type_flags;
    uint8_t Property_flags;
    uint32_t Send_time;
    uint16_t Duration;
    uint8_t Segment_properties;

    u_char paddingLengthType(){
        return (Length_type_flags >> 3) & 0x3;
    }
};

#define PRE_STREAM_ID	0x81

//заголовок пакета данных
struct ASFPayloadHeader{
    ASFPayloadHeader(){
        Stream_ID = PRE_STREAM_ID;
        Frame_number = 0;
        Fragment_offset = 0;
        Replicated_data_length = 0;
        Frame_size = 0;
        Send_time = 0;
        Payload_length = 0;
    }

    uint8_t Stream_ID;// = 0x81
    uint32_t Frame_number;
    uint32_t Fragment_offset;
    uint8_t Replicated_data_length;
    uint32_t Frame_size;
    uint32_t Send_time;
    uint16_t Payload_length;
};

////////////////////////////////////////

template<typename T >
inline T qMin(T a, T b)
{
    return a > b? b : a;
}

GenASFPkts::GenASFPkts() {

}

std::vector<bytearray> GenASFPkts::getPkts(const char *data, int len) {
    std::vector<bytearray> packet;

    if(!len || !data)
        return packet;

    ASFDataHeader dheader;
    ASFPayloadHeader pheader;

    bool done = false;

    uint FragmentOffset = 0;
    uint FrameSize = len;
    uint MaxSizePacket = 1200;

    while(!done){
        bytearray output;

        DataStream stream(&output);
        //stream.setByteOrder(DataStream::LittleEndian);

        stream << dheader.ASF_ID;

        //m_current_time_snapshot = QDateTime::currentMSecsSinceEpoch();

        stream << dheader.Reserved;
        stream << dheader.Length_type_flags;
        stream << dheader.Property_flags;
        stream << dheader.Send_time;
        stream << dheader.Duration;
        stream << dheader.Segment_properties;

        stream << pheader.Stream_ID;

        pheader.Frame_number = 0;
        pheader.Fragment_offset = FragmentOffset;
        pheader.Frame_size = len;
        pheader.Payload_length = qMin(FrameSize - FragmentOffset, MaxSizePacket);

        stream << pheader.Frame_number;
        stream << pheader.Fragment_offset;
        stream << pheader.Replicated_data_length;

        stream << pheader.Frame_size;
        stream << pheader.Send_time;
        stream << pheader.Payload_length;

        stream.writeRawData(data + FragmentOffset, pheader.Payload_length);

        FragmentOffset += pheader.Payload_length;

        if(FragmentOffset >= FrameSize)
            done = true;

        packet.push_back(output);
    }

    return packet;
}
