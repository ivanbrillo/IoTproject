#include "contiki.h"
#include <stdint.h>
#include "coap-engine.h"
#include "sys/log.h"
#include "ml-prediction.h"
#include "energy-readings.c"
#include "os/dev/button-hal.h"
#include <locale.h>

#define LOG_MODULE "BUILDING_ROUTER"
#define LOG_LEVEL LOG_LEVEL_INFO

extern coap_resource_t res_power;
extern coap_resource_t res_battery_setpoint;
extern coap_resource_t res_battery_soc;
extern coap_resource_t res_energy_modality;

extern float last_prediction;
extern float last_reading[N_FEATURES];
extern float current_soc;

extern int8_t modality_disabled;

PROCESS(udp_server_process, "UDP server");
AUTOSTART_PROCESSES(&udp_server_process);

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_server_process, ev, data)
{
  static struct etimer power_timer;
  static struct etimer soc_timer;
  PROCESS_BEGIN();

  setlocale(LC_NUMERIC, "C");

#if BORDER_ROUTER_CONF_WEBSERVER
  PROCESS_NAME(webserver_nogui_process);
  rocess_start(&webserver_nogui_process, NULL);
#endif /* BORDER_ROUTER_CONF_WEBSERVER */

  coap_activate_resource(&res_power, "power");
  coap_activate_resource(&res_battery_soc, "battery/soc");
  coap_activate_resource(&res_battery_setpoint, "battery/setpoint");
  coap_activate_resource(&res_energy_modality, "energy-modality");
  LOG_INFO("Server initialized\n");

  /* Initialize timers */
  etimer_set(&power_timer, CLOCK_SECOND * 10); // for power and modality
  etimer_set(&soc_timer, CLOCK_SECOND * 15);   // for battery soc
  static int reading_counter = 0;

  while (1)
  {
    PROCESS_WAIT_EVENT();

    if (ev == button_hal_press_event)
    {
      modality_disabled = (modality_disabled == 0) ? 1 : 0; // toggle modality_disabled
    }
    else if (ev == PROCESS_EVENT_TIMER)
    {
      if (data == &power_timer)
      {
        float *raw = get_energy_reading();

        memcpy(last_reading, raw, N_FEATURES * sizeof(float));
        last_prediction = predict_power(raw, ++reading_counter);

        LOG_INFO("pred %.3f\n", last_prediction);

        res_power.trigger();
        res_energy_modality.trigger();

        etimer_reset(&power_timer);
      }
      else if (data == &soc_timer)
      {
        current_soc = get_soc_reading();

        res_battery_soc.trigger();

        etimer_reset(&soc_timer);
      }
    }
  }

  PROCESS_END();
}

/*---------------------------------------------------------------------------*/
