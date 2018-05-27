package com.capstone.smallserver;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.MultipleFileDownload;
import com.amazonaws.mobileconnectors.s3.transfermanager.MultipleFileUpload;
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class UploadActivity extends AppCompatActivity {


    private static final String SUFFIX = "/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        String path1= Environment.getExternalStorageDirectory().getAbsolutePath()+"/servers";
        File sourceFile=new File(path1+"/chat");
        final Button uploadButton=(Button)findViewById(R.id.awsBtn);


        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:4d7fa6d0-b38a-4156-b02d-62aa17339c92", // 자격 증명 풀 ID
                Regions.US_EAST_1 // 리전
        );

        AmazonS3 s3=new AmazonS3Client(credentialsProvider);
        TransferUtility transferUtility=new TransferUtility(s3,getApplicationContext());

        s3.setRegion(Region.getRegion(Regions.AP_NORTHEAST_2));
        s3.setEndpoint("s3.ap-northeast-2.amazonaws.com");



        uploadButton.setOnClickListener(

                new Button.OnClickListener() {
                    public void onClick(View v) {

                        new Thread() {
                            public  void run() {

                                TransferManager manager = new TransferManager(s3);

                                try
                                {
                                    MultipleFileUpload upload =  manager.uploadDirectory("s3abcd","rsp",new File(path1+"/rsp"), true);
//                                    download.addProgressListener(new ProgressListener() {
//                                        @Override
//                                        public void progressChanged(ProgressEvent progressEvent) {
//                                            double pct= progressEvent.getBytesTransferred() * 100.0/ progressEvent.getBytesTransferred();
//                                            //Log.d("percent",String.valueOf(pct));
//                                            Log.d("percent",download.getProgress().getPercentTransferred()+"%");
//                                        }
//                                    });
                                    upload.waitForCompletion();
//                                    Transfer.TransferState xfer_state=download.getState();
//                                    Log.d("state",xfer_state.toString());

                                }
                                catch (InterruptedException e)
                                {
                                }
                                manager.shutdownNow();

                               // copyDirectory(sourceFile, s3, null);
                            }
                        }.start();
                    }

                }
        );


    }

    public void copyDirectory(File sourceLocation,  AmazonS3 s3,String folderName)
    {
        if (sourceLocation.isDirectory()) {
            if(folderName==null)
                folderName=sourceLocation.getName();
            else
                folderName=folderName+SUFFIX+sourceLocation.getName();

            createFolder("s3abcd",folderName,s3);
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]), s3, folderName);
            }
        } else {

            String fileName=folderName+SUFFIX+sourceLocation.getName();
            s3.putObject(new PutObjectRequest("s3abcd",fileName, sourceLocation).withCannedAcl(CannedAccessControlList.PublicReadWrite));

        }// make sure the directory we plan to store the recording in exists
           // File directory = targetLocation.getParentFile();
//            if (directory != null && !directory.exists() && !directory.mkdirs()) {
//                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
//                TransferObserver observer=transferUtility.upload(
//                        "s3abcd",
//                        sourceLocation.getName(),
//                        sourceLocation
                //);


    }

    public static void createFolder(String bucketName, String folderName, AmazonS3 client) {
        // create meta-data for your folder and set content-length to 0
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);

        // create empty content
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        // create a PutObjectRequest passing the folder name suffixed by /
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
                folderName + SUFFIX, emptyContent, metadata);

        // send request to S3 to create folder
        client.putObject(putObjectRequest);
    }


//
//    public void copy(File sourceLocation) throws IOException {
//        if (sourceLocation.isDirectory()) {
//            copyDirectory(sourceLocation, sourceLocation.getName());
//        } else {
//            copyFile(sourceLocation);
//        }
//    }
//
//    private void copyDirectory(File source, String name) throws IOException {
//
//        for (String f : source.list()) {
//            copy(new File(source, f));
//        }
//    }
//
//    private void copyFile(File source, TransferUtility transferUtility, String name) throws IOException {
//
//        TransferObserver observer=transferUtility.upload(
//                        "s3abcd",
//                        name+"/"+source.getName(),
//                        source
//        );
//    }

}
