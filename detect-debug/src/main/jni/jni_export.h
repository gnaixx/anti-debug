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
#include <sys/inotify.h>

#include "gtuil.h"

#define EVENT_SIZE (sizeof (struct inotify_event))
#define EVENT_BUFF_LEN (1024 * EVENT_SIZE)
#define BUFF_LEN 1024 * 4
#define PATH_LEN 1024
#define TCP_PORT "5D8A"                             //23946


void *inotify_read();

JNIEXPORT void start_inotify_read(JNIEnv *, jclass);

void *inotify_select();

JNIEXPORT void start_inotify_select(JNIEnv *, jclass);

JNIEXPORT void stop_inotify(JNIEnv *, jclass, jint);

JNIEXPORT void tcp_monitor(JNIEnv *, jclass);

JNIEXPORT jint JNI_OnLoad(JavaVM *, void *);

void JNI_OnUnLoad(JavaVM *, void *);

JNIEnv *g_env;
jclass native_class;
int stop = 0;            //是否停止监控

static JNINativeMethod methods[] = {
        {"startInotifyByRead",   "()V",  start_inotify_read},
        {"startInotifyBySelect", "()V",  start_inotify_select},
        {"stopInotify",          "(I)V", stop_inotify},
        {"netMonitor",           "()V",  tcp_monitor},
};
#endif //ANTI_REVERSE_JNI_EXPORT_H
