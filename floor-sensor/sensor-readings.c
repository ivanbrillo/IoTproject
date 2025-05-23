#include <stdlib.h>

extern float last_ac_setpoint;     // temperature setpoint in Celsius
extern float last_window_setpoint; // window cover percentage (0–100)
extern float last_light;           // in lumen
extern float last_temperature;     // in Celsius

// Internal previous values to detect trends
static float previous_ac_setpoint = 22.0f;
static float previous_window_setpoint = 50.0f;

// Clamp function
float clamp(float value, float min, float max)
{
    if (value < min)
        return min;
    if (value > max)
        return max;
    return value;
}

// Simple biased random generator
float biased_random(float bias, float max_change)
{
    // Random value in range [-0.5, 0.5]
    float r = ((float)(rand() % 1001) / 1000.0f) - 0.5f;

    // Apply bias (-1 to 1) scaled by max_change
    float result = r * max_change + bias * (max_change / 2.0f);
    return result;
}

float read_temperature()
{
    // Compute simple bias: if setpoint decreased, bias toward negative
    float setpoint_diff = last_ac_setpoint - previous_ac_setpoint;

    float bias = 0.0f;
    if (setpoint_diff > 0.1f)
    {
        bias = 0.5f; // AC increased: temperature may go up
    }
    else if (setpoint_diff < -0.1f)
    {
        bias = -0.5f; // AC lowered: temperature may go down
    }

    float delta = biased_random(bias, 0.8f); // max change ±0.8°C
    float new_temperature = clamp(last_temperature + delta, -20.0f, 200.0f);

    previous_ac_setpoint = last_ac_setpoint;
    return new_temperature;
}

float read_light()
{
    // Compute simple bias: if covers closed more, light decreases
    float cover_diff = last_window_setpoint - previous_window_setpoint;

    float bias = 0.0f;
    if (cover_diff > 2.0f)
    {
        bias = -0.5f; // Closed more → darker
    }
    else if (cover_diff < -2.0f)
    {
        bias = 0.5f; // Opened more → brighter
    }

    float delta = biased_random(bias, 40.0f); // max change ±40 lumens
    float new_light = clamp(last_light + delta, 0.0f, 1000.0f);

    previous_window_setpoint = last_window_setpoint;
    return new_light;
}
