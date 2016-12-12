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
import android.view.KeyEvent;
import android.view.WindowManager;
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

    private boolean lockVolume = false;

    private void initialize_views() {
        final Resources res = getResources();

        final TextView viewDutyLevel = (TextView)findViewById(R.id.viewDutyCycle);
        final TextView viewImpulseLength = (TextView)findViewById(R.id.viewImpulseLength);
        final TextView viewImpulseDelay = (TextView)findViewById(R.id.viewImpulseDelay);

        SeekBar seekDutyCycle = (SeekBar)findViewById(R.id.seekDutyCycle);
        SeekBar seekImpulseLength = (SeekBar)findViewById(R.id.seekImpulseLength);
        SeekBar seekImpulseDelay = (SeekBar)findViewById(R.id.seekImpulseDelay);

        ToggleButton toggleMaster = (ToggleButton)findViewById(R.id.toggleMaster);
        ToggleButton toggleVolLock = (ToggleButton)findViewById(R.id.toggleVolLock);
        ToggleButton toggleStayAwake = (ToggleButton)findViewById(R.id.toggleStayAwake);



        toggleMaster.setChecked(play.isPlaying());
        toggleMaster.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isPressed) {
                if (isPressed) {
                    play.startPlay(); //starts thread
                } else {
                    play.stopPlay(); //stops thread
                }
            }
        });

        toggleVolLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC,
                            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                            AudioManager.FLAG_SHOW_UI);
                    lockVolume = true;
                } else {
                    lockVolume = false;
                }
            }
        });

        // initialize "Keep screen on" toggle
        // no need to recover state here
        toggleStayAwake.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });

        // initialize "Power" (pulse width) slider
        int pulseWidthUs = play.getPulseWidthUs();
        int progress = pulseWidthUs - PWMPlayer.MinPulseWidthUs;
        int dutyCycle = 100 * progress / (PWMPlayer.MaxPulseWidthUs - PWMPlayer.MinPulseWidthUs);
        seekDutyCycle.setMax(PWMPlayer.MaxPulseWidthUs - PWMPlayer.MinPulseWidthUs);
        seekDutyCycle.setProgress(progress);
        viewDutyLevel.setText(res.getString(R.string.value_percent, dutyCycle));
        seekDutyCycle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int pulseWidthUs = PWMPlayer.MinPulseWidthUs + progress;
                int dutyCycle = 100 * progress / (PWMPlayer.MaxPulseWidthUs - PWMPlayer.MinPulseWidthUs);
                viewDutyLevel.setText(res.getString(R.string.value_percent, dutyCycle));
                play.setPulseWidthUs(pulseWidthUs);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // initialize impulse length slider
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

        // inititalize impulse delay slider
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
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        FragmentManager fm = getFragmentManager();
        play = (PWMPlayer) fm.findFragmentByTag(TAG_PWMPLAY_FRAGMENT);

        if (play == null) {
            play = new PWMPlayer();
            fm.beginTransaction().add(play, TAG_PWMPLAY_FRAGMENT).commit();
        }

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initialize_views();
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (lockVolume && (keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
