# XenClient dom0 image

include xenclient-image-common.inc
IMAGE_FEATURES += "package-management"

COMPATIBLE_MACHINE = "(openxt-dom0)"
IMAGE_INITSCRIPTS = "xenclient-dom0-initscripts"

IMAGE_FSTYPES = "xc.ext3.gz"

# No thanks, we provide our own xorg.conf with the hacked Intel driver
# And we don't need Avahi
BAD_RECOMMENDATIONS += "xserver-xorg avahi-daemon avahi-autoipd"
# The above seems to be broken and we *really* don't want avahi!
PACKAGE_REMOVE = "avahi-daemon avahi-autoipd"

ANGSTROM_EXTRA_INSTALL += " \
			  " 
export IMAGE_BASENAME = "xenclient-dom0-image"
export STAGING_KERNEL_DIR

DEPENDS = "packagegroup-base packagegroup-xenclient-dom0 packagegroup-xenclient-dom0-extra"
IMAGE_INSTALL = "\
    ${ROOTFS_PKGMANAGE} \
    ${IMAGE_INITSCRIPTS} \
    modules \
    packagegroup-core-boot \
    packagegroup-base \
    packagegroup-xenclient-common \
    packagegroup-xenclient-dom0 \
    v4v-module \
    xenclient-preload-hs-libs \
    ${ANGSTROM_EXTRA_INSTALL}"

# IMAGE_PREPROCESS_COMMAND = "create_etc_timestamp"

process_password_stuff() {
	#zap root password for release images
	${@base_conditional("DISTRO_TYPE", "release", "zap_root_password; ", "",d)}

	# zap root password in shadow
	sed -i 's%^root:[^:]*:%root:*:%' ${IMAGE_ROOTFS}/etc/shadow;

	sed -i 's|root:x:0:0:root:/home/root:/bin/sh|root:x:0:0:root:/root:/bin/bash|' ${IMAGE_ROOTFS}/etc/passwd;
}

# Use version of files in volatile space
redirect_files() {
	mkdir -p ${IMAGE_ROOTFS}/config/etc;
	mv ${IMAGE_ROOTFS}/etc/passwd ${IMAGE_ROOTFS}/config/etc;
	mv ${IMAGE_ROOTFS}/etc/shadow ${IMAGE_ROOTFS}/config/etc;
	ln -s ../config/etc/passwd ${IMAGE_ROOTFS}/etc/passwd;
	ln -s ../config/etc/shadow ${IMAGE_ROOTFS}/etc/shadow;
	ln -s ../config/etc/.pwd.lock ${IMAGE_ROOTFS}/etc/.pwd.lock;
	ln -s ../var/volatile/etc/asound ${IMAGE_ROOTFS}/etc/asound;

	rm ${IMAGE_ROOTFS}/etc/hosts; ln -s /var/run/hosts ${IMAGE_ROOTFS}/etc/hosts;
	ln -s /var/volatile/etc/resolv.conf ${IMAGE_ROOTFS}/etc/resolv.conf;

	# Write coredumps in /var/cores
	echo 'kernel.core_pattern = /var/cores/%e-%t.%p.core' >> ${IMAGE_ROOTFS}/etc/sysctl.conf ;
}

grab_initramfs() {
	cat ${DEPLOY_DIR_IMAGE}/openxt-initramfs-image-openxt-dom0.cpio.gz > ${IMAGE_ROOTFS}/boot/initramfs.gz ;
}

set_ratelimit() {
	echo 'kernel.printk_ratelimit = 0' >> ${IMAGE_ROOTFS}/etc/sysctl.conf;
}

set_consoles() {
	# Add dom0 console getty
	sed -i 's|1:2345:respawn:/sbin/getty 38400 tty1|#1:2345:respawn:/sbin/getty 38400 tty1|' ${IMAGE_ROOTFS}/etc/inittab ;

	echo '1:2345:respawn:/sbin/getty 38400 tty1' >> ${IMAGE_ROOTFS}/etc/inittab ;
}

create_mounts() {
	# Create mountpoint for /mnt/secure
	mkdir -p ${IMAGE_ROOTFS}/mnt/secure ;

	# Create mountpoint for /mnt/upgrade
	mkdir -p ${IMAGE_ROOTFS}/mnt/upgrade ;

	# Create mountpoint for boot/system
	mkdir -p ${IMAGE_ROOTFS}/boot/system ;
}

remove_unwanted_packages() {
	opkg-cl -f ${IPKGCONF_TARGET} -o ${IMAGE_ROOTFS} ${OPKG_ARGS} -force-depends remove ${PACKAGE_REMOVE};
}

# Remove network modules except netfront
remove_excess_modules() {
	for x in `find ${IMAGE_ROOTFS}/lib/modules -name *.ko | grep drivers/net | grep -v xen-netfront`; do
		pkg="kernel-module-`basename $x .ko | sed s/_/-/g`";
		opkg-cl ${IPKG_ARGS} -force-depends remove $pkg;
	done;
}

### Stubdomain stuff - temporary
STUBDOMAIN_DEPLOY_DIR_IMAGE = "${DEPLOY_DIR}/images/openxt-stubdomain"
STUBDOMAIN_IMAGE = "${STUBDOMAIN_DEPLOY_DIR_IMAGE}/xenclient-stubdomain-initramfs-image-openxt-stubdomain.cpio.gz"
STUBDOMAIN_KERNEL = "${STUBDOMAIN_DEPLOY_DIR_IMAGE}/bzImage-openxt-stubdomain.bin"
process_tmp_stubdomain_items() {
	mkdir -p ${IMAGE_ROOTFS}/usr/lib/xen/boot ;
	cat ${STUBDOMAIN_IMAGE} > ${IMAGE_ROOTFS}/usr/lib/xen/boot/stubdomain-initramfs ;
	cat ${STUBDOMAIN_KERNEL} > ${IMAGE_ROOTFS}/usr/lib/xen/boot/stubdomain-bzImage ; 
}

ROOTFS_POSTPROCESS_COMMAND += " process_password_stuff; redirect_files; grab_initramfs; set_ratelimit; remove_unwanted_packages; remove_excess_modules; process_tmp_stubdomain_items; "

inherit selinux-image
#inherit validate-package-versions
inherit xenclient-image-src-info
inherit xenclient-image-src-package
inherit xenclient-licences
require xenclient-version.inc

LICENSE = "GPLv2 & MIT"
LIC_FILES_CHKSUM = "file://${TOPDIR}/COPYING.GPLv2;md5=751419260aa954499f7abaabaa882bbe      \
                    file://${TOPDIR}/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
