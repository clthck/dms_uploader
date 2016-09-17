package com.upwork.iurii.dms_uploader.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.upwork.iurii.dms_uploader.BarcodeCaptureActivity;
import com.upwork.iurii.dms_uploader.DBManager;
import com.upwork.iurii.dms_uploader.R;
import com.upwork.iurii.dms_uploader.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainFragment extends Fragment implements View.OnClickListener, UploadTask.UploadTaskListener {

    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private View rootView;
    private CompoundButton useFlash;
    private TextView imageCounterTextView, queueSizeTextView;
    private EditText refEditText;
    private ImageView imageView;
    private ProgressBar uploadProgress;
    private Button uploadButton;

    private String ref, currentImagePath, currentFilename;
    private Integer imageCount;

    private Uri photoURI;
    private MediaPlayer mp;

    public MainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        getActivity().setTitle(R.string.nav_main_screen);
        mp = MediaPlayer.create(getActivity(), R.raw.rvrb2);
        ref = "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        queueSizeTextView = (TextView) rootView.findViewById(R.id.queueSizeTextView);

        uploadProgress = (ProgressBar) rootView.findViewById(R.id.uploadProgress);

        imageCounterTextView = (TextView) rootView.findViewById(R.id.imageCounterTextView);
        imageCounterTextView.setVisibility(View.GONE);

        useFlash = (CompoundButton) rootView.findViewById(R.id.use_flash);

        refEditText = (EditText) rootView.findViewById(R.id.refEditText);
        refEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setRef(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        Button scanButton = (Button) rootView.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(this);
        uploadButton = (Button) rootView.findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(this);

        imageView = (ImageView) rootView.findViewById(R.id.imageView);
        imageView.setOnClickListener(this);

        updateState();

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scanButton:
                Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
                break;
            case R.id.imageView:
                // TODO: 15.09.2016 add from gallery
                if (ref.isEmpty()) {
                    Snackbar.make(rootView, "Set REF first", Snackbar.LENGTH_LONG).show();
                } else {
                    dispatchTakePictureIntent();
                }
                break;
            case R.id.uploadButton:
                UploadTask uploadTask = new UploadTask();
                uploadTask.setListener(this);
                uploadTask.execute();
                break;
        }
    }

    private File createImageFile() throws IOException {
        currentFilename = String.format("%1$s-%2$s", refEditText.getText().toString(), String.valueOf(imageCount + 1));
        File image = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), currentFilename);
        currentImagePath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                photoURI = FileProvider.getUriForFile(getActivity(), "com.upwork.iurii.dms_uploader.fileprovider", photoFile);
                List<ResolveInfo> resInfoList = getActivity().getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    getActivity().grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE && resultCode == CommonStatusCodes.SUCCESS) {
            if (data != null) {
                mp.start();
                Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                refEditText.setText(barcode.displayValue);
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == CommonStatusCodes.SUCCESS_CACHE) {
            imageView.setImageURI(photoURI);
            DBManager.getInstance().addQueueRecord(currentImagePath, currentFilename, "Queued");
            DBManager.getInstance().increaseCountForRef(ref);
            updateState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setRef(String ref) {
        imageView.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.make_photo));
        photoURI = null;
        imageCounterTextView.setVisibility(View.GONE);
        this.ref = ref;
        updateState();
    }

    private void updateState() {
        if (UploadTask.isRunning()) {
            uploadProgress.setVisibility(View.VISIBLE);
            uploadButton.setVisibility(View.GONE);
            UploadTask.getRunningTask().setListener(this);
        } else {
            uploadProgress.setVisibility(View.GONE);
            uploadButton.setVisibility(View.VISIBLE);
        }

        if (!ref.isEmpty()) {
            imageCount = DBManager.getInstance().getCountForRef(ref);
            imageCounterTextView.setText(String.valueOf(imageCount));
            imageCounterTextView.setVisibility(View.VISIBLE);
            imageCounterTextView.bringToFront();
        }

        queueSizeTextView.setText(String.format(getResources().getString(R.string.queue_size), DBManager.getInstance().getQueuePendingSize()));
    }

    @Override
    public void onStatusChanged(Integer id, String status, String uploadResult) {
    }

    @Override
    public void onFinished() {
        updateState();
    }

    @Override
    public void onStarted() {
        updateState();
    }
}
