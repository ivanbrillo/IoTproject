CONTIKI_PROJECT = floor
# coap-example-observe-client is outdated but will be ported at a later point
all: $(CONTIKI_PROJECT)

# Do not try to build on Sky because of code size limitation
PLATFORMS_EXCLUDE = sky z1

CONTIKI=../../..

MODULES_REL += ./res


# Include the CoAP implementation
include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/coap
PROJECT_SOURCEFILES += modality-obs.c

include $(CONTIKI)/Makefile.include
