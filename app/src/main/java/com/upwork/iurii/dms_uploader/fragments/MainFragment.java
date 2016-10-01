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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.upwork.iurii.dms_uploader.BarcodeCaptureActivity;
import com.upwork.iurii.dms_uploader.DBManager;
import com.upwork.iurii.dms_uploader.R;
import com.upwork.iurii.dms_uploader.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainFragment extends Fragment implements View.OnClickListener, UploadTask.UploadTaskListener {

    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private View rootView;
    private TextView imageCounterTextView, queueSizeTextView;
    private EditText refEditText;
    private ImageView imageView, deleteImageView, backImageView, forwardImageView;

    private String ref, tempImagePath, tempFilename;
    private QueueImage currentImage;
    private Integer queueSize;

    private MediaPlayer mp;
    private DBManager db;

    private ArrayList<QueueImage> newImages;

    public MainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        getActivity().setTitle(R.string.nav_main_screen);
        mp = MediaPlayer.create(getActivity(), R.raw.rvrb2);
        db = DBManager.getInstance();
        queueSize = 0;
        if (UploadTask.isRunning()) {
            UploadTask.getRunningTask().setListener(this);
        }
    }

    private ArrayList<QueueImage> fillQueueImagesList() {
        ArrayList<QueueImage> list = new ArrayList<>();
        ArrayList<HashMap<String, Object>> queues = db.getNewRecordsByRef(ref);
        for (HashMap<String, Object> queueRecord : queues) {
            list.add(new QueueImage((Integer) queueRecord.get("id"), (String) queueRecord.get("filename"), (String) queueRecord.get("fileurl"), (String) queueRecord.get("ref")));
        }
        updateState();
        return list;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        queueSizeTextView = (TextView) rootView.findViewById(R.id.queueSizeTextView);

        imageCounterTextView = (TextView) rootView.findViewById(R.id.imageCounterTextView);
        imageCounterTextView.setVisibility(View.GONE);

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

        ImageButton scanButton = (ImageButton) rootView.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(this);
        Button uploadButton = (Button) rootView.findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(this);

        imageView = (ImageView) rootView.findViewById(R.id.imageView);
        imageView.setOnClickListener(this);
        deleteImageView = (ImageView) rootView.findViewById(R.id.deleteImageView);
        deleteImageView.setOnClickListener(this);
        backImageView = (ImageView) rootView.findViewById(R.id.backImageView);
        backImageView.setOnClickListener(this);
        forwardImageView = (ImageView) rootView.findViewById(R.id.forwardImageView);
        forwardImageView.setOnClickListener(this);

        setRef("");
        if (newImages.size() > 0) {
            setCurrentImage(newImages.size() - 1);
        } else {
            updateState();
        }

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scanButton:
                Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
                break;
            case R.id.imageView:
                if (ref.isEmpty()) {
                    Snackbar.make(rootView, "Set REF first", Snackbar.LENGTH_LONG).show();
                } else {
                    dispatchTakePictureIntent();
                }
                break;
            case R.id.uploadButton:
                if ((queueSize > 0 || newImages.size() > 0) && !UploadTask.isRunning()) {
                    ArrayList<HashMap<String, Object>> list = db.getNewRecordsByRef(ref);
                    list.addAll(db.getQueuePendingRecords());
                    UploadTask uploadTask = new UploadTask(list);
                    uploadTask.setListener(this);
                    uploadTask.execute();
                    setRef("");
                    refEditText.setText("");
                }
                break;
            case R.id.deleteImageView:
                deleteImage();
                break;
            case R.id.backImageView:
                setCurrentImage(newImages.indexOf(currentImage) - 1);
                break;
            case R.id.forwardImageView:
                setCurrentImage(newImages.indexOf(currentImage) + 1);
                break;
        }
    }

    private Uri createImageFile() throws IOException {
        tempFilename = String.format("%1$s-%2$s.jpg", ref, String.valueOf(System.currentTimeMillis() / 1000));
        File image = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), tempFilename);
        tempImagePath = image.getAbsolutePath();
        return getUriFromFile(image);
    }

    private Uri getUriFromFile(File image) {
        return FileProvider.getUriForFile(getActivity(), "com.upwork.iurii.dms_uploader.fileprovider", image);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            try {
                Uri uri = createImageFile();
                List<ResolveInfo> resInfoList = getActivity().getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    getActivity().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
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
            int id = db.addNewRecord(tempImagePath, tempFilename, ref);
            newImages.add(new QueueImage(id, tempFilename, tempImagePath, ref));
            setCurrentImage(newImages.size() - 1);
            updateState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setRef(String ref) {
        this.ref = ref;
        clearImages();
        newImages = fillQueueImagesList();
        if (newImages.size() > 0) {
            setCurrentImage(newImages.size() - 1);
        }
        updateState();
    }

    private void deleteImage() {
        db.deleteRecordById(currentImage.getId());
        File fdelete = new File(currentImage.getImagePath());
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.e("queue", "file Deleted :" + currentImage.getImagePath());
            } else {
                Log.e("queue", "file not deleted :" + currentImage.getImagePath());
            }
        }
        int index = newImages.indexOf(currentImage);
        if (newImages.size() == 1) {
            newImages.remove(index);
            clearImages();
        } else if (index == 0) {
            newImages.remove(index);
            setCurrentImage(index);
        } else if (index > 0) {
            newImages.remove(index);
            setCurrentImage(index - 1);
        }
        updateState();
    }

    private void clearImages() {
        imageView.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.make_photo));
        deleteImageView.setVisibility(View.GONE);
        currentImage = null;
        newImages = new ArrayList<>();
        updateBackForwardArrowsState();
    }

    public void setCurrentImage(int listPosition) {
        this.currentImage = newImages.get(listPosition);
        imageView.setImageURI(getUriFromFile(new File(currentImage.getImagePath())));
        deleteImageView.setVisibility(View.VISIBLE);
        deleteImageView.bringToFront();
        updateBackForwardArrowsState();
    }

    private void updateState() {
        updateImageCounter();
        updateQueueSize();
    }

    private void updateImageCounter() {
        if (!ref.isEmpty()) {
            imageCounterTextView.setText(String.valueOf(newImages.size()));
            imageCounterTextView.setVisibility(View.VISIBLE);
            imageCounterTextView.bringToFront();
        } else {
            imageCounterTextView.setVisibility(View.GONE);
        }
    }

    private void updateQueueSize() {
        queueSize = db.getQueuePendingRecords().size();
        if (queueSize > 0) {
            queueSizeTextView.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.holo_red_dark));
        } else {
            queueSizeTextView.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.black));
        }
        queueSizeTextView.setText(String.format(getResources().getString(R.string.queue_size), queueSize));
    }

    private void updateBackForwardArrowsState() {
        int size = newImages.size();
        backImageView.setVisibility(View.GONE);
        forwardImageView.setVisibility(View.GONE);
        if (size <= 1) {
            return;
        }
        int index = newImages.indexOf(currentImage);
        if (index < size - 1) {
            forwardImageView.setVisibility(View.VISIBLE);
        }
        if (index > 0) {
            backImageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStatusChanged(Integer id, String status, String uploadResult) {
        updateQueueSize();
    }

    @Override
    public void onFinished() {
        updateState();
    }

    @Override
    public void onStarted() {
        updateState();
    }

    class QueueImage {
        private Integer id;
        private String filename;
        private String imagePath;
        private String ref;

        QueueImage(Integer id, String filename, String imagePath, String ref) {
            this.id = id;
            this.filename = filename;
            this.imagePath = imagePath;
            this.ref = ref;
        }

        public Integer getId() {
            return id;
        }

        public String getFilename() {
            return filename;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getRef() {
            return ref;
        }
    }
}
