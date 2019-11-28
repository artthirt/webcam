//
// Created by 1 on 26.11.2019.
//

#ifndef WEBCAM_GENASFPKTS_H
#define WEBCAM_GENASFPKTS_H

#include <vector>

#include "DataStream.h"

class GenASFPkts {
public:
    GenASFPkts();
    std::vector< bytearray > getPkts(const char* data, int len);
};


#endif //WEBCAM_GENASFPKTS_H
