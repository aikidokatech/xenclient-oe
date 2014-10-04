DESCRIPTION = "GTK switcher bar"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=4641e94ec96f98fabc56ff9cc48be14b"
DEPENDS = "xenclient-idl xenclient-rpcgen-native dbus libxcdbus libglade libnotify xen-tools"
RDEPENDS_${PN} = "notification-daemon"

SRC_URI = "${OPENXT_GIT_MIRROR}/xctools.git;protocol=git;tag=${OPENXT_TAG}"

EXTRA_OECONF += "--with-idldir=${STAGING_IDLDIR}"

S = "${WORKDIR}/git/gtk-switcher"

inherit autotools
inherit xenclient

do_install_append() {
	install -d ${D}/usr/bin
	install -m 0755 ${S}/src/gtk-switcher-run ${D}/usr/bin/gtk-switcher-run
}
