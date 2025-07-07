#include "contiki.h"
#include "dev/etc/rgb-led/rgb-led.h"  // for rgb_led_set(), rgb_led_off()
#include "sys/etimer.h"

#define DISABLE       -1
#define COLOR_GREEN    0
#define COLOR_BLUE     1
#define COLOR_RED      2

void set_color_led(int color) {
  rgb_led_off();

  switch (color) {
    case COLOR_GREEN:
      rgb_led_set(RGB_LED_GREEN);
      break;
    case COLOR_BLUE:
      rgb_led_set(RGB_LED_BLUE);
      break;
    case COLOR_RED:
      rgb_led_set(RGB_LED_RED);
      break;
    case DISABLE:
      rgb_led_off();
      break;
    default:
      break;
  }
}
