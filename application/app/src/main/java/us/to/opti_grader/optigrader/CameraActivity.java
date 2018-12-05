package us.to.opti_grader.optigrader;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
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

import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
            // This code is run after camera init and when the user has not pressed the screen
            // If the user has pressed the screen, control is turned over to onUserInteraction()

            List<MatOfPoint> contours = new ArrayList<>();

            // Save frame to global variable for onUserInteraction() processing
            altframe = inputFrame.rgba();

            // Filtering
            Imgproc.GaussianBlur(inputFrame.gray(), mIntermediateMat2, new Size(5, 5), 0);
            Imgproc.Canny(mIntermediateMat2, mIntermediateMat, 75, 200);

            // Obtain contours, looking for scantron outline
            Imgproc.findContours(mIntermediateMat, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Draw biggest contour onto the screen.  If user presses screen, they will select that
            // contour and control will be moved to onUserInteraction()
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

            // Draw contour only if one actually exists
            if (contours.size() > 0)
                Imgproc.drawContours(mRgba, temp, -1, new Scalar(57, 255, 20), 2);
        }

        return mRgba;
    }

    // This method is called by Android every time there is an input event by the user.
    // Touching the screen calls this.
    @Override
    public void onUserInteraction() {
        if (start && second)
        {
            // If camera has been initialized and the screen has been pressed a second time (to confirm scantron contour).
            // Start processing image.  This code transforms, crops, and detects the answer circles.
            pressed = true;
            Log.i(TAG, "Screen pressed");

            // Approximates contour with less vertexes
            MatOfPoint2f  m2f = new MatOfPoint2f();
            maxContour.convertTo(m2f, CvType.CV_32FC2);
            double arc = Imgproc.arcLength(m2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(m2f, approx, arc*0.01, true);
            MatOfPoint contour = new MatOfPoint();
            approx.convertTo(contour, CvType.CV_32S);

            // Get the centroid of the image
            Moments moment = Imgproc.moments(contour);
            int x = (int) (moment.get_m10() / moment.get_m00());
            int y = (int) (moment.get_m01() / moment.get_m00());

            Point[] sortedPoints = new Point[4];

            // Using that centroid, find the outermost points on the image's matrix.
            double[] data;
            int count = 0;
            Log.i(TAG, "Screen pressed2: " + contour.rows());
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
            // ^ BUG HERE WHERE NOT TAKING INTO ACCOUNT STRANGE CORNER.  Can ignore for now.  Not that important

            // Corners of material to perspective transform from
            MatOfPoint2f src = new MatOfPoint2f(
                    sortedPoints[0],
                    sortedPoints[1],
                    sortedPoints[2],
                    sortedPoints[3]);

            // Corners of material to perspective transform to
            MatOfPoint2f dst = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(1100 - 1, 0),
                    new Point(0, 550 - 1),
                    new Point(1100 - 1, 550 - 1)


            );
            //                    new Point(0, 0),
            //                    new Point(1920 - 1, 0),
            //                    new Point(0, 1080 - 1),
            //                    new Point(1920 - 1, 1080 - 1)

            // Get transform to warp how we want
            Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);

            // Warp image/material with transform
            Mat destImage = new Mat();
            Imgproc.warpPerspective(altframe, destImage, warpMat, mRgba.size());

            // ignore
            /*List<MatOfPoint> temp = new ArrayList<>();
            temp.add(new MatOfPoint(
                    new Point(0, 0),
                    new Point(0, 550),
                    new Point(1100, 550),
                    new Point(1100, 0)));*/
            //Imgproc.drawContours(destImage, temp, -1, new Scalar(57, 255, 20), 2);

            // Isolate scantron answers
            Rect scantron = new Rect(95, 275, 835, 225);

            // Crop image (filter out any unnecessary data)
            Mat cropped = new Mat(destImage, scantron);

            // Reminder: OpenCV is in landscape!  This means if you're holding your phone upright, x axis is y and y axis is x.
            // Coordinates (0,0) are the top right of the phone screen. (Still holding phone upright)

            // Base material to display to users.  SCREEN-SPECIFC!  It only works with camera's initialized to the 1920*1080 resolution.
            // Deviating from this resolution will result in an app crash bc cant place small material on bigger screen. rgb is 0,0,0 so all pixels initialized to black.
            Mat incoming = new Mat(1080, 1920, CvType.CV_8UC4, new Scalar(0, 0, 0));

            // In order to insert cropped image into base material, have to adjust the Region of Interest.  Sad to say it took me 4 hours to figure this out with no help from the internet forums.
            incoming.adjustROI(0, -(1080-scantron.height), 0, -(1920-scantron.width));
            cropped.copyTo(incoming);
            incoming.adjustROI(0, 1080-scantron.height, 0, 1920-scantron.width);

            // Filtering and circle detection.  The Hough Circles params are *super* delicate, so treat with care.
            Mat circles = new Mat();
            List<MatOfPoint> bub = new ArrayList<>();
            Imgproc.cvtColor(incoming,mIntermediateMat, 7);
            //Imgproc.Canny(incoming, mIntermediateMat, 75, 200);
            Imgproc.HoughCircles(mIntermediateMat, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 9, 200, 12, 5, 10);
            //Imgproc.findContours(mIntermediateMat, bub, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            //            List<MatOfPoint> draft = new ArrayList<>();
            //            for(MatOfPoint c : bub)
            //            {
            //                Rect rec = Imgproc.boundingRect(c);
            //                int w = rec.width;
            //                int h = rec.height;
            //                double ratio = Math.max(w,h) / Math.min(w,h);
            //
            //                if(ratio >=  0.9 && ratio <= 1.1)
            //                    draft.add(c);
            //            }
            //
            //            if (draft.size() > 0)
            //                Imgproc.drawContours(mRgba, draft, -1, new Scalar(57, 255, 20), 2);
            //            }


            List<double[]> bubbles = new ArrayList<>();
            // Draw Hough Circles onto base material
            for (int i = 0; i < circles.cols(); i++)
            {
                double[] vCircle = circles.get(0, i);
                bubbles.add(vCircle);

                //Moved output temporarily to after sorting
                //Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                //int radius = (int) Math.round(vCircle[2]);
                //Imgproc.circle(incoming, pt, radius, new Scalar(57, 255, 20), 2);
            }


            //WORK AREA
            //sort bubbles based on X-axis
            //Short on time, could think of better sort method
            Collections.sort(bubbles, new Comparator<double[]>()
            {
                @Override
                public int compare(double[] a, double[] b)
                {
                    if(a[0] < b[0])
                        return -1;
                    else if(a[0] > b[0])
                        return 1;
                    else
                        return 0;
                }
            });
            for(int i = 0; i < bubbles.size(); i+=5)
            {
                List<double[]> question = new ArrayList<>();
                for(int j = 0; j < 5; j++)
                {
                    question.add(bubbles.get(i+j));
                    Point pt = new Point(Math.round(question.get(i+j)[0]), Math.round(question.get(i+j)[1]));
                    int radius = (int) Math.round(question.get(i+j)[2]);
                    Imgproc.circle(incoming, pt, radius, new Scalar(57, 255, 20), 2);
                }
                //Sort 5 bubbles for each question based on y-axis.
                //Collections.sort(question, new Comparator<double[]>()
                //{
                //    @Override
                //    public int compare(double[] a, double[] b)
                //    {
                //        if(a[1] < b[1])
                //            return -1;
                //        else if(a[1] > b[1])
                //            return 1;
                //        else
                //            return 0;
                //        }
                //});
            }
            //END OF WORK AREA

            // Pass to global frame img variable that's returned onCameraFrame.  Shows cropped scantron with circles.
            mRgba = incoming;
        }
        else if (pressed == false && start)
        {
            // First press, stops onCameraFrame updating to show user the contour.  Another press and will start processing.
            second = true;
            pressed = true;
        }
    }
}
