#include <jni.h>
#include <android/log.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>
#include <setjmp.h>
#include <dlfcn.h>

#define LOG_TAG "NativeSdkProtection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static struct sigaction old_segv_handler = {0};
static sigjmp_buf crash_jmp_buf;
static volatile sig_atomic_t crash_recovered = 0;

// Custom SIGSEGV handler
static void sigsegv_handler(int sig, siginfo_t *info, void *context) {
    LOGE("=== SIGSEGV caught! si_addr=%p, si_code=%d ===", info->si_addr, info->si_code);
    
    // Option 1: Try to recover using longjmp (experimental, can lead to undefined behavior)
    // Uncomment the following lines if you want to attempt recovery
    /*
    if (!crash_recovered) {
        crash_recovered = 1;
        siglongjmp(crash_jmp_buf, 1);
    }
    */
    
    // Option 2: Pass to original handler if exists, otherwise re-raise
    if (old_segv_handler.sa_flags & SA_SIGINFO && old_segv_handler.sa_sigaction) {
        old_segv_handler.sa_sigaction(sig, info, context);
    } else if (old_segv_handler.sa_handler != SIG_DFL && old_segv_handler.sa_handler != SIG_IGN) {
        old_segv_handler.sa_handler(sig);
    } else {
        // Restore default action and re-raise
        signal(sig, SIG_DFL);
        raise(sig);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_top_niunaijun_blackbox_security_SdkProtectionManager_mediateLibraryLoadingNative(JNIEnv*, jobject) {
    LOGD("mediateLibraryLoadingNative enabled");
}

extern "C" JNIEXPORT void JNICALL
Java_top_niunaijun_blackbox_security_SdkProtectionManager_ensureSignalCompatibilityNative(JNIEnv*, jobject) {
    LOGD("Installing custom SIGSEGV handler");
    
    // Save the current handler
    sigaction(SIGSEGV, nullptr, &old_segv_handler);
    
    // Install our handler
    struct sigaction new_action = {0};
    new_action.sa_sigaction = sigsegv_handler;
    new_action.sa_flags = SA_SIGINFO | SA_RESTART;
    sigemptyset(&new_action.sa_mask);
    
    if (sigaction(SIGSEGV, &new_action, nullptr) == 0) {
        LOGD("SIGSEGV handler installed successfully");
    } else {
        LOGE("Failed to install SIGSEGV handler");
    }
}

// Optional: JNI function to set a recovery point (called from Java before running game code)
extern "C" JNIEXPORT jboolean JNICALL
Java_top_niunaijun_blackbox_security_SdkProtectionManager_setCrashRecoveryPoint(JNIEnv*, jobject) {
    if (sigsetjmp(crash_jmp_buf, 1) == 0) {
        return JNI_TRUE;  // First time, set the point
    } else {
        LOGD("Recovered from SIGSEGV via longjmp");
        return JNI_FALSE; // Recovered from crash
    }
}

extern "C" JNIEXPORT void JNICALL
Java_top_niunaijun_blackbox_security_SdkProtectionManager_virtualizePathsNative(JNIEnv*, jobject) {
    LOGD("virtualizePathsNative enabled");
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGD("NativeSdkProtection loaded");
    return JNI_VERSION_1_6;
}
