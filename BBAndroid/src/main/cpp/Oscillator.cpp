//
// Created by User on 27.05.2022.
//

#include "Oscillator.h"
#include <math.h>
#include <android/log.h>

#define TWO_PI (3.14159 * 2)

void Oscillator::setSampleRate(int32_t sampleRate) {
    double coefficient = TWO_PI / (double) sampleRate;
    for (int i = 0; i < FREQUENCYNUMBER; i++)
        phaseIncrement_[i] = coefficient * (double) frequencys_[i];
}

void Oscillator::setWaveOn(bool isWaveOn) {
    isWaveOn_.store(isWaveOn);
}

void Oscillator::setAmplitude(float amplitude) {
    amplitude_.store(amplitude);
}

void Oscillator::render(float *audioData, int32_t numFrames) {

//    if (!isWaveOn_.load()) phase_ = 0;

    for (int i = 0; i < numFrames; i++) {

        if (isWaveOn_.load()) {

            // Calculates the next sample value for the sine wave.
            audioData[i] = (float) ((sin(phase_[0]) * amplitudeRender[0]) +
                                    (sin(phase_[1]) * amplitudeRender[1]) / 4.0f +
                                    (sin(phase_[2]) * amplitudeRender[2]) / 8.0f) / 3.0f;

            // Increments the phase, handling wrap around.
            for (int freqN = 0; freqN < FREQUENCYNUMBER; freqN++) {
                phase_[freqN] += phaseIncrement_[freqN];

                if (phase_[freqN] > TWO_PI) {
//                __android_log_print(ANDROID_LOG_DEBUG, "Oscillator", "%f", phase_);
                    phase_[freqN] -= TWO_PI;
                    if (amplitudeRender[freqN] != amplitude_.load())
                        amplitudeRender[freqN] += (amplitude_.load() - amplitudeRender[freqN]) / 4.0f;
                }
            }

        } else {
            // Outputs silence by setting sample value to zero.
            audioData[i] = 0;
        }
    }
}