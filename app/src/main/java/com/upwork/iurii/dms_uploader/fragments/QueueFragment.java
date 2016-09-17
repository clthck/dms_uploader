package com.upwork.iurii.dms_uploader.fragments;


import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.upwork.iurii.dms_uploader.DBManager;
import com.upwork.iurii.dms_uploader.R;
import com.upwork.iurii.dms_uploader.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class QueueFragment extends Fragment implements UploadTask.UploadTaskListener, View.OnClickListener {

    private View rootView;
    private RecyclerView recyclerView;
    private Button clearButton;
    private QueueAdapter adapter;

    private HashMap<Integer, QueueAdapter.QueueRecord> mapViewHolderById;

    public QueueFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        getActivity().setTitle(R.string.nav_queue);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_queue, container, false);

        clearButton = (Button) rootView.findViewById(R.id.clearButton);
        clearButton.setOnClickListener(this);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new QueueAdapter(DBManager.getInstance().getQueue(), R.layout.item_queue);
        recyclerView.setAdapter(adapter);

        if (UploadTask.isRunning()) {
            UploadTask.getRunningTask().setListener(this);
        }

        return rootView;
    }

    @Override
    public void onStatusChanged(Integer id, String status, String uploadResult) {
        QueueAdapter.QueueRecord queueRecord = mapViewHolderById.get(id);
        if (queueRecord != null) {
            queueRecord.setStatus(status, uploadResult);
        }
    }

    @Override
    public void onFinished() {

    }

    @Override
    public void onStarted() {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clearButton:
                ArrayList<String> filesToDelete = DBManager.getInstance().clearQueueByStatus("Done");
                for (String uri : filesToDelete) {
                    File fdelete = new File(Uri.parse(uri).getPath());
                    if (fdelete.exists()) {
                        if (fdelete.delete()) {
                            Log.e("queue", "file Deleted :" + uri);
                        } else {
                            Log.e("queue", "file not deleted :" + uri);
                        }
                    }
                }
                adapter = new QueueAdapter(DBManager.getInstance().getQueue(), R.layout.item_queue);
                recyclerView.setAdapter(adapter);
                break;
        }
    }

    private class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> data;
        private final int item_queue;

        public QueueAdapter(ArrayList<HashMap<String, Object>> data, int item_queue) {
            this.data = data;
            this.item_queue = item_queue;
            mapViewHolderById = new HashMap<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(item_queue, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            HashMap<String, Object> item = data.get(position);
            QueueRecord queueRecord = mapViewHolderById.get((Integer) item.get("id"));
            if (queueRecord == null) {
                String status = (String) item.get("status");
                String filename = (String) item.get("filename");
                String uploadResult = (String) item.get("upload_result");
                if (uploadResult == null) uploadResult = "";
                queueRecord = new QueueRecord(position, filename, status, uploadResult);
                mapViewHolderById.put((Integer) item.get("id"), queueRecord);
            }

            holder.imageName.setText(queueRecord.getFilename());
            holder.status.setText(
                    String.format(
                            "%s%s",
                            queueRecord.getStatus(),
                            (queueRecord.getUploadResult().isEmpty()) ? "" : String.format(" [%s]", queueRecord.getUploadResult())
                    )
            );
            int color = 0;
            switch (queueRecord.getStatus()) {
                case "Done":
                    color = ContextCompat.getColor(getActivity(), android.R.color.holo_green_light);
                    break;
                case "Error":
                    color = ContextCompat.getColor(getActivity(), android.R.color.holo_red_light);
                    break;
                case "Uploading...":
                    color = ContextCompat.getColor(getActivity(), android.R.color.holo_blue_bright);
                    break;
                case "Compressing...":
                    color = ContextCompat.getColor(getActivity(), android.R.color.holo_blue_light);
                    break;
            }
            holder.itemLayout.setBackgroundColor(color);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView imageName, status;
            public LinearLayout itemLayout;

            public ViewHolder(View itemView) {
                super(itemView);
                imageName = (TextView) itemView.findViewById(R.id.imageNameTextView);
                status = (TextView) itemView.findViewById(R.id.statusTextView);
                itemLayout = (LinearLayout) itemView.findViewById(R.id.item_layout);
            }
        }

        class QueueRecord {
            private final int position;
            private final String filename;
            private String status;
            private String uploadResult;

            QueueRecord(int position, String filename, String status, String uploadResult) {
                this.position = position;
                this.filename = filename;
                this.status = status;
                this.uploadResult = uploadResult;
            }

            void setStatus(String status, String uploadResult) {
                this.status = status;
                this.uploadResult = uploadResult;
                notifyItemChanged(position);
            }

            public String getFilename() {
                return filename;
            }

            public String getStatus() {
                return status;
            }

            public String getUploadResult() {
                return uploadResult;
            }
        }

    }

}
