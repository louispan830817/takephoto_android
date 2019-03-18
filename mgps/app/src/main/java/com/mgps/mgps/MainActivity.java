package com.mgps.mgps;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    WebView myWebView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        myWebView = (WebView)findViewById(R.id.webView);
        myWebView.loadUrl("http://113.196.107.137:8088/UploadFiles.asp");
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        myWebView.getSettings().setSupportMultipleWindows(false);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.getSettings().setSupportZoom(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setDefaultTextEncodingName("UTF-8");
        myWebView.getSettings().getAllowFileAccess();
        myWebView.getSettings().setAllowContentAccess(true);
        myWebView.setWebViewClient(new MyWebViewClient());

        myWebView.setWebChromeClient(new WebChromeClient());


}
    class MyWebChromeClient extends WebChromeClient {
        // For Android < 3.0

        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            WebCameraHelper.getInstance().mUploadMessage = uploadMsg;
            WebCameraHelper.getInstance().showOptions(MainActivity.this);
        }

        // For Android > 4.1.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                    String acceptType, String capture) {
            WebCameraHelper.getInstance().mUploadMessage = uploadMsg;
            WebCameraHelper.getInstance().showOptions(MainActivity.this);
        }

        // For Android > 5.0支持多张上传
        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> uploadMsg,
                                         FileChooserParams fileChooserParams) {
            WebCameraHelper.getInstance().mUploadCallbackAboveL = uploadMsg;
            WebCameraHelper.getInstance().showOptions(MainActivity.this);
            return true;
        }
    }
    public static class WebCameraHelper {
        private static class SingletonHolder {
            static final WebCameraHelper INSTANCE = new WebCameraHelper();
        }

        public static WebCameraHelper getInstance() {
            return SingletonHolder.INSTANCE;
        }

        public ValueCallback<Uri> mUploadMessage;
        public ValueCallback<Uri[]> mUploadCallbackAboveL;

        public Uri fileUri;
        public static final int TYPE_REQUEST_PERMISSION = 3;
        public static final int TYPE_CAMERA = 1;
        public static final int TYPE_GALLERY = 2;

        public void showOptions(final Activity act) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(act);
            alertDialog.setOnCancelListener(new ReOnCancelListener());
            alertDialog.setTitle("選擇");
            alertDialog.setItems(new CharSequence[]{"相機", "相簿"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                if (ContextCompat.checkSelfPermission(act,
                                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    // 申请WRITE_EXTERNAL_STORAGE权限
                                    ActivityCompat
                                            .requestPermissions(
                                                    act,
                                                    new String[]{Manifest.permission.CAMERA},
                                                    TYPE_REQUEST_PERMISSION);
                                } else {
                                    toCamera(act);
                                }
                            } else {
                                Intent i = new Intent(
                                        Intent.ACTION_PICK,
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);// 调用android的图库
                                act.startActivityForResult(i,
                                        TYPE_GALLERY);
                            }
                        }
                    });
            alertDialog.show();
        }

/*        *
         * 点击取消的回调*/

        private class ReOnCancelListener implements
                DialogInterface.OnCancelListener {

            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessage = null;
                }
                if (mUploadCallbackAboveL != null) {
                    mUploadCallbackAboveL.onReceiveValue(null);
                    mUploadCallbackAboveL = null;
                }
            }
        }

/*        *
         * 请求拍照
         *
         * @param act*/

        public void toCamera(Activity act) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 调用android的相机
            // 创建一个文件保存图片
            fileUri = Uri.fromFile(FileManager.getImgFile(act.getApplicationContext()));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            act.startActivityForResult(intent, TYPE_CAMERA);
        }

/*        *
         * startActivityForResult之后要做的处理
         *
         * @param requestCode
         * @param resultCode
         * @param intent*/

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            if (requestCode == TYPE_CAMERA) { // 相册选择
                if (resultCode == -1) {//RESULT_OK = -1，拍照成功
                    if (mUploadCallbackAboveL != null) { //高版本SDK处理方法
                        Uri[] uris = new Uri[]{fileUri};
                        mUploadCallbackAboveL.onReceiveValue(uris);
                        mUploadCallbackAboveL = null;
                    } else if (mUploadMessage != null) { //低版本SDK 处理方法
                        mUploadMessage.onReceiveValue(fileUri);
                        mUploadMessage = null;
                    } else {
//                    Toast.makeText(MainActivity.this, "无法获取数据", Toast.LENGTH_LONG).show();
                    }
                } else { //拍照不成功，或者什么也不做就返回了，以下的处理非常有必要，不然web页面不会有任何响应
                    if (mUploadCallbackAboveL != null) {
                        mUploadCallbackAboveL.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                        mUploadCallbackAboveL = null;
                    } else if (mUploadMessage != null) {
                        mUploadMessage.onReceiveValue(fileUri);
                        mUploadMessage = null;
                    } else {
//                    Toast.makeText(MainActivity.this, "无法获取数据", Toast.LENGTH_LONG).show();
                    }

                }
            } else if (requestCode == TYPE_GALLERY) {// 相册选择
                if (mUploadCallbackAboveL != null) {
                    mUploadCallbackAboveL.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                    mUploadCallbackAboveL = null;
                } else if (mUploadMessage != null) {
                    Uri result = intent == null || resultCode != Activity.RESULT_OK ? null : intent.getData();
                    mUploadMessage.onReceiveValue(result);
                    mUploadMessage = null;
                } else {
//                Toast.makeText(MainActivity.this, "无法获取数据", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
/*    *
     * @desc
     * @auth 方毅超
     * @time 2017/12/8 14:41*/


    public static class FileManager {

        public static final String ROOT_NAME = "RongYifu";
        public static final String LOG_NAME = "UserLog";
        public static final String CACHE_NAME = "Cache";
        public static final String IMAGE_NAME = "Image";
        public static final String RECORD_NAME = "Voice";

        public static final String ROOT_PATH = File.separator + ROOT_NAME
                + File.separator;
        public static final String LOG_PATH_NAME = File.separator + LOG_NAME
                + File.separator;
        public static final String CACHE_PATH_NAME = File.separator + CACHE_NAME
                + File.separator;
        public static final String IMAGE_PATH_NAME = File.separator + IMAGE_NAME
                + File.separator;
        public static final String RECORD_PATH_NAME = File.separator + RECORD_NAME
                + File.separator;

        public static final String ACTION_DEL_ALL_IMAGE_CACHE = "com.citic21.user_delImageCache";
        public static final String CODE_ENCODING = "utf-8";

        public static String getRootPath(Context appContext) {

            String rootPath = null;
            if (checkMounted()) {
                rootPath = getRootPathOnSdcard();
            } else {
                rootPath = getRootPathOnPhone(appContext);
            }
            return rootPath;
        }

        public static String getRootPathOnSdcard() {
            File sdcard = Environment.getExternalStorageDirectory();
            String rootPath = sdcard.getAbsolutePath() + ROOT_PATH;
            return rootPath;
        }

        public static String getRootPathOnPhone(Context appContext) {
            File phoneFiles = appContext.getFilesDir();
            String rootPath = phoneFiles.getAbsolutePath() + ROOT_PATH;
            return rootPath;
        }

        public static String getSdcardPath() {
            File sdDir = null;
            boolean sdCardExist = checkMounted(); // 判断sd卡是否存在
            if (sdCardExist) {
                sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
                return sdDir.getPath();
            }
            return "/";
        }

        // SD卡剩余空间
        public long getSDFreeSize() {
            // 取得SD卡文件路径
            File path = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(path.getPath());
            // 获取单个数据块的大小(Byte)
            long blockSize = sf.getBlockSize();
            // 空闲的数据块的数量
            long freeBlocks = sf.getAvailableBlocks();
            // 返回SD卡空闲大小
            // return freeBlocks * blockSize; //单位Byte
            // return (freeBlocks * blockSize)/1024; //单位KB
            return (freeBlocks * blockSize) / 1024 / 1024; // 单位MB
        }

        public static boolean checkMounted() {
            return Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState());
        }

        public static String getUserLogDirPath(Context appContext) {

            String logPath = getRootPath(appContext) + LOG_PATH_NAME;
            return logPath;
        }

        // 缓存整体路径
        public static String getCacheDirPath(Context appContext) {

            String imagePath = getRootPath(appContext) + CACHE_PATH_NAME;
            return imagePath;
        }

        // 图片缓存路径
        public static String getImageCacheDirPath(Context appContext) {

            String imagePath = getCacheDirPath(appContext) + IMAGE_PATH_NAME;
            return imagePath;
        }

        // 创建一个图片文件
        public static File getImgFile(Context context) {
            File file = new File(getImageCacheDirPath(context));
            if (!file.exists()) {
                file.mkdirs();
            }
            String imgName = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            File imgFile = new File(file.getAbsolutePath() + File.separator
                    + "IMG_" + imgName + ".jpg");
            return imgFile;
        }


        // 创建拍照处方单路径
        public static File initCreatImageCacheDir(Context appContext) {
            String rootPath = getImageCacheDirPath(appContext);
            File dir = new File(rootPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir;
        }


        public static final String getFileSize(File file) {
            String fileSize = "0.00K";
            if (file.exists()) {
                fileSize = FormetFileSize(file.length());
                return fileSize;
            }
            return fileSize;
        }

        public static String FormetFileSize(long fileS) {// 转换文件大小
            DecimalFormat df = new DecimalFormat("0.00");
            String fileSizeString = "";
            if (fileS < 1024) {
                fileSizeString = df.format((double) fileS) + "B";
            } else if (fileS < 1048576) {
                fileSizeString = df.format((double) fileS / 1024) + "K";
            } else if (fileS < 1073741824) {
                fileSizeString = df.format((double) fileS / 1048576) + "M";
            } else {
                fileSizeString = df.format((double) fileS / 1073741824) + "G";
            }
            return fileSizeString;
        }

        public static boolean writeStringToFile(String text, File file) {
            try {
                return writeStringToFile(text.getBytes(CODE_ENCODING), file);
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
                return false;
            }
        }

        static void close(Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        public static boolean writeStringToFile(byte[] datas, File file) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(datas);
                fos.flush();
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(fos);
            }
            return false;
        }

 /*       *
         * @param oldpath URL 的 md5+"_tmp"
         * @param newpath URL 的 md5+
         * @return*/

        public static boolean renameFileName(String oldpath, String newpath) {
            try {
                File file = new File(oldpath);
                if (file.exists()) {
                    file.renameTo(new File(newpath));
                }
                return true;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return false;
        }

        public static final boolean isFileExists(File file) {
            if (file.exists()) {

                return true;
            }
            return false;
        }

        public static final long getFileSizeByByte(File file) {
            long fileSize = 0l;
            if (file.exists()) {
                fileSize = file.length();
                return fileSize;
            }
            return fileSize;
        }


        public static boolean checkCachePath(Context appContext) {
            String path = getCacheDirPath(appContext);
            File file = new File(path);
            if (!file.exists()) {
                return false;
            }
            return true;
        }


        public static String getUrlFileName(String resurlt) {
            if (!TextUtils.isEmpty(resurlt)) {
                int nameIndex = resurlt.lastIndexOf("/");
                String loacalname = "";
                if (nameIndex != -1) {
                    loacalname = resurlt.substring(nameIndex + 1);
                }

                int index = loacalname.indexOf("?");
                if (index != -1) {
                    loacalname = loacalname.substring(0, index);
                }
                return loacalname;
            } else {
                return resurlt;
            }
        }

        // 存储map类型数据 转换为Base64进行存储
        public static String SceneList2String(Map<String, String> SceneList)
                throws IOException {
            ByteArrayOutputStream toByte = new ByteArrayOutputStream();

            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(toByte);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                oos.writeObject(SceneList);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // 对byte[]进行Base64编码

            String SceneListString = new String(Base64.encode(toByte.toByteArray(),
                    Base64.DEFAULT));
            return SceneListString;

        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("Web",url);
            return super.shouldOverrideUrlLoading(view, url);
        }
    }
}
