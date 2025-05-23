CONTIKI_PROJECT = building-router
all: $(CONTIKI_PROJECT)
CONTIKI = ../../..

# The BR is either native or embedded, and in the latter case must support SLIP
PLATFORMS_EXCLUDE = z1

# Include RPL BR module
include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_SERVICES_DIR)/rpl-border-router
# Include webserver module
MODULES_REL += webserver
MODULES_REL += ./resources

MODULES_REL += /home/iot_ubuntu_intel/.local/lib/python3.10/site-packages/emlearn
TARGET_LIBFILES += -lm
INC += /home/iot_ubuntu_intel/.local/lib/python3.10/site-packages/emlearn

# Include the CoAP implementation
include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/coap

PROJECT_SOURCEFILES += ml-prediction.c


include $(CONTIKI)/Makefile.include
