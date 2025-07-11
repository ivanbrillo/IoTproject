#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include <locale.h>

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "FLOOR_ACTUATOR"
#define LOG_LEVEL LOG_LEVEL_APP

/*
 * Resources to be activated need to be imported through the extern keyword.
 * The build system automatically compiles the resources in the corresponding sub-directory.
 */
extern coap_resource_t res_ac_setpoint, res_window_setpoint;

PROCESS(er_example_server, "Actuators Floor Coap Server");
AUTOSTART_PROCESSES(&er_example_server);

PROCESS_THREAD(er_example_server, ev, data)
{
  PROCESS_BEGIN();
  setlocale(LC_NUMERIC, "C");

  PROCESS_PAUSE();

  LOG_INFO("Starting Actuators Floor CoAP Server\n");

  coap_activate_resource(&res_ac_setpoint, "AC/setpoint");
  coap_activate_resource(&res_window_setpoint, "Window/setpoint");

  while (1) // wait until a new event
  {
    PROCESS_YIELD();
  }

  PROCESS_END();
}
