# Credit goes to jmir1
# get versions from source code

# mpv version — try git describe first, fall back to VERSION file
if [ -d buildscripts/deps/mpv/.git ]; then
    MPV_VERSION=$(cd buildscripts/deps/mpv && git describe --tags --match 'v*' --dirty 2>/dev/null || git describe --always 2>/dev/null)
else
    MPV_VERSION=v$(cat buildscripts/deps/mpv/VERSION 2>/dev/null || echo "unknown")
fi
if [ -z "$MPV_VERSION" ]; then
    MPV_VERSION=v$(cat buildscripts/deps/mpv/VERSION 2>/dev/null || echo "unknown")
fi

# libplacebo version
if [ -f buildscripts/deps/libplacebo/_build$1/src/version.h ]; then
    LIBPLACEBO_VERSION=$(cat buildscripts/deps/libplacebo/_build$1/src/version.h | grep "#define BUILD_VERSION" | cut -d '"' -f 2)
else
    LIBPLACEBO_VERSION=unknown
fi

# ffmpeg version
if [ -d buildscripts/deps/ffmpeg/.git ]; then
    FFMPEG_VERSION=$(cd buildscripts/deps/ffmpeg/ && git rev-parse --short HEAD 2>/dev/null || echo "unknown")
else
    FFMPEG_VERSION=unknown
fi

# get build date from compiled object file
DATE=unknown
OBJ_FILE=buildscripts/deps/mpv/_build$1/libmpv.so.p/common_version.c.o
if [ -f "$OBJ_FILE" ]; then
    RODATA_SECTION=$(readelf "$OBJ_FILE" -S 2>/dev/null | grep .rodata || true)
    if [ -n "$RODATA_SECTION" ]; then
        START_RODATA=0x$(echo "$RODATA_SECTION" | cut -d ' ' -f 27)
        START=0x$(readelf "$OBJ_FILE" -s 2>/dev/null | grep mpv_builddate | cut -d ' ' -f 7)
        SIZE=$(readelf "$OBJ_FILE" -s 2>/dev/null | grep mpv_builddate | cut -d ' ' -f 11)
        if [ -n "$START" ] && [ -n "$SIZE" ]; then
            SKIP=$(($START_RODATA + $START - 1))
            dd if="$OBJ_FILE" of=date.txt bs=1 skip=$SKIP count=$SIZE 2>/dev/null
            DATE=$(cat date.txt 2>/dev/null)
            rm -f date.txt
        fi
    fi
fi

# write versions to Utils.kt
sed -i "s/%MPV_VERSION%/$MPV_VERSION/g" app/src/main/java/is/xyz/mpv/Utils.kt
sed -i "s/%LIBPLACEBO_VERSION%/$LIBPLACEBO_VERSION/g" app/src/main/java/is/xyz/mpv/Utils.kt
sed -i "s/%FFMPEG_VERSION%/$FFMPEG_VERSION/g" app/src/main/java/is/xyz/mpv/Utils.kt
sed -i "s/%DATE%/$DATE/g" app/src/main/java/is/xyz/mpv/Utils.kt