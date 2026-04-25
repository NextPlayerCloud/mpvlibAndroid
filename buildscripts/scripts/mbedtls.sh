#!/bin/bash -e

. ../../include/path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf _build$ndk_suffix
	exit 0
else
	exit 255
fi

mkdir -p _build$ndk_suffix
cd _build$ndk_suffix

if [ -z "$android_abi" ]; then
	echo "Error: android_abi is not set. buildall.sh must export it." >&2
	exit 1
fi

cmake -G Ninja \
	-DCMAKE_TOOLCHAIN_FILE="$DIR/sdk/android-ndk-${v_ndk}/build/cmake/android.toolchain.cmake" \
	-DANDROID_ABI="$android_abi" \
	-DANDROID_PLATFORM=android-24 \
	-DCMAKE_INSTALL_PREFIX="$prefix_dir" \
	-DENABLE_TESTING=OFF \
	-DENABLE_PROGRAMS=OFF \
	-DCMAKE_BUILD_TYPE=Release \
	..

cmake --build . -j"$cores"
cmake --install .

echo "mbedtls installed to $prefix_dir"
