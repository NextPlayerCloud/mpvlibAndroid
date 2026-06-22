#!/bin/bash -e

. ../../include/path.sh
. ../../include/depinfo.sh
. ../../include/cmake-android.sh

build=_build$ndk_suffix

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf "$build"
	exit 0
else
	exit 255
fi

android_cmake_setup . "$build" \
	-DBUILD_CURL_EXE=OFF \
	-DBUILD_SHARED_LIBS=OFF \
	-DBUILD_STATIC_LIBS=ON \
	-DBUILD_STATIC_CURL=OFF \
	-DBUILD_LIBCURL_DOCS=OFF \
	-DBUILD_MISC_DOCS=OFF \
	-DENABLE_CURL_MANUAL=OFF \
	-DCURL_USE_MBEDTLS=ON \
	-DCURL_USE_OPENSSL=OFF \
	-DMBEDTLS_USE_STATIC_LIBS=ON \
	-DMBEDTLS_INCLUDE_DIR="$prefix_dir/include" \
	-DMBEDTLS_LIBRARY="$prefix_dir/lib/libmbedtls.a" \
	-DMBEDX509_LIBRARY="$prefix_dir/lib/libmbedx509.a" \
	-DMBEDCRYPTO_LIBRARY="$prefix_dir/lib/libmbedcrypto.a" \
	-DCURL_ZLIB=OFF \
	-DCURL_BROTLI=OFF \
	-DCURL_ZSTD=OFF \
	-DUSE_NGHTTP2=OFF \
	-DUSE_NGTCP2=OFF \
	-DUSE_QUICHE=OFF \
	-DUSE_LIBIDN2=OFF \
	-DCURL_USE_LIBPSL=OFF \
	-DCURL_USE_LIBSSH2=OFF \
	-DCURL_USE_LIBSSH=OFF \
	-DCURL_USE_GSSAPI=OFF \
	-DCURL_USE_GSASL=OFF \
	-DCURL_DISABLE_DICT=ON \
	-DCURL_DISABLE_DOH=ON \
	-DCURL_DISABLE_FILE=ON \
	-DCURL_DISABLE_GOPHER=ON \
	-DCURL_DISABLE_IMAP=ON \
	-DCURL_DISABLE_LDAP=ON \
	-DCURL_DISABLE_LDAPS=ON \
	-DCURL_DISABLE_MQTT=ON \
	-DCURL_DISABLE_POP3=ON \
	-DCURL_DISABLE_RTSP=ON \
	-DCURL_DISABLE_SMTP=ON \
	-DCURL_DISABLE_TELNET=ON \
	-DCURL_DISABLE_TFTP=ON \
	-DCURL_DISABLE_WEBSOCKETS=ON \
	-DCURL_ENABLE_SMB=OFF \
	-DCURL_ENABLE_NTLM=OFF \
	-DCURL_CA_BUNDLE=none \
	-DCURL_CA_PATH=/system/etc/security/cacerts

android_cmake_build "$build"
android_cmake_install "$build"

pc="$prefix_dir/lib/pkgconfig/libcurl.pc"
if [ -f "$pc" ]; then
	${SED:-sed} -i.bak 's/-l-pthread/-pthread/g' "$pc"
	rm -f "$pc.bak"
fi
