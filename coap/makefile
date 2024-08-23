CONTIKI_PROJECT = coap-actuator
all: $(CONTIKI_PROJECT)

# do not try to build on Sky because of code size limitation
PLATFORMS_EXCLUDE = sky z1

# include CoAP resources
MODULES_REL += ./resources

CONTIKI = ~/contiki-ng

# include the CoAP implementation
include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/coap

include $(CONTIKI)/Makefile.include
