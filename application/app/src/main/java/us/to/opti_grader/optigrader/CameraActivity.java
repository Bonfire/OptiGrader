package us.to.opti_grader.optigrader;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
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
    private String                 tempAnswers;

    MatOfPoint                     maxContour;

    private Button btnConfirm;
    private Button btnRetry;


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

        btnConfirm = (Button)findViewById(R.id.button_confirm);
        btnRetry = (Button)findViewById(R.id.button_retry);

        btnConfirm.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (start && second) {
                    // If camera has been initialized and the screen has been pressed a second time (to confirm scantron contour).
                    // Start processing image.  This code transforms, crops, and detects the answer circles.
                    pressed = true;
                    Log.i(TAG, "Screen pressed");

                    // Approximates contour with less vertexes
                    MatOfPoint2f m2f = new MatOfPoint2f();
                    maxContour.convertTo(m2f, CvType.CV_32FC2);
                    double arc = Imgproc.arcLength(m2f, true);
                    MatOfPoint2f approx = new MatOfPoint2f();
                    Imgproc.approxPolyDP(m2f, approx, arc * 0.01, true);
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

                    // Get transform to warp how we want
                    Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);

                    // Warp image/material with transform
                    Mat destImage = new Mat();
                    Imgproc.warpPerspective(altframe, destImage, warpMat, mRgba.size());

                    // Isolate scantron answers
                    Rect scantron = new Rect(75, 275, 855, 225);

                    // Crop image (filter out any unnecessary data)
                    Mat cropped = new Mat(destImage, scantron);

                    // Reminder: OpenCV is in landscape!  This means if you're holding your phone upright, x axis is y and y axis is x.
                    // Coordinates (0,0) are the top right of the phone screen. (Still holding phone upright)

                    // Base material to display to users.  SCREEN-SPECIFC!  It only works with camera's initialized to the 1920*1080 resolution.
                    // Deviating from this resolution will result in an app crash bc cant place small material on bigger screen. rgb is 0,0,0 so all pixels initialized to black.
                    Mat incoming = new Mat(1080, 1920, CvType.CV_8UC4, new Scalar(0, 0, 0));

                    // In order to insert cropped image into base material, have to adjust the Region of Interest.  Sad to say it took me 4 hours to figure this out with no help from the internet forums.
                    incoming.adjustROI(0, -(1080 - scantron.height), 0, -(1920 - scantron.width));
                    cropped.copyTo(incoming);
                    incoming.adjustROI(0, 1080 - scantron.height, 0, 1920 - scantron.width);

                    // Filtering and circle detection.  The Hough Circles params are *super* delicate, so treat with care.
                    Mat circles = new Mat();
                    //Convert color to gray
                    Imgproc.cvtColor(incoming, mIntermediateMat, 7);
                    Imgproc.HoughCircles(mIntermediateMat, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 9, 200, 12, 5, 10);

                    List<Point> points = new ArrayList<Point>();

                    // Draw Hough Circles onto base material
                    for (int i = 0; i < circles.cols(); i++) {
                        double[] vCircle = circles.get(0, i);

                        Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                        int radius = (int) Math.round(vCircle[2]);
                        points.add(pt);

                        //Imgproc.circle(incoming, pt, radius, new Scalar(57, 255, 20), 2);
                    }


                    // Sort by y axis (when holding phone upright)
                    Collections.sort(points, new Comparator<Point>() {
                        public int compare(Point o1, Point o2) {
                            int result = 0;
                            if (o1.x > o2.x)
                                result = 1;
                            else if (o1.x < o2.x)
                                result = -1;
                            else
                                result = 0;

                            return result;
                        }
                    });

                    // Group by 5s.  Only if number of answers is divisible by 5 (will crash otherwise)
                    List<List<Point>> points_grouped = new ArrayList<List<Point>>();
                    if (points.size() == 250) {
                        List<Point> temp;
                        for (int i = 0; i < points.size() / 5; i++) {
                            temp = new ArrayList<>();

                            temp.add(points.get(i * 5));
                            temp.add(points.get(i * 5 + 1));
                            temp.add(points.get(i * 5 + 2));
                            temp.add(points.get(i * 5 + 3));
                            temp.add(points.get(i * 5 + 4));

                            points_grouped.add(temp);
                        }
                    }
                    else
                    {
                        second = false;
                        pressed = false;

                        Toast.makeText(getApplicationContext(),"Error: Invalid number of circles, try again.",Toast.LENGTH_LONG).show();

                        return;
                    }


                    // Sort each group by x axis to align with letters A, B, C, etc
                    for (List<Point> group : points_grouped)
                    {
                        Collections.sort(group, new Comparator<Point>()
                        {
                            public int compare(Point o1, Point o2) {
                                int result = 0;
                                if (o1.y < o2.y)
                                    result = 1;
                                else if (o1.y > o2.y)
                                    result = -1;
                                else
                                    result = 0;

                                return result;
                            }
                        });
                    }

                    // If have all the circles, make circle in the letters A and C of question 1
                    //if (points.size() % 5 == 0)
                    //{
                    //    List<Point> group = points_grouped.get(0);
                    //Imgproc.circle(incoming, group.get(0), 5, new Scalar(57, 255, 20), 2);
                    //Imgproc.circle(incoming, group.get(2), 5, new Scalar(57, 255, 20), 2);
                    //}

                    Mat thresh = new Mat(incoming.size(), CvType.CV_8UC1);
                    Imgproc.threshold(mIntermediateMat, thresh, 150, 250, Imgproc.THRESH_BINARY);

                    //Core.bitwise_and(thresh, mask, conjunction);

                    //Grading
                    //Loop of grouped points
                    int selection[] = new int[points_grouped.size()];
                    int lCol = 0;
                    int rCol = 25;
                    // Mat conjunction = new Mat(circles.size(), CvType.CV_8UC1);

                    if(points_grouped.size() > 0)
                    {
                        String tempAnswersl = "";
                        String tempAnswersr = "";

                        for (int i = 0; i < points_grouped.size(); i++)
                        {
                            int mostFilled = 100000;
                            int selectIdx = -1;

                            for (int j = 0; j < 5; j++)
                            {
                                Point cur = points_grouped.get(i).get(j);
                                Mat mask = new Mat(incoming.size(), CvType.CV_8UC1, Scalar.all(0));
                                Imgproc.circle(mask, cur, 6, new Scalar(57, 255, 20), 2);

                                Mat conjunction = new Mat(circles.size(), CvType.CV_8UC1);
                                Core.bitwise_and(thresh, mask, conjunction);

                                int countWhitePixels = Core.countNonZero(conjunction);

                                if (countWhitePixels < mostFilled)
                                {
                                    mostFilled = countWhitePixels;
                                    selectIdx = j;
                                }
                            }
                            //add selected answer to array and output image
                            if (selectIdx != -1 && i % 2 == 0)
                            {
                                selection[lCol] = selectIdx;
                                lCol++;

                                switch (selectIdx)
                                {
                                    case 0:
                                        tempAnswersl = tempAnswersl + "A";
                                        break;
                                    case 1:
                                        tempAnswersl = tempAnswersl + "B";
                                        break;
                                    case 2:
                                        tempAnswersl = tempAnswersl + "C";
                                        break;
                                    case 3:
                                        tempAnswersl = tempAnswersl + "D";
                                        break;
                                    case 4:
                                        tempAnswersl = tempAnswersl + "E";
                                        break;
                                    default:
                                        break;
                                }

                                Imgproc.circle(incoming, points_grouped.get(i).get(selectIdx), 6, new Scalar(57, 255, 20), 2);
                            }
                            else if(selectIdx != -1 && i % 2 == 1)
                            {
                                selection[rCol] = selectIdx;
                                rCol++;

                                switch (selectIdx)
                                {
                                    case 0:
                                        tempAnswersr = tempAnswersr + "A";
                                        break;
                                    case 1:
                                        tempAnswersr = tempAnswersr + "B";
                                        break;
                                    case 2:
                                        tempAnswersr = tempAnswersr + "C";
                                        break;
                                    case 3:
                                        tempAnswersr = tempAnswersr + "D";
                                        break;
                                    case 4:
                                        tempAnswersr = tempAnswersr + "E";
                                        break;
                                    default:
                                        break;
                                }

                                Imgproc.circle(incoming, points_grouped.get(i).get(selectIdx), 6, new Scalar(57, 255, 20), 2);
                            }
                        }

                        tempAnswers =  tempAnswersl + tempAnswersr;
                    }

                    //sort array to string of numbers
                    // Pass to global frame img variable that's returned onCameraFrame.  Shows cropped scantron with circles.
                    mRgba = incoming;
                    second = false;
                }
                else if (pressed == false && start)
                {
                    // First press, stops onCameraFrame updating to show user the contour.  Another press and will start processing.
                    pressed = true;
                    second = true;
                }
                else if (start)
                {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("tempAnswers", tempAnswers);
                    setResult(Activity.RESULT_OK,returnIntent);
                    finish();
                }
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                pressed = false;
                second = false;
            }
        });
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
            //Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            mRgba = altframe;
            // Draw contour only if one actually exists
            if (contours.size() > 0)
                Imgproc.drawContours(mRgba, temp, -1, new Scalar(57, 255, 20), 2);
        }

        return mRgba;
    }
}
