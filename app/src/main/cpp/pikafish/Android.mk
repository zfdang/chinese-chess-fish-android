LOCAL_PATH := $(call my-dir)
PIKAFISH_SOURCES := $(filter-out src/main.cpp src/universal/%, $(patsubst $(LOCAL_PATH)/%,%, $(wildcard $(LOCAL_PATH)/src/*.cpp $(LOCAL_PATH)/src/*/*.cpp $(LOCAL_PATH)/src/*/*/*.cpp)))

include $(CLEAR_VARS)
LOCAL_MODULE := pikafish
LOCAL_SRC_FILES := jni_bridge.cpp $(PIKAFISH_SOURCES)
LOCAL_CPPFLAGS := -std=c++20 -O3 -DNDEBUG -DIS_64BIT -DUSE_POPCNT -DUSE_NEON=8 -DZSTD_DISABLE_ASM
LOCAL_CPP_FEATURES := exceptions rtti
LOCAL_LDFLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := pikafish_dotprod
LOCAL_SRC_FILES := jni_bridge.cpp $(PIKAFISH_SOURCES)
LOCAL_CPPFLAGS := -std=c++20 -O3 -DNDEBUG -DIS_64BIT -DUSE_POPCNT -DUSE_NEON=8 -DUSE_NEON_DOTPROD -DZSTD_DISABLE_ASM -march=armv8.2-a+dotprod
LOCAL_CPP_FEATURES := exceptions rtti
LOCAL_LDFLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
include $(BUILD_SHARED_LIBRARY)
