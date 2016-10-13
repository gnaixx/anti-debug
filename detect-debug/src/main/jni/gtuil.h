//
// Created by 薛祥清 on 2016/10/12.
//

#ifndef ANTI_REVERSE_GTUIL_H
#define ANTI_REVERSE_GTUIL_H

#ifndef NELEM //计算元素个数
# define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif

#define TAG "GNAIXX_NDK"
//开发打印
#ifdef TAG
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#else
#define LOGI(...)
    #define LOGV(...)
    #define LOGD(...)
    #define LOGW(...)
    #define LOGE(...)
#endif

#endif //ANTI_REVERSE_GTUIL_H
