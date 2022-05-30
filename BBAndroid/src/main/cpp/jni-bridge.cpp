#include <jni.h>
#include <android/input.h>
#include "AudioEngine.h"

static AudioEngine *audioEngine = new AudioEngine();

extern "C" {

JNIEXPORT void JNICALL
Java_com_zark_bbandroid_brainbitandroid_MainActivity_setToneOn(JNIEnv *env, jobject obj,
                                                               jboolean toneOn) {
    audioEngine->setToneOn(toneOn);

}

JNIEXPORT void JNICALL
Java_com_zark_bbandroid_brainbitandroid_MainActivity_startEngine(JNIEnv *env, jobject /* this */) {
    audioEngine->start();
}

JNIEXPORT void JNICALL
Java_com_zark_bbandroid_brainbitandroid_MainActivity_stopEngine(JNIEnv *env, jobject /* this */) {
    audioEngine->stop();
}

JNIEXPORT void JNICALL
Java_com_zark_bbandroid_brainbitandroid_Signal_setAmplitudes(JNIEnv *env, jobject obj, jfloat t1,
                                                             jfloat a1, jfloat b1) {
    audioEngine->setAmplitudes(t1, a1, b1);
}

}