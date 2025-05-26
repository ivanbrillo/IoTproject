
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "coap-log.h"
#include "modality-obs.h"
#include "../led_button_helper.c"

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

static coap_observee_t *obs;
#define OBS_RESOURCE_URI "energy-modality"

uint8_t modality = 0;

/*----------------------------------------------------------------------------*/
/*
 * Handle the response to the observe request and the following notifications
 */
static void
notification_callback(coap_observee_t *obs, void *notification,
                      coap_notification_flag_t flag)
{
  int len = 0;
  const uint8_t *payload = NULL;

  LOG_INFO("Observee URI: %s\n", obs->url);
  if (notification)
  {
    len = coap_get_payload(notification, &payload);
  }

  switch (flag)
  {
  case NOTIFICATION_OK:
    if (payload != NULL && len >= 1)
    {
      uint8_t mod = payload[0];

      if (mod <= 2)
      {
        LOG_INFO("Modality PARSED: %u\n", mod);
        modality = mod;
        set_color_led(modality);
      }
      else
      {
        LOG_INFO("Unknown modality value: %u\n", modality);
      }
    }
    else
    {
      LOG_INFO("No payload received to parse modality\n");
    }
    break;

  case OBSERVE_OK:
    LOG_INFO("OBSERVE_OK: %*s\n", len, (char *)payload);
    break;

  case OBSERVE_NOT_SUPPORTED:
    LOG_INFO("OBSERVE_NOT_SUPPORTED: %*s\n", len, (char *)payload);
    obs = NULL;
    break;

  case ERROR_RESPONSE_CODE:
    LOG_INFO("ERROR_RESPONSE_CODE: %*s\n", len, (char *)payload);
    obs = NULL;
    break;

  case NO_REPLY_FROM_SERVER:
    LOG_INFO("NO_REPLY_FROM_SERVER: removing observe registration with token %x%x\n",
             obs->token[0], obs->token[1]);
    obs = NULL;
    break;
  }
}

void toggle_observation(coap_endpoint_t *server_ep)
{
  if (obs)
  {
    LOG_INFO("Stopping observation\n");
    coap_obs_remove_observee(obs);
    obs = NULL;
  }
  else
  {
    LOG_INFO("Starting observation\n");
    obs = coap_obs_request_registration(server_ep, OBS_RESOURCE_URI, notification_callback, NULL);
  }
}