
#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"
#include "../led_button_helper.c"

#include "sys/log.h"
#include "ml-prediction.h"

#define LOG_MODULE "ENERGY_MOD_RES"
#define LOG_LEVEL LOG_LEVEL_INFO

extern float last_prediction;
int8_t last_modality = -1; // -2: disabled, -1: to be calculated, 0: normal, 1:  light power saving, 2: heavy power saving
int8_t modality_disabled = 0;

// Response structure
typedef struct
{
  int32_t version;
  int8_t modality;
} energy_modality_response_t;

int32_t version = 0;

#define MAX_TH 3
#define LOW_TH 2

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

EVENT_RESOURCE(res_energy_modality,
               "title=\"EnergyModality\";obs",
               res_get_handler,
               NULL,
               NULL,
               NULL,
               res_event_handler);

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  energy_modality_response_t *resp = (energy_modality_response_t *)buffer;

  // Ensure we have enough buffer space
  if (preferred_size < sizeof(energy_modality_response_t))
  {
    coap_set_status_code(response, INTERNAL_SERVER_ERROR_5_00);
    LOG_ERR("Buffer Allocation Insufficient");
    return;
  }

  // Fill response structure
  resp->version = version++;
  resp->modality = (last_modality != -2) ? last_modality : 0;

  coap_set_header_content_format(response, APPLICATION_OCTET_STREAM); // binary format
  coap_set_payload(response, buffer, sizeof(energy_modality_response_t));
}

static void res_event_handler(void)
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
    mod = 2; // heavy power saving
  else if (last_prediction > LOW_TH)
    mod = 1; // light power saving

  if (last_modality != mod)
  {
    last_modality = mod;
    LOG_INFO("Energy Modality changed to %d\n", last_modality);
    set_color_led(mod);
    coap_notify_observers(&res_energy_modality);
  }
}