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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class VoltageDialogFragment extends DialogFragment {

    private final static float MinVoltage = 0.1f;
    private final static float MaxVoltage = 24.0f;

    private float supplyVoltage;
    private float motorVoltage;

    private int voltageToProgress(float voltage) {
        return (int)((voltage - MinVoltage) * 10f);
    }

    private float progressToVoltage(int progress) {
        return progress / 10f + MinVoltage;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity a = (MainActivity)getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                a.setVoltages(motorVoltage, supplyVoltage);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        @SuppressLint("InflateParams") View dialogView = a.getLayoutInflater().inflate(R.layout.dialog_voltages, null);

        final TextView viewSupplyVoltage = (TextView)dialogView.findViewById(R.id.viewSupplyVoltage);
        final TextView viewMotorVoltage = (TextView)dialogView.findViewById(R.id.viewMotorVoltage);
        final SeekBar seekSupplyVoltage = (SeekBar)dialogView.findViewById(R.id.seekSupplyVoltage);
        final SeekBar seekMotorVoltage = (SeekBar)dialogView.findViewById(R.id.seekMotorVoltage);

        supplyVoltage = a.getSupplyVoltage();
        motorVoltage = a.getMotorVoltage();

        seekSupplyVoltage.setMax(voltageToProgress(MaxVoltage));
        seekMotorVoltage.setMax(voltageToProgress(supplyVoltage));

        seekSupplyVoltage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekMotorVoltage.setMax(progress);
                supplyVoltage = progressToVoltage(progress);
                viewSupplyVoltage.setText(getResources().getString(R.string.value_volt, supplyVoltage));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekMotorVoltage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                motorVoltage = progressToVoltage(progress);
                viewMotorVoltage.setText(getResources().getString(R.string.value_volt, motorVoltage));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekSupplyVoltage.setProgress(voltageToProgress(supplyVoltage));
        seekMotorVoltage.setProgress(voltageToProgress(motorVoltage));

        builder.setView(dialogView);

        return builder.create();
    }

}
