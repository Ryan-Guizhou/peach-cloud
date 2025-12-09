package com.peach.redis.bloom.core;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 命名空间级状态模型：包含段状态、近似计数、容量、综合 FPP 等。
 */
public class BloomStatus {

    public static class SegmentStatus {
        public String name;
        public int index;
        public long capacity;
        public long countApprox;
        public double load; // count / capacity
        public double fpp;

        @Override
        public String toString() {
            return String.format(
                    "SegmentStatus{name='%s', index=%d, capacity=%,d, countApprox=%,d, load=%.2f%%, fpp=%.4f%%}",
                    name, index, capacity, countApprox, load * 100, fpp * 100
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SegmentStatus that = (SegmentStatus) o;
            return index == that.index &&
                    capacity == that.capacity &&
                    countApprox == that.countApprox &&
                    Double.compare(that.load, load) == 0 &&
                    Double.compare(that.fpp, fpp) == 0 &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, index, capacity, countApprox, load, fpp);
        }
    }

    public String namespace;
    public int segments;
    public String tailSegment;
    public long totalCapacity;
    public long totalCountApprox;
    public double effectiveFpp; // 1 - Π(1 - fpp_i)
    public List<SegmentStatus> segmentStatuses;

    @Override
    public String toString() {
        String segmentInfo = segmentStatuses != null ?
                segmentStatuses.stream()
                        .map(SegmentStatus::toString)
                        .collect(Collectors.joining(", ", "[", "]")) :
                "null";

        return String.format(
                "BloomStatus{namespace='%s', segments=%d, tailSegment='%s', " +
                        "totalCapacity=%,d, totalCountApprox=%,d, effectiveFpp=%.4f%%, " +
                        "segmentStatuses=%s}",
                namespace, segments, tailSegment,
                totalCapacity, totalCountApprox, effectiveFpp * 100,
                segmentInfo
        );
    }

    // 简洁版本，适合日志输出
    public String toSimpleString() {
        return String.format(
                "BloomStatus[%s]: %d segments, %,d/%,d items (%.1f%%), FPP=%.4f%%",
                namespace, segments, totalCountApprox, totalCapacity,
                (totalCapacity > 0 ? (double) totalCountApprox / totalCapacity * 100 : 0),
                effectiveFpp * 100
        );
    }

    // 详细版本，包含所有段信息
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Bloom Status for '%s'%n", namespace));
        sb.append(String.format("  Segments: %d, Tail: %s%n", segments, tailSegment));
        sb.append(String.format("  Total Capacity: %,d%n", totalCapacity));
        sb.append(String.format("  Total Count: %,d (%.1f%%)%n",
                totalCountApprox,
                totalCapacity > 0 ? (double) totalCountApprox / totalCapacity * 100 : 0
        ));
        sb.append(String.format("  Effective FPP: %.4f%%%n", effectiveFpp * 100));

        if (segmentStatuses != null && !segmentStatuses.isEmpty()) {
            sb.append("  Segment Details:\n");
            for (SegmentStatus segment : segmentStatuses) {
                sb.append(String.format("    - %s: %,d/%,d (%.1f%%), FPP=%.4f%%%n",
                        segment.name, segment.countApprox, segment.capacity,
                        segment.load * 100, segment.fpp * 100
                ));
            }
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BloomStatus that = (BloomStatus) o;
        return segments == that.segments &&
                totalCapacity == that.totalCapacity &&
                totalCountApprox == that.totalCountApprox &&
                Double.compare(that.effectiveFpp, effectiveFpp) == 0 &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(tailSegment, that.tailSegment) &&
                Objects.equals(segmentStatuses, that.segmentStatuses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, segments, tailSegment, totalCapacity, totalCountApprox, effectiveFpp, segmentStatuses);
    }
}