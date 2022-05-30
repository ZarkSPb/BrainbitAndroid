//
// Created by User on 27.05.2022.
//

#ifndef WAVEMAKER_OSCILLATOR_H
#define WAVEMAKER_OSCILLATOR_H
#define FREQUENCYNUMBER 12

#include <atomic>
#include <stdint.h>

class Oscillator {
public:
    void setWaveOn(bool isWaveOn);
    void setSampleRate(int32_t sampleRate);
    void render(float *audioData, int32_t numFrames);
    void setAmplitudes(float t1, float a1, float b1);

private:
    std::atomic<float> t1_{0.0};
    std::atomic<float> a1_{0.0};
    std::atomic<float> b1_{0.0};
    std::atomic<bool> isWaveOn_{true};
    float amplitudeRender[FREQUENCYNUMBER] = {};
    double phase_[FREQUENCYNUMBER] = {};
    double phaseIncrement_[FREQUENCYNUMBER] = {};
    double frequencys_[FREQUENCYNUMBER] = {277.18, 554.36, 1108.73, 2217.4,
                                           369.99, 739.99, 1479.98, 3332.4,
                                           466.3, 932.33, 1864.66, 3729.2};
};


#endif //WAVEMAKER_OSCILLATOR_H
