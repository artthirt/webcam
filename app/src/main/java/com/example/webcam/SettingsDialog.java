package com.example.webcam;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import static com.example.webcam.MainActivity.LOG_TAG;
import static java.lang.Integer.parseInt;

public class SettingsDialog {
    AlertDialog.Builder mDialogBuilder;

    private MainActivity mMain;
    private View mDialogView;

    public SettingsDialog(Activity act, MainActivity main){
        mDialogBuilder = new AlertDialog.Builder(act);
        LayoutInflater inflater = act.getLayoutInflater();
        mDialogView = inflater.inflate(R.layout.settings_layout, null);

        mMain = main;

        mDialogBuilder.setView(mDialogView);

        final EditText eIP = (EditText) mDialogView.findViewById(R.id.ip_address);
        final EditText ePort = (EditText) mDialogView.findViewById(R.id.ip_port);
        final CheckBox chPreview = (CheckBox) mDialogView.findViewById(R.id.preview);
        final RadioButton bJpeg = (RadioButton) mDialogView.findViewById(R.id.JPEG);
        final RadioButton bH264 = (RadioButton) mDialogView.findViewById(R.id.H264);
        final Spinner eFrameRate = (Spinner) mDialogView.findViewById(R.id.frame_rate);
        final EditText eBitrate = (EditText) mDialogView.findViewById(R.id.bitrate);
        eFrameRate.setSelection(2);

        Button bOk = (Button) mDialogView.findViewById(R.id.settings_ok);

        eIP.setText(CameraService.HOST);
        ePort.setText(Integer.toString(CameraService.PORT));
        eBitrate.setText(Integer.toString(CameraService.BITRATE));
        chPreview.setChecked(CameraService.USE_PREVIEW);

        int frameRates[] = {15, 25, 30, 60};

        int id = 0;
        for(int j = 0; j < frameRates.length; ++j){
            if(frameRates[j] == CameraService.FRAMERATE){
                id = j;
                break;
            }
        }
        eFrameRate.setSelection(id);

        bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraService.HOST = eIP.getText().toString();
                Editable sport = ePort.getText();
                CameraService.PORT = parseInt(sport.toString());
                mMain.setUsePreview(chPreview.isChecked());
                mMain.setTypeEncoding(bJpeg.isChecked()? 0 : 1);

                int frame_rate = parseInt(eFrameRate.getSelectedItem().toString());
                CameraService.FRAMERATE = frame_rate;
                Log.i(LOG_TAG, "framerate " + frame_rate);
                int bitrate = parseInt(eBitrate.getText().toString());
                CameraService.BITRATE = bitrate;

                Spinner fcam = (Spinner) mDialogView.findViewById(R.id.sizesfrontcamera);
                Spinner bcam = (Spinner) mDialogView.findViewById(R.id.sizesbackcamera);

                mMain.setFrontCameraIndex(fcam.getSelectedItemPosition());
                mMain.setBackCameraIndex(bcam.getSelectedItemPosition());

                mMain.updateConfig();

                if(mDlg != null){
                    mDlg.dismiss();
                }
            }
        });
    }

    private ArrayAdapter<String> getArrayAdapter(Size sizes[])
    {
        String arrays[] = new String[sizes.length];

        int id = 0;
        for(Size size: sizes){
            String sid = "" + size.getWidth() + "x" + size.getHeight();
            arrays[id++] = sid;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mDialogView.getContext(),
                android.R.layout.simple_spinner_item, arrays);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    public void setSizesFrontCamera(Size[] sizes, int index)
    {
        Spinner fcam = (Spinner) mDialogView.findViewById(R.id.sizesfrontcamera);

        ArrayAdapter<String> adapter = getArrayAdapter(sizes);
        fcam.setAdapter(adapter);
        fcam.setSelection(index);
    }

    public void setSizesBackCamera(Size[] sizes, int index)
    {
        Spinner fcam = (Spinner) mDialogView.findViewById(R.id.sizesbackcamera);

        ArrayAdapter<String> adapter = getArrayAdapter(sizes);
        fcam.setAdapter(adapter);
        fcam.setSelection(index);
    }

    private AlertDialog mDlg = null;

    public void show(){
        mDlg = mDialogBuilder.show();
    }
}
