#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <pty.h>
#include <cstdlib>
#include <cstring>
#include <cerrno>
#include <android/log.h>

#define TAG "UVOS_PTY_NATIVE"
#ifdef NDEBUG
  #define LOGE(...) ((void)0)
#else
  #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#endif

static int get_descriptor_field(JNIEnv *env, jobject fileDescriptor) {
    if (!fileDescriptor) return -1;
    jclass fdClass = env->GetObjectClass(fileDescriptor);
    jfieldID descriptorField = env->GetFieldID(fdClass, "descriptor", "I");
    return env->GetIntField(fileDescriptor, descriptorField);
}

static jobject create_file_descriptor(JNIEnv *env, int fd) {
    jclass fdClass = env->FindClass("java/io/FileDescriptor");
    jmethodID fdConstructor = env->GetMethodID(fdClass, "<init>", "()V");
    jfieldID descriptorField = env->GetFieldID(fdClass, "descriptor", "I");
    jobject obj = env->NewObject(fdClass, fdConstructor);
    env->SetIntField(obj, descriptorField, fd);
    return obj;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_uvos_terminal_jni_PtyBridge_nativeCreateSubprocess(
        JNIEnv *env,
        jclass /* clazz */,
        jstring jCmd,
        jobjectArray jEnv,
        jstring jCwd,
        jintArray jPidOut,
        jint rows,
        jint cols) {

    int masterFd = -1;
    struct winsize win = {
        .ws_row = static_cast<unsigned short>(rows),
        .ws_col = static_cast<unsigned short>(cols),
        .ws_xpixel = 0,
        .ws_ypixel = 0
    };

    pid_t pid = forkpty(&masterFd, nullptr, nullptr, &win);

    if (pid < 0) {
        LOGE("forkpty failed: %s", strerror(errno));
        return nullptr;
    }

    if (pid == 0) {
        // Child process
        if (jCwd != nullptr) {
            const char *cwd = env->GetStringUTFChars(jCwd, nullptr);
            if (cwd && chdir(cwd) != 0) {
                chdir("/");
            }
            if (cwd) env->ReleaseStringUTFChars(jCwd, cwd);
        }

        // Environment variables
        setenv("TERM", "xterm-256color", 1);
        setenv("HOME", "/data/data/com.uvos.terminal/files", 1);
        setenv("UVOS_ENVIRONMENT", "1", 1);

        if (jEnv != nullptr) {
            jsize envLen = env->GetArrayLength(jEnv);
            for (jsize i = 0; i < envLen; ++i) {
                auto envStr = (jstring)env->GetObjectArrayElement(jEnv, i);
                if (envStr) {
                    const char *cEnv = env->GetStringUTFChars(envStr, nullptr);
                    if (cEnv) {
                        putenv(strdup(cEnv));
                        env->ReleaseStringUTFChars(envStr, cEnv);
                    }
                }
            }
        }

        const char *cmd = env->GetStringUTFChars(jCmd, nullptr);
        char *argv[] = { (char *)"/system/bin/sh", (char *)"-c", (char *)cmd, nullptr };
        execvp("/system/bin/sh", argv);

        // Fallback direct execution
        char *singleArgv[] = { (char *)cmd, nullptr };
        execvp(cmd, singleArgv);

        _exit(127);
    }

    // Parent process
    jint pids[1] = { pid };
    env->SetIntArrayRegion(jPidOut, 0, 1, pids);

    // Non-blocking mode for Master PTY
    int flags = fcntl(masterFd, F_GETFL, 0);
    fcntl(masterFd, F_SETFL, flags | O_NONBLOCK);

    return create_file_descriptor(env, masterFd);
}

extern "C" JNIEXPORT void JNICALL
Java_com_uvos_terminal_jni_PtyBridge_nativeSetPtyWindowSize(
        JNIEnv *env,
        jclass /* clazz */,
        jobject fileDescriptor,
        jint rows,
        jint cols) {
    int fd = get_descriptor_field(env, fileDescriptor);
    if (fd < 0) return;

    struct winsize win = {
        .ws_row = static_cast<unsigned short>(rows),
        .ws_col = static_cast<unsigned short>(cols),
        .ws_xpixel = 0,
        .ws_ypixel = 0
    };
    ioctl(fd, TIOCSWINSZ, &win);
}

extern "C" JNIEXPORT void JNICALL
Java_com_uvos_terminal_jni_PtyBridge_nativeClosePty(
        JNIEnv *env,
        jclass /* clazz */,
        jobject fileDescriptor) {
    int fd = get_descriptor_field(env, fileDescriptor);
    if (fd >= 0) {
        close(fd);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_uvos_terminal_jni_PtyBridge_nativeWaitForProcess(
        JNIEnv * /* env */,
        jclass /* clazz */,
        jint pid) {
    if (pid <= 0) return -1;
    int status = 0;
    waitpid(pid, &status, 0);
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    return -1;
}
