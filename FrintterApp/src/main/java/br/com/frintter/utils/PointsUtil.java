package br.com.frintter.utils;

import org.opencv.core.Point;

import java.util.List;

/**
 * Created by rober on 25/10/2016.
 */
public class PointsUtil {


    public static boolean isRectangle(double x1, double y1,
                                      double x2, double y2,
                                      double x3, double y3,
                                      double x4, double y4) {
        double cx, cy;
        double dd1, dd2, dd3, dd4;

        cx = (x1 + x2 + x3 + x4) / 4;
        cy = (y1 + y2 + y3 + y4) / 4;

        // http://stackoverflow.com/questions/2303278/find-if-4-points-on-a-plane-form-a-rectangle
        // testar
        dd1 = Math.sqrt(cx - x1) + Math.sqrt(cy - y1);
        dd2 = Math.sqrt(cx - x2) + Math.sqrt(cy - y2);
        dd3 = Math.sqrt(cx - x3) + Math.sqrt(cy - y3);
        dd4 = Math.sqrt(cx - x4) + Math.sqrt(cy - y4);
        return dd1 == dd2 && dd1 == dd3 && dd1 == dd4;
    }


    public static boolean isSquare(List<Point> points) {
        if (points == null || points.size() != 4)
            return false;
        int dist1 = sqDist(points.get(0), points.get(1));
        int dist2 = sqDist(points.get(0), points.get(2));
        if (dist1 == dist2) { //if neither are the diagonal
            dist2 = sqDist(points.get(0), points.get(3));
        }
        int s = Math.min(dist1, dist2);
        int d = s * 2;

        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                int dist = sqDist(points.get(i), points.get(j));
                if (((dist * 100) / s) < 5 && ((dist * 100) / d) < 5)
                    //    if (dist != s && dist != d)
                    return false;
            }
        }
        return true;
    }

    public static int sqDist(Point p1, Point p2) {
        int x = (int) (p1.x - p2.x);
        int y = (int) (p1.y - p2.y);
        return (x * x + y * y);
    }
}
