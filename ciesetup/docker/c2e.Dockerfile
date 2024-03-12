FROM --platform=linux/386 debian:wheezy-slim
# packages
ADD https://archive.debian.org/debian/pool/main/libx/libx11/libx11-6_1.1.5-2_i386.deb libx11-6.deb
ADD https://archive.debian.org/debian/pool/main/libx/libx11/libx11-data_1.1.5-2_all.deb libx11-data.deb
ADD https://archive.debian.org/debian/pool/main/libx/libxcb/libxcb-xlib0_1.1-1.2_i386.deb libxcb-xlib0.deb
ADD https://archive.debian.org/debian/pool/main/libx/libxcb/libxcb1_1.1-1.2_i386.deb libxcb1.deb
ADD https://archive.debian.org/debian/pool/main/libx/libxau/libxau6_1.0.1-2_i386.deb libxau6.deb
ADD https://archive.debian.org/debian/pool/main/libx/libxdmcp/libxdmcp6_1.0.3-2_i386.deb libxdmcp6.deb
ADD https://archive.debian.org/debian/pool/main/x/xorg/x11-common_7.3+20_all.deb x11-common.deb
ADD https://archive.debian.org/debian/pool/main/libx/libxext/libxext6_1.0.1-2_i386.deb libxext6.deb
RUN dpkg -i libx11-6.deb libx11-data.deb libxcb-xlib0.deb libxcb1.deb libxau6.deb libxdmcp6.deb x11-common.deb libxext6.deb
RUN rm libx11-6.deb libx11-data.deb libxcb-xlib0.deb libxcb1.deb libxau6.deb libxdmcp6.deb x11-common.deb libxext6.deb
# extract engine & set language
RUN mkdir c2e
ADD repo/pkg_engine.tar ./c2e/
RUN cp c2e/engine/language-en-GB.cfg c2e/engine/language.cfg
# "headless mode" lc2e
# settings
EXPOSE 20001
