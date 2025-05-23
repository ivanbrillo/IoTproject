#ifndef ML_PREDICTION_H
#define ML_PREDICTION_H

#include <stdint.h>

#define N_FEATURES 2
#define N_SAMPLES 20
#define BUFFER_SIZE 5

float predict_power(float *raw, int reading_counter);


#endif // ML_PREDICTION_H
