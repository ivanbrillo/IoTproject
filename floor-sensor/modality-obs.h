#ifndef ENERGY_OBSERVER_H
#define ENERGY_OBSERVER_H

#include "coap-engine.h"


/* URI of the observed resource */
#define OBS_RESOURCE_URI "energy-modality"
extern uint8_t modality;

void toggle_observation(coap_endpoint_t* server_ep);


#endif /* ENERGY_OBSERVER_H */
