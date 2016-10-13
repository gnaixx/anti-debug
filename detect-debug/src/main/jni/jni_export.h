//
// Created by 薛祥清 on 2016/10/12.
//

#ifndef ANTI_REVERSE_JNI_EXPORT_H
#define ANTI_REVERSE_JNI_EXPORT_H
#include <jni.h>
#include <android/log.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <stdlib.h>
#include <sys/inotify.h>

#include "gtuil.h"

#define EVENT_SIZE (sizeof (struct inotify_event))
#define EVENT_BUFF_LEN (1024 * EVENT_SIZE)
#define BUFF_LEN 1024 * 4
#define PATH_LEN 1024
#define TCP_PORT "5D8A"                             //23946


//阻塞监控
void *inotify_maps_block();

//非阻塞监控
void *inotify_maps_unblock();

//开始监控
JNIEXPORT void start_inotify(JNIEnv *, jclass, jint);

//停止监控
JNIEXPORT void stop_inotify(JNIEnv *, jclass, jint);

//监控TCP端口
JNIEXPORT void tcp_monitor(JNIEnv *, jclass);

void tarce_pid(char *);

//监控TarcePid值
JNIEXPORT void tarce_pid_monitor();

time_t start_time;
time_t end_time;

JNIEXPORT void single_step();

//动态注册
JNIEXPORT jint JNI_OnLoad(JavaVM *, void *);
void JNI_OnUnLoad(JavaVM *, void *);


JNIEnv *g_env;
jclass native_class;
int stop = 0;            //是否停止监控

static JNINativeMethod methods[] = {
        {"startInotify",        "(I)V", start_inotify},
        {"stopInotify",         "(I)V", stop_inotify},
        {"tcpPortMonitor",      "()V",  tcp_monitor},
        {"tarcePidMonitor",     "()V",  tarce_pid_monitor},
        {"singleStepDetect",    "()V",  single_step},
};
#endif //ANTI_REVERSE_JNI_EXPORT_H
