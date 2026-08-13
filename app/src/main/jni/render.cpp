#include <jni.h>

#include <mpv/client.h>

#include "jni_utils.h"
#include "log.h"
#include "globals.h"
#include "render.h"

extern "C" {
    jni_func(void, attachSurface, jobject surface_);
    jni_func(void, replaceSurface, jobject surface_);
    jni_func(void, detachSurface);
    jni_func(void, attachOsdSurface, jobject surface_);
    jni_func(void, replaceOsdSurface, jobject surface_);
    jni_func(void, detachOsdSurface);
};

static jobject video_surface;
static jobject osd_surface;

static bool update_surface(JNIEnv *env, jobject *current_surface,
                           const char *property, jobject new_surface) {
    jobject new_ref = new_surface ? env->NewGlobalRef(new_surface) : NULL;
    if (new_surface && !new_ref) {
        ALOGE("failed to retain Android Surface for %s", property);
        return false;
    }

    int64_t wid = reinterpret_cast<intptr_t>(new_ref);
    int result = mpv_set_property(g_mpv, property, MPV_FORMAT_INT64, &wid);
    if (result < 0) {
        ALOGE("mpv_set_property(%s) returned error %s",
              property, mpv_error_string(result));
        if (new_ref)
            env->DeleteGlobalRef(new_ref);
        return false;
    }

    if (*current_surface)
        env->DeleteGlobalRef(*current_surface);
    *current_surface = new_ref;
    return true;
}

static void clear_surface(JNIEnv *env, jobject *current_surface,
                          const char *property) {
    if (!*current_surface)
        return;

    if (g_mpv) {
        int64_t wid = 0;
        int result = mpv_set_property(g_mpv, property, MPV_FORMAT_INT64, &wid);
        if (result < 0)
            ALOGE("mpv_set_property(%s) returned error %s",
                  property, mpv_error_string(result));
    }

    env->DeleteGlobalRef(*current_surface);
    *current_surface = NULL;
}

void release_surfaces(JNIEnv *env) {
    clear_surface(env, &video_surface, "wid");
    clear_surface(env, &osd_surface, "android-osd-wid");
}

static void set_surface(JNIEnv *env, jobject *current_surface,
                        const char *property, jobject surface_) {
    CHECK_MPV_INIT();
    if (!surface_)
        die("invalid surface provided");
    update_surface(env, current_surface, property, surface_);
}

jni_func(void, attachSurface, jobject surface_) {
    set_surface(env, &video_surface, "wid", surface_);
}

jni_func(void, replaceSurface, jobject surface_) {
    set_surface(env, &video_surface, "wid", surface_);
}

jni_func(void, detachSurface) {
    CHECK_MPV_INIT();
    update_surface(env, &video_surface, "wid", NULL);
}

jni_func(void, attachOsdSurface, jobject surface_) {
    set_surface(env, &osd_surface, "android-osd-wid", surface_);
}

jni_func(void, replaceOsdSurface, jobject surface_) {
    set_surface(env, &osd_surface, "android-osd-wid", surface_);
}

jni_func(void, detachOsdSurface) {
    CHECK_MPV_INIT();
    update_surface(env, &osd_surface, "android-osd-wid", NULL);
}
