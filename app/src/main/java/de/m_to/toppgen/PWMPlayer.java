/*
 * The Clear BSD License
 *
 * Copyright (c) 2016, Tobias Modschiedler (mail@m-to.de)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted (subject to the limitations in the disclaimer
 * below) provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of its contributors may be used
 *   to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package de.m_to.toppgen;

import android.app.Fragment;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;


public class PWMPlayer extends Fragment {

    public static final int PeriodLengthMs = 20;
    private static final int MinPulseWidthUs = 1000;
    private static final int MaxPulseWidthUs = 2000;

    private static final int sampleRate = 48000;
    private static final int samplesPerPeriod = PeriodLengthMs * sampleRate / 1000;
    private static final int bufferPeriods = 10;

    // parameters shared with thread
    private volatile float limitPulseWidthFactor = 1.0f;
    private volatile float pulseWidthFactor = 1.0f;
    private volatile int impulseLengthMS = 0;
    private volatile int impulseDelayMS = 0;
    private volatile int impulseRiseMS = 0;
    private volatile int impulseFallMS = 0;
    private volatile boolean stopping;

    // variables used only by thread
    private int currentPeriodTimeMs;

    private final byte[] sampleBuffer;
    private final AudioTrack audioTrack;

    public PWMPlayer() {

        // Retain Fragment across Activity re-creation (such as from a configuration change)
        // because our Thread can still be running.
        setRetainInstance(true);

        sampleBuffer = new byte[samplesPerPeriod];
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT,
                samplesPerPeriod * bufferPeriods, AudioTrack.MODE_STREAM);
    }

    public int getImpulseFallMS() {
        return impulseFallMS;
    }

    public void setImpulseFallMS(int impulseFallMS) {
        this.impulseFallMS = impulseFallMS;
    }

    public int getImpulseRiseMS() {
        return impulseRiseMS;
    }

    public void setImpulseRiseMS(int impulseRiseMS) {
        this.impulseRiseMS = impulseRiseMS;
    }

    public float getLimitPulseWidthFactor() {
        return limitPulseWidthFactor;
    }

    public void setLimitPulseWidthFactor(float v) {
        this.limitPulseWidthFactor = v;
    }

    public void setPulseWidthFactor(float v) {
        pulseWidthFactor = v;
    }

    public float getPulseWidthFactor() {
        return pulseWidthFactor;
    }

    public void setImpulseLengthMS(int length) {
        if (length < 0) {
            this.impulseLengthMS = 0;
        } else {
            this.impulseLengthMS = length - length % PeriodLengthMs;
        }
    }

    public int getImpulseLengthMS() {
        return impulseLengthMS;
    }

    public int getImpulseDelayMS() {
        return impulseDelayMS;
    }

    public void setImpulseDelayMS(int impulseDelayMS) {
        if (impulseDelayMS < 0) {
            this.impulseDelayMS = 0;
        } else {
            this.impulseDelayMS = impulseDelayMS - impulseDelayMS % PeriodLengthMs;
        }
    }

    /** Determine pulse width for current period based on current impulse time.
     *
     */
    private float decideCurrentPulseWidth() {
        float factor;

        if (impulseLengthMS == 0) {
            // impulse unset
            // full power
            factor = 1.0f;
        } else if (currentPeriodTimeMs < impulseLengthMS) {
            // impulse phase
            if (currentPeriodTimeMs < impulseRiseMS) {
                // slope rise
                int currentRiseTimeMS = currentPeriodTimeMs - 0;
                factor =  (float)currentRiseTimeMS / impulseRiseMS;
            } else if (currentPeriodTimeMs < impulseLengthMS-impulseFallMS) {
                // full power
                factor = 1.0f;
            } else {
                // slope fall
                int currentFallTimeMS = currentPeriodTimeMs - (impulseLengthMS - impulseFallMS);
                factor = 1.0f - (float)currentFallTimeMS / impulseFallMS;
            }
        } else {
            // delay phase
            // no power
            factor = 0.0f;
        }

        // advance current impulse time
        currentPeriodTimeMs += PeriodLengthMs;
        if (currentPeriodTimeMs >= impulseLengthMS + impulseDelayMS) {
            currentPeriodTimeMs = 0;
        }

        return factor * pulseWidthFactor * limitPulseWidthFactor;
    }

    private void fillBuffer() {

        float pulseWidthFactor;
        int pulseWidthUs;
        int pulseWidthSamples;
        int sample;

        pulseWidthFactor = decideCurrentPulseWidth();
        pulseWidthUs = MinPulseWidthUs + Math.round(pulseWidthFactor * (MaxPulseWidthUs - MinPulseWidthUs));
        pulseWidthSamples = pulseWidthUs * sampleRate / 1000000;

        // generate samples
        for (sample = 0; sample < pulseWidthSamples; sample++) {
            sampleBuffer[sample] = (byte)0xff;
        }
        for (; sample < samplesPerPeriod; sample++) {
            sampleBuffer[sample] = (byte)0;
        }

    }

    public void startPlaying() {
        if (!isPlaying()) {
            stopping = false;
            audioTrack.play();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    currentPeriodTimeMs = 0;

                    int writeCount = samplesPerPeriod;
                    while (writeCount == samplesPerPeriod && !stopping) {
                        decideCurrentPulseWidth();
                        fillBuffer();
                        writeCount = audioTrack.write(sampleBuffer, 0, samplesPerPeriod);
                    }
                }
            }).start();
        }
    }

    public void stopPlaying(boolean immediately) {
        stopping = true;
        if (immediately) {
            audioTrack.pause(); // allowed during write() in other thread
        } else {
            audioTrack.stop(); // audio will stop playing after the last buffer that was written has been played
        }
        audioTrack.flush();
    }

    public boolean isPlaying() {
        return (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
    }

    public void close() {
        stopPlaying(false);
        audioTrack.release();
    }


}
