#include "coap-engine.h"
#include "coap-blocking-api.h"

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

#define NUMBER_OF_URLS 2

char *service_urls[NUMBER_OF_URLS] =
    {"AC/setpoint", "Window/setpoint"};

/* This function is will be passed to COAP_BLOCKING_REQUEST() to handle responses. */
void client_chunk_handler(coap_message_t *response)
{
  const uint8_t *chunk;

  if (response == NULL)
  {
    puts("Request timed out \n");
    return;
  }
  int len = coap_get_payload(response, &chunk);
  LOG_INFO("%.*s \n", len, (char *)chunk);
}

/*----------------------------------------------------------------------------*/