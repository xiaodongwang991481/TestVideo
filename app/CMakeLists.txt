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

set(ffmpeg_include_dir ${CMAKE_SOURCE_DIR}/src/main/cpp/includes/${ANDROID_ABI})
include_directories(${ffmpeg_include_dir})
set(ffmpeg_lib_dir ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})
link_directories(${ffmpeg_lib_dir})

add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_lib_dir}/libavutil.so)
add_library(swscale SHARED IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_lib_dir}/libswscale.so)
add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_lib_dir}/libswresample.so)
add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_lib_dir}/libavcodec.so)
add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_lib_dir}/libavformat.so)
add_library(avfilter SHARED IMPORTED)
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_lib_dir}/libavfilter.so)
add_library(avdevice SHARED IMPORTED)
set_target_properties(avdevice PROPERTIES IMPORTED_LOCATION
    ${ffmpeg_lib_dir}/libavdevice.so)

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
                       avdevice
                       avfilter
                       avformat
                       avcodec
                       swresample
                       swscale
                       avutil
                       -ljnigraphics
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )



