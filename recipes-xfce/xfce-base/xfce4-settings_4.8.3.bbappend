PRINC = "1"
FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}-${PV}:"
SRC_URI += "file://minimal-settings-helper.patch"
