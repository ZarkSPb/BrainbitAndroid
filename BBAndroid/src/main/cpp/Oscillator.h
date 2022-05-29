//
// Created by User on 27.05.2022.
//

#ifndef WAVEMAKER_OSCILLATOR_H
#define WAVEMAKER_OSCILLATOR_H
#define FREQUENCYNUMBER 3

#include <atomic>
#include <stdint.h>

class Oscillator {
public:
    void setWaveOn(bool isWaveOn);
    void setSampleRate(int32_t sampleRate);
    void render(float *audioData, int32_t numFrames);
    void setAmplitude(float amplitude);

private:
    std::atomic<float> amplitude_{0.0};
    float amplitudeRender[FREQUENCYNUMBER] = {0.0, 0.0, 0.0};
    std::atomic<bool> isWaveOn_{true};
    double phase_[FREQUENCYNUMBER] = {0.0, 0.0, 0.0};
    double phaseIncrement_[FREQUENCYNUMBER] = {0.0, 0.0, 0.0};
    double frequencys_[FREQUENCYNUMBER] = {369.99, 739.99, 1479.98};
};


#endif //WAVEMAKER_OSCILLATOR_H
