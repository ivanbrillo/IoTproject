#include <stdint.h>

float update_temp_setpoint(float current_temp, float setpoint, float temp_required, int8_t modality)
{
    float difference = temp_required - current_temp;
    float scaling_factor = (modality == 0) ? 4.0f : (modality == 1 ? 5.0f : 8.0f);
    float new_setpoint;

    if (difference < 0.0f)
    {
        new_setpoint = setpoint + difference / scaling_factor;
    }
    else
    {
        new_setpoint = setpoint + difference / 4.0f;
    }

    // Clamp new_setpoint to be within ±2 of temp_required
    if (new_setpoint > temp_required + 2.0f)
    {
        new_setpoint = setpoint + 2.0f;
    }
    else if (new_setpoint < temp_required - 2.0f)
    {
        new_setpoint = setpoint - 2.0f;
    }

    return new_setpoint;
}

float update_window_setpoint(float current_light, float setpoint, float light_required)
{
    float new_setpoint = setpoint - (light_required - current_light) / 10.0f;

    if (new_setpoint < 0.0f)
        new_setpoint = 0.0f;
    else if (new_setpoint > 100.0f)
        new_setpoint = 100.0f;
    return new_setpoint;
}
