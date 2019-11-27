cmake_minimum_required(VERSION 3.4.1)

find_library(log-lib log)

add_library(sendasf SHARED sendasf.cpp GenASFPkts.cpp DataStream.cpp)

include_directories(.)

target_link_libraries(sendasf ${log-lib})