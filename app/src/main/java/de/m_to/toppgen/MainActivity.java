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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private static final float DEFAULT_MOTOR_VOLTAGE = 4.8f;
    private static final float DEFAULT_SUPPLY_VOLTAGE = 12.0f;
    private static final int MaxImpulseLength = 1000;
    private static final int MaxImpulseDelay = 1000;
    private static final String TAG_PWMPLAY_FRAGMENT = "pwmplay_fragment";

    private final static String STATE_VOL_LOCK = "vol_lock";
    private final static String STATE_STAY_AWAKE = "stay_awake";
    private final static String STATE_SUPPLY_VOLTAGE = "supply_voltage";
    private final static String STATE_MOTOR_VOLTAGE = "motor_voltage";
    private final static String STATE_DUTY_CYCLE = "duty_cycle";
    private final static String STATE_IMPULSE_LENGTH = "impulse_length";
    private final static String STATE_IMPULSE_DELAY = "impulse_delay";

    private PWMPlayer play = null;

    private void setLimit(float motorVoltage, float supplyVoltage) {
        play.setLimitPulseWidthFactor(motorVoltage / supplyVoltage);
    }

    private void setLocked(boolean l) {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (l) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_SHOW_UI);
        }
    }

    private void setStayAwake(boolean s) {
        if (s) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void save_settings() {
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        ToggleButton toggleVolLock = (ToggleButton)findViewById(R.id.toggleVolLock);
        ToggleButton toggleStayAwake = (ToggleButton)findViewById(R.id.toggleStayAwake);

        EditText editSupplyVoltage = (EditText)findViewById(R.id.editSupplyVoltage);
        EditText editMotorVoltage = (EditText)findViewById(R.id.editMotorVoltage);

        SeekBar seekDutyCycle = (SeekBar)findViewById(R.id.seekDutyCycle);
        SeekBar seekImpulseLength = (SeekBar)findViewById(R.id.seekImpulseLength);
        SeekBar seekImpulseDelay = (SeekBar)findViewById(R.id.seekImpulseDelay);

        editor.putBoolean(STATE_VOL_LOCK, toggleVolLock.isChecked());
        editor.putBoolean(STATE_STAY_AWAKE, toggleStayAwake.isChecked());

        editor.putFloat(STATE_SUPPLY_VOLTAGE, Float.parseFloat(editSupplyVoltage.getText().toString()));
        editor.putFloat(STATE_MOTOR_VOLTAGE, Float.parseFloat(editMotorVoltage.getText().toString()));

        editor.putInt(STATE_DUTY_CYCLE, seekDutyCycle.getProgress());
        editor.putInt(STATE_IMPULSE_LENGTH, seekImpulseLength.getProgress() * PWMPlayer.PeriodLengthMs);
        editor.putInt(STATE_IMPULSE_DELAY, seekImpulseDelay.getProgress() * PWMPlayer.PeriodLengthMs);

        editor.commit();
    }

    private void initialize_views() {
        final Resources res = getResources();
        final SharedPreferences settings = getPreferences(MODE_PRIVATE);

        final TextView viewDutyLevel = (TextView)findViewById(R.id.viewDutyCycle);
        final TextView viewImpulseLength = (TextView)findViewById(R.id.viewImpulseLength);
        final TextView viewImpulseDelay = (TextView)findViewById(R.id.viewImpulseDelay);

        SeekBar seekDutyCycle = (SeekBar)findViewById(R.id.seekDutyCycle);
        SeekBar seekImpulseLength = (SeekBar)findViewById(R.id.seekImpulseLength);
        SeekBar seekImpulseDelay = (SeekBar)findViewById(R.id.seekImpulseDelay);

        ToggleButton toggleMaster = (ToggleButton)findViewById(R.id.toggleMaster);
        ToggleButton toggleVolLock = (ToggleButton)findViewById(R.id.toggleVolLock);
        ToggleButton toggleStayAwake = (ToggleButton)findViewById(R.id.toggleStayAwake);

        final EditText editSupplyVoltage = (EditText)findViewById(R.id.editSupplyVoltage);
        final EditText editMotorVoltage = (EditText)findViewById(R.id.editMotorVoltage);

        toggleMaster.setChecked(play.isPlaying()); // do this before setting change listener here
        toggleMaster.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isPressed) {
                play.setPlaying(isPressed);
            }
        });

        toggleVolLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                setLocked(isChecked);
            }
        });
        toggleVolLock.setChecked(settings.getBoolean(STATE_VOL_LOCK, false));

        // initialize "Keep screen on" toggle
        toggleStayAwake.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                setStayAwake(isChecked);
            }
        });
        toggleStayAwake.setChecked(settings.getBoolean(STATE_STAY_AWAKE, false));

        float motorVoltage = settings.getFloat(STATE_MOTOR_VOLTAGE, DEFAULT_MOTOR_VOLTAGE);
        float supplyVoltage = settings.getFloat(STATE_SUPPLY_VOLTAGE, DEFAULT_SUPPLY_VOLTAGE);
        editMotorVoltage.setText(String.valueOf(motorVoltage));
        editSupplyVoltage.setText(String.valueOf(supplyVoltage));
        setLimit(motorVoltage, supplyVoltage);

        editMotorVoltage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    setLimit(Float.parseFloat(editMotorVoltage.getText().toString()), Float.parseFloat(editSupplyVoltage.getText().toString()));
                }
            }
        });

        editSupplyVoltage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    setLimit(Float.parseFloat(editMotorVoltage.getText().toString()), Float.parseFloat(editSupplyVoltage.getText().toString()));
                }
            }
        });

        // initialize "Power" (pulse width) slider
        seekDutyCycle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewDutyLevel.setText(res.getString(R.string.value_percent, progress));
                play.setPulseWidthFactor(progress / 100.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        int dutyProgress = settings.getInt(STATE_DUTY_CYCLE, Math.round(100 * play.getPulseWidthFactor()));
        seekDutyCycle.setProgress(dutyProgress);

        // initialize impulse length slider
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
        int impulseLength = settings.getInt(STATE_IMPULSE_LENGTH, play.getImpulseLengthMS());
        seekImpulseLength.setMax(MaxImpulseLength / PWMPlayer.PeriodLengthMs);
        seekImpulseLength.setProgress(impulseLength / PWMPlayer.PeriodLengthMs);

        // inititalize impulse delay slider
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
        int impulseDelay = settings.getInt(STATE_IMPULSE_DELAY, play.getImpulseDelayMS());
        seekImpulseDelay.setMax(MaxImpulseDelay / PWMPlayer.PeriodLengthMs);
        seekImpulseDelay.setProgress(impulseDelay / PWMPlayer.PeriodLengthMs);
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

        initialize_views();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            play.close();
        }

        save_settings();
    }

    @Override
    public void onBackPressed() {
        // Do not call super.onBackPressed() to avoid finish().
        moveTaskToBack(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        ToggleButton toggleVolLock = (ToggleButton)findViewById(R.id.toggleVolLock);

        if (toggleVolLock.isChecked() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
