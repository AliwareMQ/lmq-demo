cmake_minimum_required(VERSION 3.7)
project(mqttdemo)

INCLUDE_DIRECTORIES(
        .
        ${CMAKE_SOURCE_DIR}/src
        ${CMAKE_BINARY_DIR}
)
SET(CMAKE_BUILD_TYPE   "Debug")
SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g2 -ggdb")
SET(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")

set(CMAKE_C_STANDARD 99)


add_executable(mqttDemo mqttDemo.c)
TARGET_LINK_LIBRARIES(mqttDemo crypto)
TARGET_LINK_LIBRARIES(mqttDemo paho-mqtt3as)