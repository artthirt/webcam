//
// Created by 1 on 28.11.2019.
//

#ifndef WEBCAM_JPEGENC_H
#define WEBCAM_JPEGENC_H

#include "DataStream.h"

class jpegenc {
public:
    jpegenc();

    bool encode(const bytearray input[3], int width, int height, bytearray& output, int quality);
};


#endif //WEBCAM_JPEGENC_H
