#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "contiki-net.h"
#include "coap-engine.h"
#include "coap-blocking-api.h"
#include "modality-obs.h"
#include "coap-helper.c"
#include "sensor-readings.c"
#include "coap-log.h"
#include "setpoints-calculator.c"
#include <locale.h>
#include <math.h>

#define LOG_MODULE "FLOOR_SENSOR"
#define LOG_LEVEL LOG_LEVEL_APP

/* FIXME: This server address is hard-coded for Cooja and link-local for unconnected border router. */
#define SERVER_EP "coap://[fe80::203:3:3:3]"
#define SERVER_EP2 "coap://[fe80::201:1:1:1]"

#define TOGGLE_INTERVAL 10

#define INITIAL_WINDOW 50.0f
#define INITIAL_AC 21.0f

// Helper lambda/function to check >2% change (absolute relative difference)
#define CHANGE_ABOVE_2_PERCENT(old, new) (fabsf((new) - (old)) / ((old) != 0 ? fabsf(old) : 1.0f) > 0.02f)

PROCESS(er_example_client, "FLOOR SENSOR CLIENT");
AUTOSTART_PROCESSES(&er_example_client);

static struct etimer et;
static coap_endpoint_t server_ep;
static coap_endpoint_t server_ep2;

float last_ac_setpoint = INITIAL_AC;
float last_window_setpoint = INITIAL_WINDOW;

extern float current_temperature;
extern float current_light;

extern float temperature_required;
extern float light_required;
extern int8_t modality;

extern coap_resource_t res_light_required, res_temp_required, res_sensors, res_dynamic_control;

extern int dynamic_control;

PROCESS_THREAD(er_example_client, ev, data)
{
  PROCESS_BEGIN();

  static coap_message_t request[1];
  setlocale(LC_NUMERIC, "C");

  coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);
  coap_endpoint_parse(SERVER_EP2, strlen(SERVER_EP2), &server_ep2);
  coap_activate_resource(&res_light_required, "LIGHT/setpoint");
  coap_activate_resource(&res_temp_required, "TEMP/setpoint");
  coap_activate_resource(&res_sensors, "SENSORS/reading");
  coap_activate_resource(&res_dynamic_control, "dynamic-control");

  etimer_set(&et, TOGGLE_INTERVAL * CLOCK_SECOND);

  toggle_observation(&server_ep2);

  while (1)
  {
    PROCESS_YIELD();

    if (etimer_expired(&et))
    {
      char query_buffer[64];

      current_temperature = read_temperature();
      current_light = read_light();

      if (dynamic_control == 1)
      {

        // Calculate new setpoints
        float new_ac_setpoint = update_temp_setpoint(current_temperature, last_ac_setpoint, temperature_required, modality);
        float new_window_setpoint = update_window_setpoint(current_light, last_window_setpoint, light_required);

        // --- AC control request ---
        if (CHANGE_ABOVE_2_PERCENT(last_ac_setpoint, new_ac_setpoint))
        {
          LOG_WARN("new ac setpoint: %.3f", new_ac_setpoint);
          last_ac_setpoint = new_ac_setpoint; // update only on send

          coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
          coap_set_header_uri_path(request, service_urls[0]);
          snprintf(query_buffer, sizeof(query_buffer), "on=1&setpoint=%.2f", new_ac_setpoint);
          coap_set_header_uri_query(request, query_buffer);
          LOG_INFO_COAP_EP(&server_ep);
          COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);
        }

        // --- Window control request ---
        if (CHANGE_ABOVE_2_PERCENT(last_window_setpoint, new_window_setpoint))
        {
          last_window_setpoint = new_window_setpoint; // update only on send

          coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
          coap_set_header_uri_path(request, service_urls[1]);
          snprintf(query_buffer, sizeof(query_buffer), "setpoint=%.2f", new_window_setpoint);
          coap_set_header_uri_query(request, query_buffer);
          LOG_INFO_COAP_EP(&server_ep);
          COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);
        }
      }

      LOG_INFO("Temp: %.2fC | AC Setpoint: %.2fC | Temp target: %.2f\n",
               current_temperature, last_ac_setpoint, temperature_required);
      LOG_INFO("Light: %.2f lm | Window Setpoint: %.2f | Light target: %.2f lm\n",
               current_light, last_window_setpoint, light_required);

      res_sensors.trigger();
      etimer_reset(&et);
    }
  }

  PROCESS_END();
}
