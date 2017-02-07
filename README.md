# APK反逆向之一：监控debug
---

> 在开发和逆向过程中很多时候都需要动态调试，开发时候可以用开发 android 的 IDE进行调试，native层也可用调试，Android Studio早就可以进行 native 的debug调试了。但是在 release 后的 apk 如果还检测到了 debug 调试，那么说明该 apk 正被破解。

## 0x00 简介
在 apk 被调试的时候，有很多特征可以检测到，比如 hook so的时候需要分析 maps文件确定内存加载的位置，还有调试器很 android 设备进行接口通讯需要开启端口映射。这些特征都可以被作为检测 debug 的一种手段。 
 
下面介绍了几种检测 debug 的方式，有些案例只是介绍思路，具体的实现方式需要进行更改，例如监控 tcp 端口，需要改成 service 形式在后台运行。    

检测 debug 是为了防止应用被逆向动态分析，所以检测的方法也都是采用 native 开发提高被逆向的成本。

源码地址：[anti-debug](https://github.com/gnaixx/anti-debug)

## 0x01 debug开关
debug 开关默认在编译 release 版本的时候自己会关闭，但是你还是可以通过显示的设置把他打开。但是如果你这么干了，估计你老板要打死你。

release 版本开启 debug 调试，修改项目 build.gradle中 的 buildTypes 参数：`debuggable true`

```bash
android {
	buildTypes {
        release {
            debuggable true
            minifyEnabled false
            proguardFiles.add(file("proguard-rules.pro"))
            signingConfig = $("android.signingConfigs.myConfig")
        }
    }
}
```

获取 debuggable 的值也很简单通过API接口就可以：

```java
void detectOsDebug(){
    boolean connected = android.os.Debug.isDebuggerConnected();
    Log.d(TAG, "debugger connect status:" + connected);
}
```
这种方式获取的值其实意义不大，发布的 release 版本基本没有会开启的除非失误。

## 0x02 单步检测
单步调试的原理很简单：检测某段代码执行的时间，动态调试的时候肯定会在一些地方下断点，如果一段代码执行时间超过2秒（这里需要排除耗时的io读写等操作），则可以认为 apk 可能被动态分析。

示例代码：

```c
JNIEXPORT void single_step(){
    time(&start_time);
    //实际需要监控的代码
    sleep(4);
    //---------------
    time(&end_time);

    LOGD("start time:%d, end time:%d", start_time, end_time);
    if(end_time - start_time > 2){
        LOGD("fit single_step");
    }
}
```

这里的时间间隔可以根据实际情况作调整。

## 0x03 监控TarcePid
在 apk 被附加进程的时候在`/proc/{pid}/status`,`/proc/{pid}/task/{pid}/status`文件中会保存附件进程的 pid ：`TarcePid : 1212`。只需要读取这两个文件中的 TarcePid 是不是为0，如果不为0则可能被附加了进程。

示例代码：

```c
void tarce_pid(char* path){
    char buf[BUFF_LEN];
    FILE *fp;
    int trace_pid = 0;
    fp = fopen(path, "r");
    if (fp == NULL) {
        LOGE("status open failed:[error:%d, desc:%s]", errno, strerror(errno));
        return;
    }

    while (fgets(buf, BUFF_LEN, fp)) {
        if (strstr(buf, "TracerPid")) {
            char *strok_rPtr, *temp;
            temp = strtok_r(buf, ":", &strok_rPtr);
            temp = strtok_r(NULL, ":", &strok_rPtr);
            trace_pid = atoi(temp);
            LOGD("%s, TarcePid:%d", path, trace_pid);
        }
    }

    fclose(fp);
    return;
}

JNIEXPORT void tarce_pid_monitor(){
    LOGD("tarce_pid_monitor");
    int pid = getpid();
    char path[BUFF_LEN];

    sprintf(path, "/proc/%d/status", pid);
    tarce_pid(path);

    sprintf(path, "/proc/%d/task/%d/status", pid, pid);
    tarce_pid(path);
}
```

检测结果：

```bash
10-13 18:31:52.716 11538-11538/cc.gnaixx.detect_debug D/GNAIXX_NDK: tarce_pid_monitor
10-13 18:31:52.716 11538-11538/cc.gnaixx.detect_debug D/GNAIXX_NDK: /proc/11538/status, TarcePid:11669
10-13 18:31:52.716 11538-11538/cc.gnaixx.detect_debug D/GNAIXX_NDK: /proc/11538/task/11538/status, TarcePid:11669
```

## 0x04 监控tcp端口
进行 debug 调试必然会开启端口映射，我们可以监控比较常用的逆向工具开启的端口，当然作弊者也可以修改端口。但是前提也是在了解了检测手段下。Android中开启的端口会保存在文件`proc/net/tcp`文件中。

示例代码：

```c
JNIEXPORT void tcp_monitor(JNIEnv *env, jclass thiz){
    LOGD("tcp_monitor");
    char buff[BUFF_LEN];

    FILE *fp;
    const char dir[] = "/proc/net/tcp";
    fp = fopen(dir, "r");
    if(fp == NULL){
        LOGE("file failed [errno:%d, desc:%s]", errno, strerror(errno));
        return;
    }
    while(fgets(buff, BUFF_LEN, fp)){
        if(strstr(buff, TCP_PORT) != NULL){
            LOGI("Line:%s", buff);
            fclose(fp);
            return;
        }
    }
}
```
这里的 TCP_PORT 为 "5D8A",也就是10进制的23946，这是ida默认的端口。

## 0x05 监控maps文件

`/proc/{pid}/maps`文件中保存了 app 运行的加载的内存信息。所有maps文件被进行ACCESS 或者 OPEN 操作都是有风险的。

可以通过 inotify 对 maps 文件进行监控，这里采用了子线程进行循环监控。

这里采用两种方式进行监控，一种阻塞的方式，一种非阻塞的方式（通过select）。

### 阻塞
代码示例：

```c
void *inotify_maps_block() {
    LOGD("start by block");
    int fd;                         //文件描述符
    int wd;                         //监视器标识符
    int event_len;                  //事件长度
    char buffer[EVENT_BUFF_LEN];    //事件buffer
    char map_path[PATH_LEN];        //监控文件路径

    stop = 0;                       //初始化监控
    fd = inotify_init();
    pid_t pid = getpid();
    sprintf(map_path, "/proc/%d/", pid); //获取当前APP maps路径
    if (fd == -1) {
        LOGE("inotify_init [errno:%d, desc:%s]", errno, strerror(errno));
        return NULL;
    }
    wd = inotify_add_watch(fd, map_path, IN_ALL_EVENTS);  //添加监控 所有事件
    LOGD("add watch success path:%s", map_path);
    while (1) {
        if (stop == 1) break;       //停止监控

        event_len = read(fd, buffer, EVENT_BUFF_LEN);   //读取事件
        if (event_len < 0) {
            LOGE("inotify_event read failed [errno:%d, desc:%s]", errno, strerror(errno));
            return NULL;
        }
        int i = 0;
        while (i < event_len) {
            struct inotify_event *event = (struct inotify_event *) &buffer[i];
            //过滤maps文件
            if (event->len && !strcmp(event->name, "maps")) {
                if (event->mask & IN_CREATE) {
                    LOGD("create: %s", event->name);
                }
                else if (event->mask & IN_DELETE) {
                    LOGD("delete: %s", event->name);
                }
                else if (event->mask & IN_MODIFY) {
                    LOGD("modified: %s", event->name);
                }
                else if (event->mask & IN_ACCESS) {
                    LOGD("access: %s", event->name);
                }
                else if (event->mask & IN_OPEN) {
                    LOGD("open : %s", event->name);
                }
                else {
                    LOGD("other event [name:%s, mask:%x]", event->name, event->mask);
                }
            }
            i += EVENT_SIZE + event->len;
        }
    }
    inotify_rm_watch(fd, wd);
    LOGD("rm watch");
    close(fd);
}
```
阻塞方法监控的是`/proc/{pid}/`文件夹，如果直接监控 maps 文件，可能造成无法结束线程。如果正常用户没有对 maps 文件操作，那么函数就会一直阻塞在 `read()` 方法。而监控 `/proc/{pid}` 文件夹，改文件夹下其他文件会有操作,所以不会阻塞在`read()`。

### 非阻塞
代码示例：

```c
void *inotify_maps_unblock() {
    LOGD("start by unblock");
    int fd;                         //文件描述符
    int wd;                         //监视器标识符
    int event_len;                  //事件长度
    char buffer[EVENT_BUFF_LEN];    //事件buffer
    char map_path[PATH_LEN];        //监控文件路径

    fd_set fds;                     //fd_set
    struct timeval time_to_wait;    //超时时间
    stop = 0;

    //初始化监控
    fd = inotify_init();
    pid_t pid = getpid();
    sprintf(map_path, "/proc/%d/maps", pid); //获取当前APP maps路径
    if (fd == -1) {
        LOGE("inotify_init [errno:%d, desc:%s]", errno, strerror(errno));
        return NULL;
    }
    wd = inotify_add_watch(fd, map_path, IN_ALL_EVENTS);  //添加监控 所有事件
    LOGD("add watch success path:%s, fd:%d, wd:%d", map_path, fd, wd);

    while (1) {
        if (stop == 2) break;       //停止监控

        FD_ZERO(&fds);
        FD_SET(fd, &fds);

        //之前我把初始化放在循环外 第一次可以阻塞,后面就直接跳过了
        time_to_wait.tv_sec = 3;
        time_to_wait.tv_usec = 0;

        int rev = select(fd + 1, &fds, NULL, NULL, &time_to_wait);//fd, readfds, writefds, errorfds, timeout:NULL阻塞, {0.0}直接过, timeout
        //int rev = select(fd + 1, &fds, NULL, NULL, NULL);//fd, readfds, writefds, errorfds, timeout:NULL阻塞, {0.0}直接过, timeout
        LOGD("select status_code: %d", rev);
        if (rev < 0) {
            //error
            LOGE("select failed [error:%d, desc:%s]", errno, strerror(errno));
        }
        else if (rev == 0) {
            //timeout
            LOGD("select timeout");
        }
        else {
            //
            event_len = read(fd, buffer, EVENT_BUFF_LEN);   //读取事件
            if (event_len < 0) {
                LOGE("inotify_event read failed [errno:%d, desc:%s]", errno, strerror(errno));
                return NULL;
            }
            int i = 0;
            while (i < event_len) {
                //注意:这里监控的是maps文件,所以event->name 参数为空
                struct inotify_event *event = (struct inotify_event *) &buffer[i];
                if (event->mask & IN_CREATE) {
                    LOGD("create: %s", event->name);
                }
                else if (event->mask & IN_DELETE) {
                    LOGD("delete: %s", event->name);
                }
                else if (event->mask & IN_MODIFY) {
                    LOGD("modified: %s", event->name);
                }
                else if (event->mask & IN_ACCESS) {
                    LOGD("access: %s", event->name);
                }
                else if (event->mask & IN_OPEN) {
                    LOGD("open : %s", event->name);
                }
                else {
                    LOGD("other event [name:%s, mask:%x]", event->name, event->mask);
                }
                i += EVENT_SIZE + event->len;
            }
        }
    }
    close(fd);
    inotify_rm_watch(fd, wd);
    LOGD("rm watch");
}
```
通过 `select()` 来绝对阻塞方式，最后一个参数（timeval）控制超时时间：

- NULL 阻塞与上面阻塞方式一样
- timeval 设置超时时间

timeval.tv_sec 为秒数
timeval.tv_usec 为微秒

**注** timeval 每次调用过 select 方法会被初始化为{0，0}，所以必须每次都在循环内复制。我也不知道为什么，试了好久。
