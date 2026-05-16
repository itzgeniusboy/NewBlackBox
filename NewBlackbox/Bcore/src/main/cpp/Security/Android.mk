LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := sdk_protection
LOCAL_SRC_FILES := NativeSdkProtection.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../Dobby

LOCAL_LDLIBS := -llog -ldl

LOCAL_CFLAGS := -O2 -fvisibility=hidden
LOCAL_CPPFLAGS := -std=c++14

include $(BUILD_SHARED_LIBRARY)
