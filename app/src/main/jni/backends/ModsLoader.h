#include <jni.h>
#include <string>
#include <android/log.h>
#include "backends/openssl/md5.h"
#include "backends/openssl/sha.h"
#include "curl/curl.h"
#include "json.hpp"

using json = nlohmann::ordered_json;

time_t rng = 0;

std::string g_Token, g_Auth;
bool bValid = false;
bool VerifiedActivity = false;
std::string EXP = "NULL";

const char *GetAndroidID(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getContentResolverMethod = env->GetMethodID(contextClass,"getContentResolver","()Landroid/content/ContentResolver;");
    jclass settingSecureClass = env->FindClass("android/provider/Settings$Secure");
    jmethodID getStringMethod = env->GetStaticMethodID(settingSecureClass,"getString", "(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;");
    auto obj = env->CallObjectMethod(context, getContentResolverMethod);
    auto str = (jstring) env->CallStaticObjectMethod(settingSecureClass, getStringMethod, obj,env->NewStringUTF("android_id"));
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceModel(JNIEnv *env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID modelId = env->GetStaticFieldID(buildClass, "MODEL","Ljava/lang/String;");
    auto str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceBrand(JNIEnv *env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID modelId = env->GetStaticFieldID(buildClass, "BRAND","Ljava/lang/String;");
    auto str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetPackageName(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getPackageNameId = env->GetMethodID(contextClass, "getPackageName","()Ljava/lang/String;");
    auto str = (jstring) env->CallObjectMethod(context, getPackageNameId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceUniqueIdentifier(JNIEnv *env, const char *uuid) {
    jclass uuidClass = env->FindClass("java/util/UUID");
    auto len = strlen(uuid);
    jbyteArray myJByteArray = env->NewByteArray(len);
    env->SetByteArrayRegion(myJByteArray, 0, len, (jbyte *) uuid);
    jmethodID nameUUIDFromBytesMethod = env->GetStaticMethodID(uuidClass,"nameUUIDFromBytes","([B)Ljava/util/UUID;");
    jmethodID toStringMethod = env->GetMethodID(uuidClass, "toString","()Ljava/lang/String;");
    auto obj = env->CallStaticObjectMethod(uuidClass, nameUUIDFromBytesMethod, myJByteArray);
    auto str = (jstring) env->CallObjectMethod(obj, toStringMethod);
    return env->GetStringUTFChars(str, 0);
}

struct MemoryStruct {
    char *memory;
    size_t size;
};

static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct MemoryStruct *mem = (struct MemoryStruct *) userp;

    mem->memory = (char *) realloc(mem->memory, mem->size + realsize + 1);
    if (mem->memory == NULL) {
        return 0;
    }

    memcpy(&(mem->memory[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->memory[mem->size] = 0;

    return realsize;
}

std::string CalcMD5(std::string s) {
    std::string result;

    unsigned char hash[MD5_DIGEST_LENGTH];
    char tmp[4];

    MD5_CTX md5;
    MD5_Init(&md5);
    MD5_Update(&md5, s.c_str(), s.length());
    MD5_Final(hash, &md5);
    for (unsigned char i : hash) {
        sprintf(tmp, "%02x", i);
        result += tmp;
    }
    return result;
}

std::string CalcSHA256(std::string s) {
    std::string result;

    unsigned char hash[SHA256_DIGEST_LENGTH];
    char tmp[4];

    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    SHA256_Update(&sha256, s.c_str(), s.length());
    SHA256_Final(hash, &sha256);
    for (unsigned char i : hash) {
        sprintf(tmp, "%02x", i);
        result += tmp;
    }
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_onecore_loader_activity_LoginActivity_Check(JNIEnv *env, jclass clazz, jobject mContext, jstring mUserKey) {
    auto user_key = env->GetStringUTFChars(mUserKey, 0);
    std::string hwid = user_key;
    hwid += GetAndroidID(env, mContext);
    hwid += GetDeviceModel(env);
    hwid += GetDeviceBrand(env);
    std::string UUID = GetDeviceUniqueIdentifier(env, hwid.c_str());
    std::string errMsg;
    struct MemoryStruct chunk{};
    chunk.memory = (char *) malloc(1);
    chunk.size = 0;

    CURL *curl;
    CURLcode res;
    curl = curl_easy_init();
    if (curl) {
        char lol[1000];
        sprintf(lol, OBFUSCATE("https://darkdevel.dynamicflash.xyz/connect"));
        curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "POST");
        curl_easy_setopt(curl, CURLOPT_URL, lol);
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1);
        curl_easy_setopt(curl, CURLOPT_DEFAULT_PROTOCOL, "https");
        struct curl_slist *headers = NULL;
        headers = curl_slist_append(headers, "Accept: application/json");
        headers = curl_slist_append(headers,"Content-Type: application/x-www-form-urlencoded");
        headers = curl_slist_append(headers, "Charset: UTF-8");
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        char data[4096];
        sprintf(data, "game=PUBG&user_key=%s&serial=%s", user_key, UUID.c_str());
        curl_easy_setopt(curl, CURLOPT_POST, 1);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, data);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *) &chunk);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYSTATUS, 0);
        curl_easy_setopt(curl, CURLOPT_USERAGENT, "AbsoluteX/2.0");
        res = curl_easy_perform(curl);
        
        if (res == CURLE_OK) {
            try {
                json result = json::parse(chunk.memory);
                auto STATUS = std::string{"status"};
                
                if (result[STATUS] == true) {
                    // Check if this is maintenance mode (has 'reason' but no 'data')
                    if (result.contains("reason") && !result.contains("data")) {
                        // Maintenance mode - return the message as error
                        std::string reason = result["reason"].get<std::string>();
                        errMsg = reason;
                        __android_log_print(ANDROID_LOG_ERROR, "DYNAMIC", "Maintenance Mode: %s", reason.c_str());
                        bValid = false;
                        VerifiedActivity = false;
                    } 
                    // Normal successful response with data
                    else if (result.contains("data")) {
                        std::string token = result["data"]["token"].get<std::string>();
                        EXP = result["data"]["EXP"].get<std::string>();
                        rng = result["data"]["rng"].get<time_t>();
                        
                        if (rng + 30 > time(0)) {
                            std::string auth = "PUBG";
                            auth += "-";
                            auth += user_key;
                            auth += "-";
                            auth += UUID;
                            auth += "-";
                            auth += "Vm8Lk7Uj2JmsjCPVPVjrLa7zgfx3uz9E";
                            std::string outputAuth = CalcMD5(auth);
                            g_Token = token;
                            g_Auth = outputAuth;
                            bValid = g_Token == g_Auth;
                            VerifiedActivity = true;
                            
                            if (bValid) {
                                __android_log_print(ANDROID_LOG_INFO, "DYNAMIC", "Login Success");
                            }
                        } else {
                            errMsg = "Timestamp validation failed";
                        }
                    } else {
                        errMsg = "Invalid response format";
                    }
                } else {
                    // Status is false - error response
                    if (result.contains("reason")) {
                        errMsg = result["reason"].get<std::string>();
                    } else {
                        errMsg = "Unknown error";
                    }
                    __android_log_print(ANDROID_LOG_ERROR, "DYNAMIC", "Login Failed: %s", errMsg.c_str());
                }
            } catch (json::exception &e) {
                errMsg = e.what();
                __android_log_print(ANDROID_LOG_ERROR, "DYNAMIC", "JSON Parse Error: %s", e.what());
                __android_log_print(ANDROID_LOG_ERROR, "DYNAMIC", "Response: %s", chunk.memory);
            }
        } else {
            errMsg = curl_easy_strerror(res);
            __android_log_print(ANDROID_LOG_ERROR, "DYNAMIC", "CURL Error: %s", errMsg.c_str());
        }
    }
    
    curl_easy_cleanup(curl);
    free(chunk.memory);
    
    // Return the appropriate message
    if (bValid) {
        return env->NewStringUTF("OK");
    } else {
        return env->NewStringUTF(errMsg.c_str());
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_onecore_loader_libhelper_DownloadZip_PASSJKPAPA(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(OBFUSCATE("0000"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_onecore_loader_activity_MainActivity_FixCrash(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(OBFUSCATE("https://github.com/ravipacharpro-jpg/onecore-Offset-Pro/releases/download/Loader/Saved.zip"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_onecore_loader_activity_MainActivity_TimeExpired(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(EXP.c_str());
}
