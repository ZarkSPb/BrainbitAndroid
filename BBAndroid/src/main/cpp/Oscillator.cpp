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
    if(!isWaveOn) {
        t1_.store(0.0);
        a1_.store(0.0);
        b1_.store(0.0);
        for (int i = 0; i < FREQUENCYNUMBER; i++) {
            amplitudeRender[i] = 0.0;
            phase_[i] = 0.0;
        }
    }
}

void Oscillator::setAmplitudes(float t1, float a1, float b1) {
    t1_.store(t1);
    a1_.store(a1);
    b1_.store(b1);
}

void Oscillator::render(float *audioData, int32_t numFrames) {
    for (int i = 0; i < numFrames; i++) {
        if (isWaveOn_.load()) {
            // Calculates the next sample value for the sine wave.
            audioData[i] = (float) ((sin(phase_[0]) * amplitudeRender[0]) +
                                    (sin(phase_[1]) * amplitudeRender[1]) / 4.0f +
                                    (sin(phase_[2]) * amplitudeRender[2]) / 6.0f +
                                    (sin(phase_[3]) * amplitudeRender[3]) / 8.0f +

                                    (sin(phase_[4]) * amplitudeRender[4]) +
                                    (sin(phase_[5]) * amplitudeRender[5]) / 4.0f +
                                    (sin(phase_[6]) * amplitudeRender[6]) / 6.0f +
                                    (sin(phase_[7]) * amplitudeRender[7]) / 8.0f +

                                    (sin(phase_[8]) * amplitudeRender[8]) +
                                    (sin(phase_[9]) * amplitudeRender[9]) / 4.0f +
                                    (sin(phase_[10]) * amplitudeRender[10]) / 6.0f +
                                    (sin(phase_[11]) * amplitudeRender[11]) / 8.0f) / 12.0f;

            // Increments the phase, handling wrap around.
            // 0 - 3
            for (int freqN = 0; freqN < 4; freqN++) {
                phase_[freqN] += phaseIncrement_[freqN];
                if (phase_[freqN] > TWO_PI) {
                    phase_[freqN] -= TWO_PI;
                    if (amplitudeRender[freqN] != t1_.load())
                        amplitudeRender[freqN] += (t1_.load() - amplitudeRender[freqN]) / 4.0f;
                }
            }

            // 4 - 7
            for (int freqN = 4; freqN < 8; freqN++) {
                phase_[freqN] += phaseIncrement_[freqN];
                if (phase_[freqN] > TWO_PI) {
                    phase_[freqN] -= TWO_PI;
                    if (amplitudeRender[freqN] != a1_.load())
                        amplitudeRender[freqN] += (a1_.load() - amplitudeRender[freqN]) / 4.0f;
                }
            }

            // 7 - 11
            for (int freqN = 8; freqN < FREQUENCYNUMBER; freqN++) {
                phase_[freqN] += phaseIncrement_[freqN];
                if (phase_[freqN] > TWO_PI) {
                    phase_[freqN] -= TWO_PI;
                    if (amplitudeRender[freqN] != b1_.load())
                        amplitudeRender[freqN] += (b1_.load() - amplitudeRender[freqN]) / 4.0f;
                }
            }

        } else {
            // Outputs silence by setting sample value to zero.
            audioData[i] = 0;
        }
    }
}