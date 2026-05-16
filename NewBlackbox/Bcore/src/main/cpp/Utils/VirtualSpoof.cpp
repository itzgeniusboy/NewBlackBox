#include <sys/system_properties.h>
#include <cstring>
#include "./xdl.h"
#include <android/log.h>
#include <dlfcn.h>
#include "Dobby/dobby.h"

#define LOG_TAG "VirtualSpoof"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

struct SpoofedProp {
    const char* key;
    const char* value;
};

SpoofedProp spoofed_props[] = {
        // Basic device spoofing
        {"ro.product.model", "Pixel 6"},
        {"ro.product.brand", "google"},
        {"ro.product.manufacturer", "Google"},
        {"ro.product.device", "oriole"},
        {"ro.product.name", "oriole"},
        {"ro.build.fingerprint", "google/oriole/oriole:12/SP1A.210812.015/7679548:user/release-keys"},
        {"ro.build.version.release", "12"},
        {"ro.build.version.sdk", "31"},
        {"ro.build.version.codename", "S"},
        {"ro.build.version.security_patch", "2022-01-05"},
        {"ro.serialno", "1A2B3C4D5E6F"},
        {"ro.hardware", "qcom"},
        {"ro.boot.hardware", "qcom"},
        {"ro.product.board", "lahaina"},
        {"ro.product.cpu.abi", "arm64-v8a"},
        {"ro.build.type", "user"},
        {"ro.build.tags", "release-keys"},
        // Emulator detection bypass
        {"ro.kernel.qemu", "0"},
        {"ro.kernel.android.qemud", ""},
        {"ro.hardware.egl", "adreno"},
        {"ro.boot.qemu", "0"},
        // Bootloader / verified boot (makes tampering harder to detect)
        {"ro.boot.flash.locked", "1"},
        {"ro.boot.verifiedbootstate", "green"},
        {"ro.boot.vbmeta.digest", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"},
        {"ro.boot.veritymode", "enforcing"},
        {"ro.boot.veritymode.managed", "yes"},
        // Additional properties that might be checked
        {"ro.product.first_api_level", "31"},
        {"ro.build.date.utc", "1640995200"},
        {nullptr, nullptr} 
};

static int (*orig_system_property_get)(const char *name, char *value) = nullptr;

int my_system_property_get(const char *name, char *value) {
    for (int i = 0; spoofed_props[i].key != nullptr; ++i) {
        if (strcmp(name, spoofed_props[i].key) == 0) {
            strcpy(value, spoofed_props[i].value);
            LOGD("[spoof] %s = %s", name, value);
            return strlen(value);
        }
    }
    if (orig_system_property_get) {
        return orig_system_property_get(name, value);
    }
    value[0] = '\0';
    return 0;
}

void install_property_get_hook() {
    void* handle = xdl_open("libc.so", XDL_DEFAULT);
    void* target = xdl_dsym(handle, "__system_property_get", nullptr);
    if (target) {
        if (DobbyHook(target, (void*)my_system_property_get, (void**)&orig_system_property_get) == 0) {
            LOGD("Spoof installed successfully");
        } else {
            LOGD("Spoof hook failed");
        }
        xdl_close(handle);
    } else {
        xdl_close(handle);
        // Fallback: try direct dlsym
        target = dlsym(RTLD_DEFAULT, "__system_property_get");
        if (target) {
            if (DobbyHook(target, (void*)my_system_property_get, (void**)&orig_system_property_get) == 0) {
                LOGD("Spoof installed via dlsym");
            }
        }
    }
}

__attribute__((constructor)) void init_virtual_spoof() {
    install_property_get_hook();
    LOGD("VirtualSpoof: __system_property_get hook loaded");
}
