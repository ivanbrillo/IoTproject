#include "ml-prediction.h"
#include <string.h> // for memmove, memcpy
#include "power_consumption.h"

#define INT16_MAX_VAL 32767.0f

const float data_min[N_FEATURES] = {
    0.08806667f,
    0.0f,
};

const float data_range[N_FEATURES] = {
    7.788f,
    0.78913333f,
};

int16_t previous_readings_buffer[BUFFER_SIZE * N_FEATURES] = {0};

void update_readings_buffer(const int16_t new_reading[N_FEATURES])
{
    memmove(&previous_readings_buffer[N_FEATURES],
            &previous_readings_buffer[0],
            sizeof(int16_t) * N_FEATURES * (BUFFER_SIZE - 1));

    memcpy(&previous_readings_buffer[0], new_reading, sizeof(int16_t) * N_FEATURES);
}

int16_t quantize_feature(int idx, float x)
{
    float norm = (x - data_min[idx]) / data_range[idx]; // → [0,1]
    norm = norm * 2.0f - 1.0f;                          // → [-1,1]
    if (norm < -1.0f)
        norm = -1.0f;
    if (norm > +1.0f)
        norm = +1.0f;
    return (int16_t)(norm * INT16_MAX_VAL);
}

// Quantize an array of float features into int16_t
void quantize_features(const float *input, int16_t *output)
{
    for (int i = 0; i < N_FEATURES; i++)
    {
        output[i] = quantize_feature(i, input[i]);
    }
}

float predict_power(float *raw, int reading_counter)
{
    static int16_t qf[N_FEATURES];
    quantize_features(raw, qf);
    update_readings_buffer(qf);
    (void)eml_error_str; // void cast, otherwise compiler will get an error about unused variable

    if (reading_counter < BUFFER_SIZE)
    {
        // not enough data to predict
        return -1.0f;
    }

    return eml_trees_regress1(&power_consumption, previous_readings_buffer, N_FEATURES);
}
