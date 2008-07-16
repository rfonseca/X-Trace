mod_xtrace.la: mod_xtrace.slo
	$(SH_LINK) -rpath $(libexecdir) -module -avoid-version  mod_xtrace.lo
DISTCLEAN_TARGETS = modules.mk
shared =  mod_xtrace.la
