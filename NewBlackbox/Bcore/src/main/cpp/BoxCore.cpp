#include "BoxCore.h"
#include "Log.h"
#include "IO.h"
#include <jni.h>
#include <JniHook/JniHook.h>
#include <Hook/VMClassLoaderHook.h>
#include <Hook/UnixFileSystemHook.h>
#include <Hook/FileSystemHook.h>
#include <Hook/BinderHook.h>
#include <Hook/DexFileHook.h>
#include <Hook/RuntimeHook.h>
#include "Utils/HexDump.h"
#include "hidden_api.h"

// ========== ADDED for safe delayed hooks ==========
#include <sys/mman.h>
#include <spawn.h>
#include <dobby.h>
#include <dlfcn.h>
#include <string.h>
#include <thread>
#include <chrono>
#include <errno.h>
#include <android/log.h>
// =================================================

struct {
    JavaVM *vm;
    jclass NativeCoreClass;
    jmethodID getCallingUidId;
    jmethodID redirectPathString;
    jmethodID redirectPathFile;
    jmethodID loadEmptyDex;
    jmethodID loadEmptyDexL;
    int api_level;
} VMEnv;

JNIEnv *getEnv() {
    JNIEnv *env;
    VMEnv.vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

JNIEnv *ensureEnvCreated() {
    JNIEnv *env = getEnv();
    if (env == NULL) {
        VMEnv.vm->AttachCurrentThread(&env, NULL);
    }
    return env;
}

int BoxCore::getCallingUid(JNIEnv *env, int orig) {
    env = ensureEnvCreated();
    return env->CallStaticIntMethod(VMEnv.NativeCoreClass, VMEnv.getCallingUidId, orig);
}

jstring BoxCore::redirectPathString(JNIEnv *env, jstring path) {
    env = ensureEnvCreated();
    return (jstring) env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.redirectPathString, path);
}

jobject BoxCore::redirectPathFile(JNIEnv *env, jobject path) {
    env = ensureEnvCreated();
    return env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.redirectPathFile, path);
}

jlongArray BoxCore::loadEmptyDex(JNIEnv *env) {
    env = ensureEnvCreated();
    return (jlongArray) env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.loadEmptyDex);
}

int BoxCore::getApiLevel() {
    return VMEnv.api_level;
}

JavaVM *BoxCore::getJavaVM() {
    return VMEnv.vm;
}

void nativeHook(JNIEnv *env) {
    BaseHook::init(env);
    UnixFileSystemHook::init(env);
    FileSystemHook::init();
    VMClassLoaderHook::init(env);
    BinderHook::init(env);
    DexFileHook::init(env);
}

void hideXposed(JNIEnv *env, jclass clazz) {
    ALOGD("set hideXposed");
    VMClassLoaderHook::hideXposed();
}

void init(JNIEnv *env, jobject clazz, jint api_level) {
    ALOGD("NativeCore init.");
    VMEnv.api_level = api_level;
    VMEnv.NativeCoreClass = (jclass) env->NewGlobalRef(env->FindClass(VMCORE_CLASS));
    VMEnv.getCallingUidId = env->GetStaticMethodID(VMEnv.NativeCoreClass, "getCallingUid", "(I)I");
    VMEnv.redirectPathString = env->GetStaticMethodID(VMEnv.NativeCoreClass, "redirectPath",
                                                      "(Ljava/lang/String;)Ljava/lang/String;");
    VMEnv.redirectPathFile = env->GetStaticMethodID(VMEnv.NativeCoreClass, "redirectPath",
                                                    "(Ljava/io/File;)Ljava/io/File;");
    VMEnv.loadEmptyDex = env->GetStaticMethodID(VMEnv.NativeCoreClass, "loadEmptyDex",
                                                "()[J");
    JniHook::InitJniHook(env, api_level);
}

void addIORule(JNIEnv *env, jclass clazz, jstring target_path,
               jstring relocate_path) {
    ALOGD("set addIORule");
    IO::addRule(env->GetStringUTFChars(target_path, JNI_FALSE),
                env->GetStringUTFChars(relocate_path, JNI_FALSE));
}

bool disableHiddenApi(JNIEnv *env, jclass clazz) {
    ALOGD("set disableHiddenApi");
    if(!disable_hidden_api(env)){
        ALOGD("set disableHiddenApi Fail!!!");
        return false;
    }
    return true;
}

bool disableResourceLoading(JNIEnv *env, jclass clazz) {
    ALOGD("set disableResourceLoading");
    if(!disable_resource_loading()){
        ALOGD("set disableResourceLoading Fail!!!");
        return false;
    }
    return true;
}

// ========== HOOK: mprotect ==========
static int (*original_mprotect)(void *addr, size_t len, int prot) = nullptr;

int mprotect_hook(void *addr, size_t len, int prot) {
    // Just pass through – no interference
    if (original_mprotect) {
        return original_mprotect(addr, len, prot);
    }
    // Fallback
    static int (*real_mprotect)(void*, size_t, int) = nullptr;
    if (!real_mprotect) real_mprotect = (int(*)(void*,size_t,int))dlsym(RTLD_DEFAULT, "mprotect");
    if (real_mprotect) return real_mprotect(addr, len, prot);
    return -1;
}

void install_mprotect_hook() {
    void *mprotect_ptr = dlsym(RTLD_DEFAULT, "mprotect");
    if (mprotect_ptr) {
        if (DobbyHook(mprotect_ptr, (void*)mprotect_hook, (void**)&original_mprotect) == 0) {
            __android_log_print(ANDROID_LOG_INFO, "BlackBox", "mprotect hook installed (delayed)");
        } else {
            __android_log_print(ANDROID_LOG_WARN, "BlackBox", "mprotect hook failed");
        }
    }
}

// ========== HOOK: ANOGS Ioctl ==========
typedef void* (*AnoSDK_Ioctl_t)(int cmd, const char* input);
static AnoSDK_Ioctl_t original_anogs_ioctl = nullptr;

void* anogs_ioctl_hook(int cmd, const char* input) {
    // Patch the emulator detection (cmd 10)
    if (cmd == 10) {
        __android_log_print(ANDROID_LOG_DEBUG, "ANOGS", "Patched cmd=10, returning empty");
        return (void*)"";
    }
    if (original_anogs_ioctl) {
        return original_anogs_ioctl(cmd, input);
    }
    return nullptr;
}

void install_anogs_hooks() {
    void *func = dlsym(RTLD_DEFAULT, "GCloud_AnoSDK_AnoSDK__Ioctl");
    if (!func) func = dlsym(RTLD_DEFAULT, "_ZN7AnoSDK5IoctlEiPKc");
    if (func) {
        if (DobbyHook(func, (void*)anogs_ioctl_hook, (void**)&original_anogs_ioctl) == 0) {
            __android_log_print(ANDROID_LOG_INFO, "BlackBox", "ANOGS Ioctl hook installed (delayed)");
        } else {
            __android_log_print(ANDROID_LOG_WARN, "BlackBox", "ANOGS hook failed");
        }
    } else {
        __android_log_print(ANDROID_LOG_WARN, "BlackBox", "ANOGS symbol not found, skipping");
    }
}

// ========== HOOK: posix_spawn (direct crash fix) ==========
static int (*original_posix_spawn)(pid_t *pid, const char *path,
                                   const posix_spawn_file_actions_t *file_actions,
                                   const posix_spawnattr_t *attrp,
                                   char *const argv[], char *const envp[]) = nullptr;

int posix_spawn_hook(pid_t *pid, const char *path,
                     const posix_spawn_file_actions_t *file_actions,
                     const posix_spawnattr_t *attrp,
                     char *const argv[], char *const envp[]) {
    __android_log_print(ANDROID_LOG_DEBUG, "BlackBox", "posix_spawn blocked: %s", path ? path : "(null)");
    errno = EPERM;
    return -1;
}

void install_posix_spawn_hook() {
    void *ptr = dlsym(RTLD_DEFAULT, "posix_spawn");
    if (ptr) {
        if (DobbyHook(ptr, (void*)posix_spawn_hook, (void**)&original_posix_spawn) == 0) {
            __android_log_print(ANDROID_LOG_INFO, "BlackBox", "posix_spawn hook installed (delayed)");
        } else {
            __android_log_print(ANDROID_LOG_WARN, "BlackBox", "posix_spawn hook failed");
        }
    } else {
        __android_log_print(ANDROID_LOG_WARN, "BlackBox", "posix_spawn not found");
    }
}
// ==========================================

void enableIO(JNIEnv *env, jclass clazz) {
    ALOGD("set enableIO");
    IO::init(env);
    nativeHook(env);
    
    // ===== DELAYED HOOK INSTALLATION (safety) =====
    std::thread([](){
        std::this_thread::sleep_for(std::chrono::seconds(2));
        install_mprotect_hook();
        install_anogs_hooks();
        install_posix_spawn_hook();
    }).detach();
}

static JNINativeMethod gMethods[] = {
        {"disableHiddenApi", "()Z",                               (void *) disableHiddenApi},
        {"disableResourceLoading", "()Z",                         (void *) disableResourceLoading},
        {"hideXposed", "()V",                                     (void *) hideXposed},
        {"addIORule",  "(Ljava/lang/String;Ljava/lang/String;)V", (void *) addIORule},
        {"enableIO",   "()V",                                     (void *) enableIO},
        {"init",       "(I)V",                                    (void *) init},
};

int registerNativeMethods(JNIEnv *env, const char *className,
                          JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, VMCORE_CLASS, gMethods,
                               sizeof(gMethods) / sizeof(gMethods[0])))
        return JNI_FALSE;
    return JNI_TRUE;
}

void registerMethod(JNIEnv *jenv) {
    registerNatives(jenv);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    VMEnv.vm = vm;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_EVERSION;
    }
    registerMethod(env);
    // No hooks here – clean startup
    return JNI_VERSION_1_6;
}
