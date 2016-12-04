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

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private static final int maxImpulseLength = 1000;
    private static final int maxImpulseDelay = 1000;
    private static final String TAG_PWMPLAY_FRAGMENT = "pwmplay_fragment";

    private PWMPlayer play = null;
    private AudioManager audio;

    private void initialize_views() {
        final Resources res = getResources();

        final TextView viewDutyLevel = (TextView)findViewById(R.id.viewDutyCycle);
        final TextView viewLevel = (TextView)findViewById(R.id.viewLevel);
        final TextView viewImpulseLength = (TextView)findViewById(R.id.viewImpulseLength);
        final TextView viewImpulseDelay = (TextView)findViewById(R.id.viewImpulseDelay);

        SeekBar seekDutyCycle = (SeekBar)findViewById(R.id.seekDutyCycle);
        SeekBar seekLevel = (SeekBar)findViewById(R.id.seekLevel);
        SeekBar seekImpulseLength = (SeekBar)findViewById(R.id.seekImpulseLength);
        SeekBar seekImpulseDelay = (SeekBar)findViewById(R.id.seekImpulseDelay);

        ToggleButton toggle = (ToggleButton)findViewById(R.id.toggleMaster);

        int pulseWidthMS = play.getPulseWidthMs();
        int progress = pulseWidthMS - PWMPlayer.MinPulseWidthMs;
        int dutyCycle = 100 * progress / (PWMPlayer.MaxPulseWidthMs - PWMPlayer.MinPulseWidthMs);
        seekDutyCycle.setMax(PWMPlayer.MaxPulseWidthMs - PWMPlayer.MinPulseWidthMs);
        seekDutyCycle.setProgress(progress);
        viewDutyLevel.setText(res.getString(R.string.value_percent, dutyCycle));
        seekDutyCycle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int pulseWidthMS = PWMPlayer.MinPulseWidthMs + progress;
                int dutyCycle = 100 * progress / (PWMPlayer.MaxPulseWidthMs - PWMPlayer.MinPulseWidthMs);
                viewDutyLevel.setText(res.getString(R.string.value_percent, dutyCycle));
                play.setPulseWidthMs(pulseWidthMS);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int levelPercent = 100 * audio.getStreamVolume(AudioManager.STREAM_MUSIC) / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekLevel.setMax(100);
        seekLevel.setProgress(levelPercent);
        viewLevel.setText(res.getString(R.string.value_percent, levelPercent));
        seekLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewLevel.setText(res.getString(R.string.value_percent, progress));
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress * audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int impulseLength = play.getImpulseLengthMS();
        seekImpulseLength.setMax(maxImpulseLength / PWMPlayer.PeriodLengthMs);
        seekImpulseLength.setProgress(impulseLength / PWMPlayer.PeriodLengthMs);
        viewImpulseLength.setText(res.getString(R.string.value_ms, impulseLength));
        seekImpulseLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int impulseLength = progress * PWMPlayer.PeriodLengthMs;
                viewImpulseLength.setText(res.getString(R.string.value_ms, impulseLength));

                play.setImpulseLengthMS(impulseLength);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int impulseDelay = play.getImpulseDelayMS();
        seekImpulseDelay.setMax(maxImpulseDelay / PWMPlayer.PeriodLengthMs);
        seekImpulseDelay.setProgress(impulseDelay / PWMPlayer.PeriodLengthMs);
        viewImpulseDelay.setText(res.getString(R.string.value_ms, impulseDelay));
        seekImpulseDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int impulseDelay = progress * PWMPlayer.PeriodLengthMs;
                viewImpulseDelay.setText(res.getString(R.string.value_ms, impulseDelay));

                play.setImpulseDelayMS(impulseDelay);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        toggle.setChecked(play.isPlaying());
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isPressed) {
                if (isPressed) {
                    play.startPlay(); //starts thread
                } else {
                    play.stopPlay(); //stops thread
                }
            }
        });


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getFragmentManager();
        play = (PWMPlayer) fm.findFragmentByTag(TAG_PWMPLAY_FRAGMENT);

        if (play == null) {
            play = new PWMPlayer();
            fm.beginTransaction().add(play, TAG_PWMPLAY_FRAGMENT).commit();
        }

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initialize_views();

        // TODO: install content observer for music volume
    }


    @Override
    protected void onPause() {
        if (isFinishing()) {
            play.close();
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // Do not call super.onBackPressed() to avoid finish().
        moveTaskToBack(false);
    }
}
