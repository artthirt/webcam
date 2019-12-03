package com.example.webcam;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import static java.lang.Integer.parseInt;

public class SettingsDialog {
    AlertDialog.Builder mDialogBuilder;

    private String mIP;
    private int mPort;
    private MainActivity mMain;

    public SettingsDialog(Activity act, MainActivity main, String ip, int port){
        mDialogBuilder = new AlertDialog.Builder(act);
        LayoutInflater inflater = act.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.settings_layout, null);

        mMain = main;

        mDialogBuilder.setView(dialogView);

        final EditText eIP = (EditText)dialogView.findViewById(R.id.ip_address);
        final EditText ePort = (EditText)dialogView.findViewById(R.id.ip_port);
        final CheckBox chPreview = (CheckBox)dialogView.findViewById(R.id.preview);
        final RadioButton bJpeg = (RadioButton)dialogView.findViewById(R.id.JPEG);
        final RadioButton bH264 = (RadioButton)dialogView.findViewById(R.id.H264);
        final Spinner eFrameRate = (Spinner)dialogView.findViewById(R.id.frame_rate);
        final EditText eBitrate = (EditText)dialogView.findViewById(R.id.bitrate);
        eFrameRate.setSelection(2);
        eBitrate.setText("" + CameraService.BITRATE);

        Button bOk = (Button)dialogView.findViewById(R.id.settings_ok);

        eIP.setText(ip);
        ePort.setText(Integer.toString(port));

        bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIP = eIP.getText().toString();
                Editable sport = ePort.getText();
                mPort = parseInt(sport.toString());
                mMain.setNetworkConfig(mIP, mPort);
                mMain.setUsePreview(chPreview.isChecked());
                mMain.setTypeEncoding(bJpeg.isChecked()? 0 : 1);

                int frame_rate = parseInt(eFrameRate.getSelectedItem().toString());
                CameraService.FRAMERATE = frame_rate;
                int bitrate = parseInt(eBitrate.getText().toString());
                CameraService.BITRATE = bitrate;

                if(mDlg != null){
                    mDlg.dismiss();
                }
            }
        });
    }

    private AlertDialog mDlg = null;

    public void show(){
        mDlg = mDialogBuilder.show();
    }
}
