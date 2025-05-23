
float update_temp_setpoint(float current_temp, float setpoint, float temp_required, uint8_t modality)
{
    float difference = temp_required - current_temp;

    if (difference < 0.0f)
    {
        float scaling_factor = modality == 0 ? 4.0f : modality == 1 ? 5.0f
                                                                    : 8.0f;

        // If the temperature is too high, decrease the setpoint -> increase energy consumption
        return setpoint + difference / scaling_factor;
    }

    return setpoint + (temp_required - current_temp) / 4.0f;
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
