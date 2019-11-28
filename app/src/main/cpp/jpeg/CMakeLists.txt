cmake_minimum_required(VERSION 3.4.1)

add_library(jpeg SHARED
        jcapimin.c
        jmemmgr.c
        jmemansi.c
        jcomapi.c
        jcmarker.c
        jutils.c
        jerror.c
        jcparam.c
        jcapistd.c
        jcinit.c jcmaster.c jccolor.c jcsample.c jcprepct.c jcdctmgr.c jcphuff.c jchuff.c
        jccoefct.c jcmainct.c jdct.h jfdctint.c jfdctflt.c jfdctfst.c)

include_directories(.)