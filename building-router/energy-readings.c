#include "ml-prediction.h"

extern float last_soc;
extern float last_battery_setpoint;

static const float fake_readings_matrix[N_SAMPLES][N_FEATURES] = {
    {2.493f, 0.10986667f},
    {1.9274f, 0.09073333f},
    {3.17073333f, 0.06773333f},
    {1.99266667f, 0.117f},
    {1.78846667f, 0.12013333f},
    {4.7648f, 0.27933333f},
    {2.65353333f, 0.1768f},
    {2.28846667f, 0.2246f},
    {1.969f, 0.22026667f},
    {1.86273333f, 0.3678f},
    {1.53846667f, 0.1946f},
    {1.78306667f, 0.14846667f},
    {2.0638f, 0.25626667f},
    {2.12146667f, 0.25853333f},
    {2.4142f, 0.102f},
    {3.5566f, 0.0752f},
    {3.34366667f, 0.0894f},
    {3.3084f, 0.09686667f},
    {3.49106667f, 0.20006667f},
    {3.32246667f, 0.1332f}};

float *get_energy_reading()
{
    static int sample_idx = -1;
    sample_idx = (sample_idx + 1) % N_SAMPLES;

    /* pick the next sample */
    float *raw = (float *)fake_readings_matrix[sample_idx];
    return raw;
}

float get_soc_reading()
{
    if (last_battery_setpoint > 0.0f && last_soc <= 99.0f)
        return last_soc + 1;
    else if (last_battery_setpoint < 0.0f && last_soc >= 1.0f)
        return last_soc - 1;
    return last_soc;
}