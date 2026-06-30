package dev.alexis.logika.model;

import dev.alexis.logika.util.Vec2;
import java.util.ArrayList;
import java.util.List;

public final class WirePath {
    public static final int DEFAULT_SAMPLES_PER_SEGMENT = 24;
    private static final double MIN_DEFAULT_CONTROL = 70.0;
    private WirePath() { }

    public static List<Segment> segments(Vec2 start, List<Vec2> controlPoints, Vec2 end) {
        List<Vec2> points = new ArrayList<>((controlPoints == null ? 0 : controlPoints.size()) + 2);
        points.add(start);
        if (controlPoints != null) points.addAll(controlPoints);
        points.add(end);
        if (points.size() == 2) return List.of(defaultSegment(points.get(0), points.get(1)));
        List<Segment> result = new ArrayList<>(points.size() - 1);
        for (int i = 0; i < points.size() - 1; i++) result.add(catmullRomSegment(points, i));
        return result;
    }

    public static Hit nearestPoint(Vec2 query, Vec2 start, List<Vec2> controlPoints, Vec2 end, int samplesPerSegment) {
        List<Segment> segments = segments(start, controlPoints, end);
        Hit best = new Hit(start, start.distanceTo(query), 0, 0.0, 0.0);
        int samples = Math.max(8, samplesPerSegment);
        for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
            Segment segment = segments.get(segmentIndex);
            for (int i = 0; i <= samples; i++) {
                double t = i / (double) samples;
                Vec2 point = segment.pointAt(t);
                double distance = point.distanceTo(query);
                if (distance < best.distance()) best = new Hit(point, distance, segmentIndex, t, (segmentIndex + t) / segments.size());
            }
        }
        return best;
    }

    public static Vec2 sample(List<Segment> segments, double pathT) {
        if (segments.isEmpty()) return new Vec2(0.0, 0.0);
        double scaled = clamp(pathT, 0.0, 1.0) * segments.size();
        int index = Math.min(segments.size() - 1, (int) Math.floor(scaled));
        return segments.get(index).pointAt(Math.min(1.0, Math.max(0.0, scaled - index)));
    }

    private static Segment defaultSegment(Vec2 start, Vec2 end) {
        double control = Math.max(MIN_DEFAULT_CONTROL, Math.abs(end.x() - start.x()) * 0.45);
        return new Segment(start, new Vec2(start.x() + control, start.y()), new Vec2(end.x() - control, end.y()), end);
    }

    private static Segment catmullRomSegment(List<Vec2> points, int index) {
        Vec2 start = points.get(index);
        Vec2 end = points.get(index + 1);
        Vec2 previous = index == 0 ? start : points.get(index - 1);
        Vec2 next = index + 2 >= points.size() ? end : points.get(index + 2);
        return new Segment(start, start.add(end.subtract(previous).scale(1.0 / 6.0)), end.subtract(next.subtract(start).scale(1.0 / 6.0)), end);
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    public record Segment(Vec2 start, Vec2 controlA, Vec2 controlB, Vec2 end) {
        public Vec2 pointAt(double t) {
            double v = clamp(t, 0.0, 1.0);
            double u = 1.0 - v;
            return start.scale(u * u * u).add(controlA.scale(3.0 * u * u * v)).add(controlB.scale(3.0 * u * v * v)).add(end.scale(v * v * v));
        }
    }

    public record Hit(Vec2 point, double distance, int segmentIndex, double segmentT, double pathT) { }
}
