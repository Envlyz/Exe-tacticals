package com.exetacticals.modularSys.util;

import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class OBB {
    public Vector3f center;
    public Vector3f[] axes = new Vector3f[3];
    public float[] halfExtents = new float[3];

    public static OBB fromAABB(AABB box, Matrix4f transform) {
        Vector3f[] corners = new Vector3f[8];
        corners[0] = new Vector3f((float) box.minX, (float) box.minY, (float) box.minZ);
        corners[1] = new Vector3f((float) box.maxX, (float) box.minY, (float) box.minZ);
        corners[2] = new Vector3f((float) box.minX, (float) box.maxY, (float) box.minZ);
        corners[3] = new Vector3f((float) box.maxX, (float) box.maxY, (float) box.minZ);
        corners[4] = new Vector3f((float) box.minX, (float) box.minY, (float) box.maxZ);
        corners[5] = new Vector3f((float) box.maxX, (float) box.minY, (float) box.maxZ);
        corners[6] = new Vector3f((float) box.minX, (float) box.maxY, (float) box.maxZ);
        corners[7] = new Vector3f((float) box.maxX, (float) box.maxY, (float) box.maxZ);

        for (int i = 0; i < 8; i++) {
            corners[i] = corners[i].mulPosition(transform);
        }

        Vector3f center = new Vector3f();
        for (Vector3f c : corners) {
            center.add(c);
        }
        center.mul(1f / 8f);

        Vector3f xAxis = new Vector3f(corners[1]).sub(corners[0]).normalize();
        Vector3f yAxis = new Vector3f(corners[2]).sub(corners[0]).normalize();
        Vector3f zAxis = new Vector3f(corners[4]).sub(corners[0]).normalize();

        float hx = corners[0].distance(corners[1]) / 2f;
        float hy = corners[0].distance(corners[2]) / 2f;
        float hz = corners[0].distance(corners[4]) / 2f;

        OBB obb = new OBB();
        obb.center = center;
        obb.axes[0] = xAxis;
        obb.axes[1] = yAxis;
        obb.axes[2] = zAxis;
        obb.halfExtents[0] = hx;
        obb.halfExtents[1] = hy;
        obb.halfExtents[2] = hz;
        return obb;
    }

    public boolean intersects(OBB other) {
        Vector3f[] axesToTest = new Vector3f[15];
        System.arraycopy(this.axes, 0, axesToTest, 0, 3);
        System.arraycopy(other.axes, 0, axesToTest, 3, 3);

        int index = 6;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                axesToTest[index++] = new Vector3f(this.axes[i]).cross(other.axes[j]);
            }
        }

        for (Vector3f axis : axesToTest) {
            if (axis.lengthSquared() < 1e-8f) continue;
            if (!overlapOnAxis(this, other, axis.normalize())) {
                return false;
            }
        }
        return true;
    }

    public boolean intersects(Vector3f point) {
        Vector3f d = new Vector3f(point).sub(center);

        for (int i = 0; i < 3; i++) {
            float dist = d.dot(axes[i]);
            if (Math.abs(dist) > halfExtents[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean intersectsRay(Vector3f rayOrigin, Vector3f rayDir) {
        return intersectsRayDistance(rayOrigin, rayDir) >= 0f;
    }

    public float intersectsRayDistance(Vector3f rayOrigin, Vector3f rayDir) {
        Vector3f p = new Vector3f(center).sub(rayOrigin);

        float tMin = Float.NEGATIVE_INFINITY;
        float tMax = Float.POSITIVE_INFINITY;

        for (int i = 0; i < 3; i++) {
            Vector3f axis = axes[i];

            float e = axis.dot(p);
            float f = axis.dot(rayDir);

            if (Math.abs(f) < 1e-6f) {
                if (-e - halfExtents[i] > 0f || -e + halfExtents[i] < 0f) {
                    return -1f;
                }
                continue;
            }

            float t1 = (e + halfExtents[i]) / f;
            float t2 = (e - halfExtents[i]) / f;

            if (t1 > t2) {
                float tmp = t1;
                t1 = t2;
                t2 = tmp;
            }

            if (t1 > tMin) tMin = t1;
            if (t2 < tMax) tMax = t2;

            if (tMin > tMax) {
                return -1f;
            }

            if (tMax < 0f) {
                return -1f;
            }
        }

        if (tMin < 0f) {
            return tMax;
        }

        return tMin;
    }

    private static boolean overlapOnAxis(OBB a, OBB b, Vector3f axis) {
        float aProj = projectRadius(a, axis);
        float bProj = projectRadius(b, axis);
        float distance = Math.abs(a.center.dot(axis) - b.center.dot(axis));
        return distance <= (aProj + bProj);
    }

    private static float projectRadius(OBB obb, Vector3f axis) {
        return obb.halfExtents[0] * Math.abs(axis.dot(obb.axes[0])) +
                obb.halfExtents[1] * Math.abs(axis.dot(obb.axes[1])) +
                obb.halfExtents[2] * Math.abs(axis.dot(obb.axes[2]));
    }
}
