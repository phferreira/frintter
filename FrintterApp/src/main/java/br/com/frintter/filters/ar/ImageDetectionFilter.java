package br.com.frintter.filters.ar;

import android.content.Context;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.com.frintter.filters.Filter;
import br.com.frintter.ui.LabActivity;
import br.com.frintter.utils.PointsUtil;

public final class ImageDetectionFilter implements Filter {

    // The reference image (this detector's target).
    private final Mat mReferenceImage;
    // Features of the reference image.
    private final MatOfKeyPoint mReferenceKeypoints =
            new MatOfKeyPoint();
    // Descriptors of the reference image's features.
    private final Mat mReferenceDescriptors = new Mat();
    // The corner coordinates of the reference image, in pixels.
    // CvType defines the color depth, number of channels, and
    // channel layout in the image. Here, each point is represented
    // by two 32-bit floats.
    private final Mat mReferenceCorners =
            new Mat(4, 1, CvType.CV_32FC2);

    // Features of the scene (the current frame).
    private final MatOfKeyPoint mSceneKeypoints =
            new MatOfKeyPoint();
    // Descriptors of the scene's features.
    private final Mat mSceneDescriptors = new Mat();
    // Tentative corner coordinates detected in the scene, in
    // pixels.
    private final Mat mCandidateSceneCorners =
            new Mat(4, 1, CvType.CV_32FC2);
    // Good corner coordinates detected in the scene, in pixels.
    private final Mat mSceneCorners = new Mat(0, 0, CvType.CV_32FC2);
    // The good detected corner coordinates, in pixels, as integers.
    private final MatOfPoint mIntSceneCorners = new MatOfPoint();

    // A grayscale version of the scene.
    private final Mat mGraySrc = new Mat();
    // Tentative matches of scene features and reference features.
    private final MatOfDMatch mMatches = new MatOfDMatch();

    // A feature detector, which finds features in images.
    private final FeatureDetector mFeatureDetector =
            FeatureDetector.create(FeatureDetector.ORB);
    // A descriptor extractor, which creates descriptors of
    // features.
    private final DescriptorExtractor mDescriptorExtractor =
            DescriptorExtractor.create(DescriptorExtractor.ORB);
    // A descriptor matcher, which matches features based on their
    // descriptors.
    private final DescriptorMatcher mDescriptorMatcher =
            DescriptorMatcher.create(
                    DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

    // The color of the outline drawn around the detected image.
    private final Scalar mLineColor = new Scalar(0, 255, 0);

    private double dX, dY, mediaXY;

    public ImageDetectionFilter(final Context context,
                                final int referenceImageResourceID) throws IOException {

        // Load the reference image from the app's resources.
        // It is loaded in BGR (blue, green, red) format.
        mReferenceImage = Utils.loadResource(context,
                referenceImageResourceID,
                Imgcodecs.CV_LOAD_IMAGE_COLOR);

        // Create grayscale and RGBA versions of the reference image.
        final Mat referenceImageGray = new Mat();
        Imgproc.cvtColor(mReferenceImage, referenceImageGray,
                Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mReferenceImage, mReferenceImage,
                Imgproc.COLOR_BGR2RGBA);

        // Store the reference image's corner coordinates, in pixels.
        mReferenceCorners.put(0, 0,
                0.0, 0.0);
        mReferenceCorners.put(1, 0,
                referenceImageGray.cols(), 0.0);
        mReferenceCorners.put(2, 0,
                referenceImageGray.cols(),
                referenceImageGray.rows());
        mReferenceCorners.put(3, 0,
                0.0, referenceImageGray.rows());

        // Detect the reference features and compute their
        // descriptors.
        mFeatureDetector.detect(referenceImageGray,
                mReferenceKeypoints);
        mDescriptorExtractor.compute(referenceImageGray,
                mReferenceKeypoints, mReferenceDescriptors);
    }

    private double getMediaXY() {
        return mediaXY;
    }

    private void setMediaXY(double mediaXY) {
        this.mediaXY = mediaXY;
    }

    @Override
    public void apply(final Mat src, final Mat dst) {
        // Convert the scene to grayscale.
        Imgproc.cvtColor(src, mGraySrc, Imgproc.COLOR_RGBA2GRAY);

        // Detect the scene features, compute their descriptors,
        // and match the scene descriptors to reference descriptors.
        mFeatureDetector.detect(mGraySrc, mSceneKeypoints);
        mDescriptorExtractor.compute(mGraySrc, mSceneKeypoints,
                mSceneDescriptors);
        mDescriptorMatcher.match(mSceneDescriptors,
                mReferenceDescriptors, mMatches);

        // Attempt to find the target image's corners in the scene.
        findSceneCorners();

        // If the corners have been found, draw an outline around the
        // target image.
        // Else, draw a thumbnail of the target image.
        draw(src, dst);
    }

    private void findSceneCorners() {

        final List<DMatch> matchesList = mMatches.toList();
        if (matchesList.size() < 4) {
            // There are too few matches to find the homography.
            return;
        }

        final List<KeyPoint> referenceKeypointsList =
                mReferenceKeypoints.toList();
        final List<KeyPoint> sceneKeypointsList =
                mSceneKeypoints.toList();

        // Calculate the max and min distances between keypoints.
        double maxDist = 0.0;
        double minDist = Double.MAX_VALUE;
        for (final DMatch match : matchesList) {
            final double dist = match.distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }

        // The thresholds for minDist are chosen subjectively
        // based on testing. The unit is not related to pixel
        // distances; it is related to the number of failed tests
        // for similarity between the matched descriptors.
        if (minDist > 50.0) {
            // The target is completely lost.
            // Discard any previously found corners.
            mSceneCorners.create(0, 0, mSceneCorners.type());
            return;
        } else if (minDist > 25.0) {
            // The target is lost but maybe it is still close.
            // Keep any previously found corners.
            return;
        }

        // Identify "good" keypoints based on match distance.
        final ArrayList<Point> goodReferencePointsList =
                new ArrayList<>();
        final ArrayList<Point> goodScenePointsList =
                new ArrayList<>();
        final double maxGoodMatchDist = 1.75 * minDist;
        for (final DMatch match : matchesList) {
            if (match.distance < maxGoodMatchDist) {
                goodReferencePointsList.add(
                        referenceKeypointsList.get(match.trainIdx).pt);
                goodScenePointsList.add(
                        sceneKeypointsList.get(match.queryIdx).pt);
            }
        }

        if (goodReferencePointsList.size() < 4 ||
                goodScenePointsList.size() < 4) {
            // There are too few good points to find the homography.
            return;
        }

        // There are enough good points to find the homography.
        // (Otherwise, the method would have already returned.)

        // Convert the matched points to MatOfPoint2f format, as
        // required by the Calib3d.findHomography function.
        final MatOfPoint2f goodReferencePoints = new MatOfPoint2f();
        goodReferencePoints.fromList(goodReferencePointsList);
        final MatOfPoint2f goodScenePoints = new MatOfPoint2f();
        goodScenePoints.fromList(goodScenePointsList);

        // Find the homography.
        final Mat homography = Calib3d.findHomography(
                goodReferencePoints, goodScenePoints);

        // Use the homography to project the reference corner
        // coordinates into scene coordinates.
        Core.perspectiveTransform(mReferenceCorners,
                mCandidateSceneCorners, homography);

        // Convert the scene corners to integer format, as required
        // by the Imgproc.isContourConvex function.
        mCandidateSceneCorners.convertTo(mIntSceneCorners,
                CvType.CV_32S);

        // Check whether the corners form a convex polygon. If not,
        // (that is, if the corners form a concave polygon), the
        // detection result is invalid because no real perspective can
        // make the corners of a rectangular image look like a concave
        // polygon!
        if (Imgproc.isContourConvex(mIntSceneCorners)) {
            // The corners form a convex polygon, so record them as
            // valid scene corners.
            mCandidateSceneCorners.copyTo(mSceneCorners);
        }
    }

    private void draw(final Mat src, final Mat dst) {

        if (dst != src) {
            src.copyTo(dst);
        }

        if (mSceneCorners.height() < 4) {
            // The target has not been found.

            // Draw a thumbnail of the target in the upper-left
            // corner so that the user knows what it is.

            // Compute the thumbnail's larger dimension as half the
            // video frame's smaller dimension.
            int height = mReferenceImage.height();
            int width = mReferenceImage.width();
            final int maxDimension = Math.min(dst.width(),
                    dst.height()) / 2;
            final double aspectRatio = width / (double) height;
            if (height > width) {
                height = maxDimension;
                width = (int) (height * aspectRatio);
            } else {
                width = maxDimension;
                height = (int) (width / aspectRatio);
            }

            // Select the region of interest (ROI) where the thumbnail
            // will be drawn.
            final Mat dstROI = dst.submat(0, height, 0, width);

            // Copy a resized reference image into the ROI.
            Imgproc.resize(mReferenceImage, dstROI, dstROI.size(),
                    0.0, 0.0, Imgproc.INTER_AREA);

            return;
        }

        // Outline the found target in green.
        Imgproc.line(dst, new Point(mSceneCorners.get(0, 0)),
                new Point(mSceneCorners.get(1, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(1, 0)),
                new Point(mSceneCorners.get(2, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(2, 0)),
                new Point(mSceneCorners.get(3, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(3, 0)),
                new Point(mSceneCorners.get(0, 0)), mLineColor, 4);
        ArrayList<Point> points = new ArrayList<>();
        points.add(new Point(mSceneCorners.get(0, 0)));
        points.add(new Point(mSceneCorners.get(1, 0)));
        points.add(new Point(mSceneCorners.get(2, 0)));
        points.add(new Point(mSceneCorners.get(3, 0)));

        if (PointsUtil.isSquare(points)) {
            dX = distance(new Point(mSceneCorners.get(0, 0)), new Point(mSceneCorners.get(1, 0))) / 14.5;
            dY = distance(new Point(mSceneCorners.get(1, 0)), new Point(mSceneCorners.get(2, 0))) / 14.5;

            setMediaXY((dX + dY) / 2);
            LabActivity.EXTRA_PHOTO_MAT = getMediaXY();

            int length1 = (int) ((double) dst.rows() / dY);
            int length2 = (int) ((double) dst.cols() / dX);

            Point start = new Point(0.0, 0.0);
            Point startY = new Point(0.0, 0.0);

            // Desenha regua lateral do eixo X
            for (int i = 0; i <= length1; i++) {
                if (i == 1) {
                    // Escreve numeração da regua lateral X
                    for (int j = 0; j <= length2; j++) {
                        startY = new Point(startY.x + dX, 30);
                        Imgproc.putText(dst, "" + (j + 1), startY, 1, 2, new Scalar(255, 0, 0), 2);
                    }
                }
                Imgproc.line(dst, start, new Point(start.x, start.y + dY), new Scalar(255, 0, 0), 4);
                start = new Point(start.x, (start.y + dY));
                Imgproc.line(dst, new Point(0, start.y), new Point(30, start.y), new Scalar(255, 0, 0), 1);
                Imgproc.putText(dst, " " + (i + 1), start, 1, 2, new Scalar(255, 0, 0), 2);
            }

            start = new Point(0.0, 0.0);

            // Desenha regua lateral do eixo Y sem numeros
            for (int i = 0; i <= length2; i++) {
                Imgproc.line(dst, start, new Point(start.x + dX, start.y), new Scalar(255, 0, 0), 4);
                start = new Point(start.x + dX, start.y);
                Imgproc.line(dst, new Point(start.x, 0), new Point(start.x, 30), new Scalar(255, 0, 0), 1);
            }
            Imgproc.putText(dst, "cm",
                    new Point(50, 100), 1, 3, new Scalar(255, 0, 0), 3);
        }
    }
//Imgproc.putText(dst,  distance1 + ":" + distance2, new Point(10, 10), 1, 1.0, new Scalar(255, 255, 255, 0));


    //euclidean distance (?)
    private double distance(Point p, Point q) {
        double deltaX = p.y - q.y;
        double deltaY = p.x - q.x;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private boolean isRectangle(Point p1, Point p2, Point p3, Point p4) {
        double m1, m2, m3;
        m1 = (p2.y - p1.y) / (p2.x - p1.x);
        m2 = (p2.y - p3.y) / (p2.x - p3.x);
        m3 = (p4.y - p3.y) / (p4.x - p3.x);

        return (m1 * m2) == -1 && (m2 * m3) == -1;
    }


}
