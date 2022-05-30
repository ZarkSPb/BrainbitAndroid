//
// Created by User on 27.05.2022.
//

#ifndef WAVEMAKER_AUDIOENGINE_H
#define WAVEMAKER_AUDIOENGINE_H

#include <aaudio/AAudio.h>
#include "Oscillator.h"

class AudioEngine {
public:
    bool start();
    void stop();
    void restart();
    void setToneOn(bool isToneOn);
    void setAmplitudes(float t1, float a1, float b1);

private:
    Oscillator oscillator_;
    AAudioStream *stream_;
};


#endif //WAVEMAKER_AUDIOENGINE_H
