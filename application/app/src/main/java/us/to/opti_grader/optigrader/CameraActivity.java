package us.to.opti_grader.optigrader;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import android.Manifest;
import android.content.Context;
import android.graphics.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import us.to.optigrader.optigrader.R;

import static android.Manifest.permission.CAMERA;

public class CameraActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private static final String    TAG = "OCVSample::Activity";

    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mIntermediateMat2;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private CameraManager          cameraManager;

    private double                 nextFrameTime = 0;

    MatOfPoint                     maxContour;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.rotationAnimation = rotationAnimation;
        win.setAttributes(winParams);

        setContentView(R.layout.activity_camera);

        ActivityCompat.requestPermissions(CameraActivity.this,
                new String[] {CAMERA}, 1);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e)
        {}
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat2 = new Mat(height, width, CvType.CV_8UC4);
        maxContour = new MatOfPoint();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
        mIntermediateMat2.release();
        maxContour.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        List<MatOfPoint> contours = new ArrayList<>();
        List<MatOfPoint> contours_filtered = new ArrayList<>();
        //Mat circles = new Mat();
        //Imgproc.getPerspectiveTransform()

        // input frame has gray scale format
        mRgba = inputFrame.rgba();
        //Imgproc.bilateralFilter(inputFrame.rgba(), mIntermediateMat2, 5, 175, 175);
        Imgproc.GaussianBlur(inputFrame.gray(), mIntermediateMat2, new Size(5, 5), 0);
        //Imgproc.Sobel();
        //Imgproc.Laplacian();
        //Imgproc.cvtColor(mIntermediateMat2, mRgba, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.Canny(mIntermediateMat2, mIntermediateMat, 75, 200);

        //if (System.currentTimeMillis()/1000 > nextFrameTime)
        //    Imgproc.HoughCircles(mIntermediateMat, circles, Imgproc.CV_HOUGH_GRADIENT, 1.2, 1000, 100, 100, 5);
        Imgproc.findContours(mIntermediateMat, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Contours
        MatOfPoint2f approx  = new MatOfPoint2f();
        MatOfPoint2f contour2f = new MatOfPoint2f();
        double area;
        /*for (MatOfPoint contour : contours)
        {
            contour.convertTo(contour2f, CvType.CV_32FC2);
            //Imgproc.approxPolyDP(contour2f, approx,0.04*Imgproc.arcLength(contour2f, true), true);
            area = Imgproc.contourArea(contour);

            double length = approx.size().height;
            if ((area > 30))
                contours_filtered.add(contour);
        }*/

        double maxContourArea = 0;
        for (MatOfPoint contour : contours)
        {
            double tempContourArea = Imgproc.contourArea(contour);
            if (tempContourArea > maxContourArea)
            {
                maxContour = contour;
                maxContourArea = tempContourArea;
            }
        }

        List<MatOfPoint> temp = new ArrayList<>();
        temp.add(maxContour);
        Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);

        if (contours.size() > 0)
            Imgproc.drawContours(mRgba, temp, -1, new Scalar(57, 255, 20), 2);

        /* Hough Circles
        if (System.currentTimeMillis()/1000 > nextFrameTime)
        {
            for (int i = 0; i < circles.cols(); i++) {
                double[] vCircle = circles.get(0, i);

                Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                int radius = (int) Math.round(vCircle[2]);

                Imgproc.circle(mRgba, pt, radius, new Scalar(57, 255, 20), 2);
            }

            nextFrameTime = System.currentTimeMillis()/1000 + 1;
        }*/

        return mRgba;
    }
}
