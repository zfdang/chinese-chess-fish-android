LOCAL_PATH := $(call my-dir)
# Recurse instead of a fixed-depth wildcard: upstream adds deeper .cpp files over time, and a
# fixed-depth glob would silently drop them and fail at link time.
PIKAFISH_SOURCES := $(filter-out src/main.cpp src/universal/%, $(patsubst $(LOCAL_PATH)/%,%, $(shell find $(LOCAL_PATH)/src -name '*.cpp' -type f)))

# Match upstream's optimized clang build for both ISA variants. Keep NDK hardening and
# exceptions/RTTI; JNIEXPORT keeps the bridge visible with hidden engine implementation symbols.
PIKAFISH_OPT_CPPFLAGS := -funroll-loops -flto=full -fvisibility=hidden -fvisibility-inlines-hidden \
                         -mllvm -inline-threshold=500
PIKAFISH_EXPORTS := $(LOCAL_PATH)/jni_exports.map
PIKAFISH_OPT_LDFLAGS := -flto=full -O3 -Wl,--exclude-libs,ALL -Wl,--version-script=$(PIKAFISH_EXPORTS)

include $(CLEAR_VARS)
LOCAL_MODULE := pikafish
LOCAL_SRC_FILES := jni_bridge.cpp $(PIKAFISH_SOURCES)
LOCAL_CPPFLAGS := -std=c++20 -O3 -DNDEBUG -DIS_64BIT -DUSE_POPCNT -DUSE_NEON=8 -DZSTD_DISABLE_ASM
LOCAL_CPPFLAGS += $(PIKAFISH_OPT_CPPFLAGS)
LOCAL_CPP_FEATURES := exceptions rtti
LOCAL_ADDITIONAL_DEPENDENCIES := $(PIKAFISH_EXPORTS)
LOCAL_LDFLAGS := $(PIKAFISH_OPT_LDFLAGS) -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := pikafish_dotprod
LOCAL_SRC_FILES := jni_bridge.cpp $(PIKAFISH_SOURCES)
LOCAL_CPPFLAGS := -std=c++20 -O3 -DNDEBUG -DIS_64BIT -DUSE_POPCNT -DUSE_NEON=8 -DUSE_NEON_DOTPROD -DZSTD_DISABLE_ASM -march=armv8.2-a+dotprod
LOCAL_CPPFLAGS += $(PIKAFISH_OPT_CPPFLAGS)
LOCAL_CPP_FEATURES := exceptions rtti
LOCAL_ADDITIONAL_DEPENDENCIES := $(PIKAFISH_EXPORTS)
LOCAL_LDFLAGS := $(PIKAFISH_OPT_LDFLAGS) -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
include $(BUILD_SHARED_LIBRARY)
