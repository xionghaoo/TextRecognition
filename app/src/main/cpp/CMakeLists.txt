# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# 引入opencv库
set(ocvlibs "${PROJECT_SOURCE_DIR}/../../../../opencv_sdk_452/native/libs")
include_directories(${PROJECT_SOURCE_DIR}/include)

add_library(libopencv_java4 SHARED IMPORTED)
set_target_properties(libopencv_java4 PROPERTIES
        IMPORTED_LOCATION "${ocvlibs}/${ANDROID_ABI}/libopencv_java4.so")

# build application's shared lib
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

add_library( # Specifies the name of the library.
        native_lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native_lib.cpp)

# Specifies a path to native header files.
include_directories(native_lib PRIVATE include/)

# NDK提供的原生API
find_library( # Defines the name of the path variable that stores the
        # location of the NDK library.
        log-lib

        # Specifies the name of the NDK library that
        # CMake needs to locate.
        log )

target_link_libraries(
        native_lib
        libopencv_java4
        ${log-lib} )