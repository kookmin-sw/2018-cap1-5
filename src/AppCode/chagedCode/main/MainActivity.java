package com.capstone.smallserver;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;


public class MainActivity extends AppCompatActivity {

    //핫스팟 설정 키기
    private void k()
    {
        Intent localIntent = new Intent("android.intent.action.MAIN", null);
        localIntent.addCategory("android.intent.category.LAUNCHER");
        localIntent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
        localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try
        {
            startActivity(localIntent);
            return;
        }
        catch (NullPointerException localNullPointerException)
        {
            localNullPointerException.printStackTrace();
            return;
        }
        catch (SecurityException localSecurityException)
        {
            for (;;) {}
        }
        catch (ActivityNotFoundException localActivityNotFoundException)
        {
            for (;;) {}
        }
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }

    //We just want one instance of node running in the background.
    public static boolean _startedNodeAlready=false;
    public static boolean _startedCopyThread=false;


    final Context context=this;
    String nodeName=null;
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode==1234){
//            nodeName=data.getStringExtra("result");
//        }
//    }

    //옵션 메뉴 생성 코드
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.hotspot,menu);
        return  true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu1:
               k();
               break;
        }
        return  true;
    }



    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {

            Toast.makeText(context, "열고 싶은 서버를 먼저 선택해주세요",Toast.LENGTH_SHORT).show();

            super.handleMessage(msg);

        }

    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        //nodeName=getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project"+"/chat/index.js";

        final Button serverButton = (Button) findViewById(R.id.ServerButton);
        final Button rspButton = (Button) findViewById(R.id.rsp);
        final TextView texView=(TextView) findViewById(R.id.textView2);


        final Handler handler2=new Handler()
        {
            public void handleMessage(Message msg){
                serverButton.setEnabled(true);
                _startedCopyThread=false;
                Toast.makeText(context, "다운로드 완료!",Toast.LENGTH_SHORT).show();
            }
        };

        //외부 저장소에서 파일을 불러오기 위한 권한 획득

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 마시멜로우 버전과 같거나 이상이라면
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "외부 저장소 사용을 위해 읽기/쓰기 필요", Toast.LENGTH_SHORT).show();
                }

                requestPermissions(new String[]
                                {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                        2);  //마지막 인자는 체크해야될 권한 갯수
            }
        }


        //node.js 엔진에서 서버를 여는 스레드 러닝 코드
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                //ExploerAcitivty에서 전달받은 경로값을 nodeName에다 넣어줌.


                try {

                    Intent intent = getIntent();
                    if(intent.getExtras()!=null)
                        {
                        nodeName = intent.getStringExtra("result");
                        texView.setText(intent.getStringExtra("serverName"));

                        }
                    else{

                       handler.sendEmptyMessage(0);
                       return;

                    }
                }
                catch (NullPointerException e)  {
                        e.printStackTrace();
                };

                String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";

                Log.d("nodeDir",nodeDir);
                Log.d("nodeName",nodeName);

                startNodeWithArguments(new String[]{"node",
                        nodeName
            });
            }
        };



        //외부저장소에 있는 servers 폴더에 저장된 파일을 갖고 오는 스레드 러닝 코드

        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {

                //The path where we expect the node project to be at runtime.
                String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";
                String path1=Environment.getExternalStorageDirectory().getAbsolutePath()+"/servers";

                File sourceLocation=new File(path1);
                try {
                    copyDirectory(sourceLocation, new File(nodeDir));
                    Message message=handler2.obtainMessage();
                    handler2.sendMessage(message);

                }
                catch (Exception e)
                {

                }

            }
        };

        //cib 스레드에 서버를 여는 런 코드를, copyassetT 스레드에 파일을 갖고오는 스레드를 할당
        Thread cib = new Thread(runnable);
        Thread copyassetT=new Thread(runnable2);


        //serverButton을 눌렀을때, 서버가져오는 스레드를 start
        serverButton.setOnClickListener(

                new Button.OnClickListener() {
                    public void onClick(View v) {

                        if (!_startedCopyThread) {

                            _startedCopyThread = true;
                            copyassetT.start();
                            serverButton.setEnabled(false);
                        }

                        }

                       /* new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Network operations should be done in the background.
                                new AsyncTask<Void,Void,String>() {
                                    @Override
                                    protected String doInBackground(Void... params) {
                                        String nodeResponse="";
                                        try {
                                            URL localNodeServer = new URL("http://localhost:3000/");
                                            BufferedReader in = new BufferedReader(
                                                    new InputStreamReader(localNodeServer.openStream()));
                                            String inputLine;
                                            while ((inputLine = in.readLine()) != null)
                                                nodeResponse=nodeResponse+inputLine;
                                            in.close();
                                        } catch (Exception ex) {
                                            nodeResponse=ex.toString();
                                        }
                                        return nodeResponse;
                                    }
                                    @Override
                                    protected void onPostExecute(String result) {
                                        //  textViewVersions.setText(Html.fromHtml(result));
                                        browser.loadDataWithBaseURL("http://localhost:3000/",result,"text/html","UTF-8",null);
                                    }
                                }.execute();
                            }
                        },3000);*/



                }
        );

        //서버 키기 버튼을 눌렀을 때 서버가동 스레드를 스타트, 한번 더 누를경우 프로세스 종료 후 재시작
        rspButton.setOnClickListener(

                new Button.OnClickListener() {
                    public void onClick(View v) {

                        if (!_startedNodeAlready) {

                            _startedNodeAlready = true;
                            cib.start();
                            rspButton.setText("서버끄기");


                        } else {

                            //cib.interrupt();

                            Intent i = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                            System.exit(0);

                            _startedNodeAlready = false;


                        }
                    }
                }
        );


        //fetch버튼을 누를 경우 가져올 수 있는 서버 목록을 출력하는 acitivity로 전환

        final Button fetchButton = (Button) findViewById(R.id.ServerListButton);

        fetchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                File dir = context.getFileStreamPath("nodejs-project");
                File[] subFiles = dir.listFiles();

//                if (subFiles != null)
//                {
//                    for (File file : subFiles)
//                    {
//                        // Here is each file
//                        Log.d("hi",file.getAbsolutePath());
//                    }
//                }

                //list를 출력하는 ExplorerActivity를 호출

                Intent explorerIntent=new Intent(MainActivity.this, ExplorerActivity.class);
                MainActivity.this.startActivity(explorerIntent);
            }
        });
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //노드를 실행하는 라이브러리 메소드
    public native Integer startNodeWithArguments(String[] arguments);


//    private static boolean deleteFolderRecursively(File file) {
//        try {
//            boolean res=true;
//            for (File childFile : file.listFiles()) {
//                if (childFile.isDirectory()) {
//                    res &= deleteFolderRecursively(childFile);
//                } else {
//                    res &= childFile.delete();
//                }
//            }
//            res &= file.delete();
//            return res;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    //외부 경로를 앱 내부 경로의 저장소에 copy하는 메소드
    // If targetLocation does not exist, it will be created.
    public void copyDirectory(File sourceLocation, File targetLocation)
         throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }

            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            // make sure the directory we plan to store the recording in exists
            File directory = targetLocation.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
            }

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }

    }


}