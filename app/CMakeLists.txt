# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

set(ffmpeg_DIR ${CMAKE_SOURCE_DIR}/src/main/cpp/FFmpeg)
include_directories(${ffmpeg_DIR}/${ANDROID_ABI}/include)

add_library(libavutil SHARED IMPORTED)
set_target_properties(libavutil PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libavutil.so)
add_library(libswscale SHARED IMPORTED)
set_target_properties(libswscale PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libswscale.so)
add_library(libswresample SHARED IMPORTED)
set_target_properties(libswresample PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libswresample.so)
add_library(libavcodec SHARED IMPORTED)
set_target_properties(libavcodec PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libavcodec.so)
add_library(libavformat SHARED IMPORTED)
set_target_properties(libavformat PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libavformat.so)
add_library(libavfilter SHARED IMPORTED)
set_target_properties(libavfilter PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libavfilter.so)
add_library(libavdevice SHARED IMPORTED)
set_target_properties(libavdevice PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libavdevice.so)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                       libavdevice
                       libavfilter
                       libavformat
                       libavcodec
                       libswresample
                       libswscale
                       libavutil
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )



