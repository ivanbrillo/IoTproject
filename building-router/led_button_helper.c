#include "os/dev/leds.h"

#define DISABLE  -1
#define COLOR_GREEN  0
#define COLOR_YELLOW 1
#define COLOR_RED    2

void set_color_led(int color) {
  leds_off(LEDS_ALL); // Turn off all LEDs first

  switch(color) {
    case COLOR_GREEN:
      leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
      break;
    case COLOR_YELLOW:
      leds_set(LEDS_NUM_TO_MASK(LEDS_YELLOW));
      break;
    case COLOR_RED:
      leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
      break;
      case DISABLE:
      leds_off(LEDS_ALL);
      break;
    default:
      // Unknown color: no LED or handle error
      break;
  }
}