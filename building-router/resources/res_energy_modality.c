
#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"
#include "../led_button_helper.c"

#include "sys/log.h"
#include "ml-prediction.h"

#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

extern float last_prediction;
int8_t last_modality = -1; // -2: disabled, -1: to be calculated, 0: normal, 1:  light power saving, 2: heavy power saving
int8_t modality_disabled = 0;

#define MAX_TH 3
#define LOW_TH 2

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

/*
 * Example for an event resource.
 * Additionally takes a period parameter that defines the interval to call [name]_periodic_handler().
 * A default post_handler takes care of subscriptions and manages a list of subscribers to notify.
 */
EVENT_RESOURCE(res_energy_modality,
               "title=\"EnergyModality\";obs",
               res_get_handler,
               NULL,
               NULL,
               NULL,
               res_event_handler);

static void
res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  // coap_set_header_content_format(response, TEXT_PLAIN);

  uint8_t modality = 0;
  if (last_modality != -2)
    modality = last_modality;

  coap_set_header_content_format(response, APPLICATION_OCTET_STREAM); // binary format
  buffer[0] = modality;
  coap_set_payload(response, buffer, 1); // one byte payload
  //coap_set_payload(response, buffer, snprintf((char *)buffer, preferred_size, "energy modality: %d", modality));

  /* A post_handler that handles subscriptions/observing will be called for periodic resources by the framework. */
}
/*
 * Additionally, res_event_handler must be implemented for each EVENT_RESOURCE.
 * It is called through <res_name>.trigger(), usually from the server process.
 */
static void
res_event_handler(void)
{
  if (modality_disabled == 1)
  {
    if (last_modality != -2)
    {
      // Modality just got disabled
      LOG_WARN("Energy Modality disabled\n");
      set_color_led(-1); // Turn off LED

      if (last_modality != 0)
      {
        last_modality = -2;                          // Set to disabled
        coap_notify_observers(&res_energy_modality); // Notify only if last modality wasn't 0
      }
      else
      {
        last_modality = -2; // Set to disabled
      }
    }
    return; // Skip rest while disabled
  }

  // Modality just got re-enabled
  if (last_modality == -2)
  {
    LOG_INFO("Energy Modality enabled\n");
    set_color_led(0); // Temporarily assume 0 until computed below
    // Don't notify yet — wait to compute actual modality
  }

  // Compute modality based on prediction
  uint8_t mod = 0;

  if (last_prediction > MAX_TH)
  {
    mod = 2; // heavy power saving
  }
  else if (last_prediction > LOW_TH)
  {
    mod = 1; // light power saving
  }

  if (last_modality != mod)
  {
    last_modality = mod;
    LOG_INFO("Energy Modality changed to %d\n", last_modality);
    set_color_led(mod);
    coap_notify_observers(&res_energy_modality);
  }
}