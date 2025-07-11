#ifndef FLOAT_HELPER_H
#define FLOAT_HELPER_H

static inline int float_to_string(char *buffer, float number)
{
    int pos = 0;

    // Handle negative numbers
    if (number < 0)
    {
        buffer[pos++] = '-';
        number = -number;
    }

    // Round to 2 decimal places
    number += 0.005f;

    // Extract integer part
    int integer_part = (int)number;

    // Extract decimal part (multiply by 100 and take integer part)
    int decimal_part = (int)((number - integer_part) * 100);

    // Convert integer part to string
    if (integer_part == 0)
    {
        buffer[pos++] = '0';
    }
    else
    {
        // Find number of digits
        int temp = integer_part;
        int digits = 0;
        while (temp > 0)
        {
            temp /= 10;
            digits++;
        }

        // Write digits from right to left
        pos += digits;
        int write_pos = pos - 1;

        temp = integer_part;
        while (temp > 0)
        {
            buffer[write_pos--] = '0' + (temp % 10);
            temp /= 10;
        }
    }

    // Add decimal point
    buffer[pos++] = '.';

    // Add decimal digits (always 2 digits)
    buffer[pos++] = '0' + (decimal_part / 10);
    buffer[pos++] = '0' + (decimal_part % 10);

    // Null terminate
    buffer[pos] = '\0';

    return pos;
}

#endif /* FLOAT_HELPER_H */