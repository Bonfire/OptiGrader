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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import us.to.optigrader.optigrader.R;

import static android.Manifest.permission.CAMERA;

public class CameraActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private static final String    TAG = "OCVSample::Activity";

    private Mat                    mRgba;
    private Mat                    altframe;
    private Mat                    mIntermediateMat;
    private Mat                    mIntermediateMat2;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private CameraManager          cameraManager;

    private double                 nextFrameTime = 0;

    private boolean                pressed;
    private boolean                start = false;
    private boolean                second = false;

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

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
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

        if (!start)
            start = true;

        if (!pressed && start)
        {
            List<MatOfPoint> contours = new ArrayList<>();
            List<MatOfPoint> contours_filtered = new ArrayList<>();
            //Mat circles = new Mat();
            //Imgproc.getPerspectiveTransform()

            // input frame has gray scale format
            altframe = inputFrame.rgba();
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
            MatOfPoint2f approx = new MatOfPoint2f();
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
            for (MatOfPoint contour : contours) {
                double tempContourArea = Imgproc.contourArea(contour);
                if (tempContourArea > maxContourArea) {
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
        }

        return mRgba;
    }

    @Override
    public void onUserInteraction() {
        if (start && second)
        {
            pressed = true;
            Log.i(TAG, "Screen pressed");

            MatOfPoint2f  m2f = new MatOfPoint2f();
            maxContour.convertTo(m2f, CvType.CV_32FC2);
            double arc = Imgproc.arcLength(m2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(m2f, approx, arc*0.01, true);
            MatOfPoint contour = new MatOfPoint();
            approx.convertTo(contour, CvType.CV_32S);

            /*Rect points = Imgproc.minAreaRect(approx).boundingRect();
            Point[] sortedPoints = new Point[4];

            sortedPoints[0] = new Point(points.x, points.y);
            sortedPoints[1] = new Point(points.x+points.width, points.y);
            sortedPoints[2] = new Point(points.x, points.y+points.height);
            sortedPoints[3] = new Point(points.x+points.width, points.y+points.height);*/

            Moments moment = Imgproc.moments(contour);
            int x = (int) (moment.get_m10() / moment.get_m00());
            int y = (int) (moment.get_m01() / moment.get_m00());

            Point[] sortedPoints = new Point[4];

            double[] data;
            int count = 0;
            for (int i = 0; i < contour.rows(); i++) {
                data = contour.get(i, 0);
                double datax = data[0];
                double datay = data[1];
                if (datax < x && datay < y) {
                    sortedPoints[0] = new Point(datax, datay);
                    count++;
                } else if (datax > x && datay < y) {
                    sortedPoints[1] = new Point(datax, datay);
                    count++;
                } else if (datax < x && datay > y) {
                    sortedPoints[2] = new Point(datax, datay);
                    count++;
                } else if (datax > x && datay > y) {
                    sortedPoints[3] = new Point(datax, datay);
                    count++;
                }
            }
            // ^ BUG HERE WHERE NOT TAKING INTO ACCOUNT STRANGE CORNER

            MatOfPoint2f src = new MatOfPoint2f(
                    sortedPoints[0],
                    sortedPoints[1],
                    sortedPoints[2],
                    sortedPoints[3]);

            MatOfPoint2f dst = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(1100 - 1, 0),
                    new Point(0, 550 - 1),
                    new Point(1100 - 1, 550 - 1)
            );

            Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);

            Mat destImage = new Mat();
            Imgproc.warpPerspective(altframe, destImage, warpMat, mRgba.size());

            List<MatOfPoint> temp = new ArrayList<>();
            temp.add(new MatOfPoint(
                    new Point(0, 0),
                    new Point(0, 550),
                    new Point(1100, 550),
                    new Point(1100, 0)));
            Imgproc.drawContours(destImage, temp, -1, new Scalar(57, 255, 20), 2);

            mRgba = destImage;
        }
        else if (pressed == false && start)
        {
            second = true;
            pressed = true;
        }
    }
}
