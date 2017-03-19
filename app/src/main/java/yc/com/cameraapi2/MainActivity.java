package yc.com.cameraapi2;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import intuit.com.UILoggerSDK.UILogger;
import yc.com.camerasdk_api2.Camera2;
import yc.com.camerasdk_api2.Camera2Fragment;
import yc.com.myutilssdk.Utils.Utils_General;
import yc.com.myutilssdk.Utils.Utils_ImageHandler;

/*
steps
1. add uses-permission in AndroidManifest
2. add TextureView

 */

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        UILogger.init(this);

        Camera2Fragment camera2Fragment = Camera2.init(this);
        camera2Fragment.setOnImageTakenBytesListener(new Camera2Fragment.OnImageTakenListener() {
            @Override
            public void onImageTakenBytes(byte[] bytes, int width, int height) {
                Bitmap bitmap = Utils_ImageHandler.getBitmapFromBytes(bytes);
                bitmap = Utils_ImageHandler.RotateBitmap(bitmap,90);
                UILogger.getLogger().addLogAndUpdateUI(bitmap);
            }
        });

        final Date previousTime = new Date();
        camera2Fragment.setOnImageLiveFrameListener(new Camera2Fragment.OnImageLiveFrameListener() {
            @Override
            public void onImageLiveFrame(byte[] bytes, int width, int height) {
                Log.d("yc","yc live frame");
                long diffInMs = new Date().getTime() - previousTime.getTime();
                long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);
                if (diffInSec > 5)
                {
//                    Bitmap bitmap = Utils_ImageHandler.getBitmapFromNV21(bytes,width,height);
//                    bitmap = Utils_ImageHandler.RotateBitmap(bitmap,90);
//                    UILogger.getLogger().addLogAndUpdateUI(bitmap);
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

}
