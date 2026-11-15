package com.exetacticals.client.model.gltf.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.exetacticals.client.model.MeshModelData;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自带的 glTF 加载器（替代 jME3）。
 *
 * <p>规则与原先基于 jME3 的实现保持一致：</p>
 * <ul>
 *     <li>骨骼 = 第一个 skin 的 joints，编号就是它在 joints 数组里的下标；</li>
 *     <li>骨骼初始姿态 = 节点自身的 translation / rotation（scale 忽略）；</li>
 *     <li>每个顶点归属权重最大的骨骼，并变换到该骨骼的局部空间；</li>
 *     <li>UV 原样使用（glTF 与 Minecraft 都是左上角为原点）；</li>
 *     <li>挂在骨骼节点自身上的 mesh 会被忽略（jME3 同样不读取它们）。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class GltfModelLoader {

    /**
     * 从指定的资源位置加载模型，并返回纯粹的mesh定义，不具备渲染
     * 此方法主要用于加载包含蒙皮的gltf模型
     *
     * @param location 模型的资源位置
     * @return 返回模型的数据结构，如果模型没有蒙皮则返回null
     * */
    public static MeshModelData loadModel(ResourceLocation location) {
        GltfDocument doc;
        try {
            doc = GltfDocument.read(location);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read gltf model: " + location, e);
        }
        return loadModel(doc);
    }

    /** 从已解析的文档构建模型数据 */
    public static MeshModelData loadModel(GltfDocument doc) {
        ResourceLocation location = doc.getLocation();
        JsonArray skins = doc.array("skins");
        if (skins.isEmpty()) {
            System.err.println("Skinning UnFound, please check your model: " + location);
            return null;
        }
        JsonArray jointNodes = skins.get(0).getAsJsonObject().getAsJsonArray("joints");

        // 节点下标 -> 骨骼编号
        Map<Integer, Integer> nodeToJoint = new HashMap<>();
        for (int i = 0; i < jointNodes.size(); i++) {
            nodeToJoint.put(jointNodes.get(i).getAsInt(), i);
        }
        // 骨骼编号 -> 父骨骼编号
        Map<Integer, Integer> parentOf = new HashMap<>();
        for (int i = 0; i < jointNodes.size(); i++) {
            JsonArray children = doc.node(jointNodes.get(i).getAsInt()).getAsJsonArray("children");
            if (children == null) {
                continue;
            }
            for (JsonElement child : children) {
                Integer childJoint = nodeToJoint.get(child.getAsInt());
                if (childJoint != null) {
                    parentOf.put(childJoint, i);
                }
            }
        }

        MeshModelData root = null;
        Map<Integer, MeshModelData> boneMap = new HashMap<>();
        for (int i = 0; i < jointNodes.size(); i++) {
            int nodeIndex = jointNodes.get(i).getAsInt();
            JsonObject node = doc.node(nodeIndex);
            String name = node.has("name") ? node.get("name").getAsString() : "Joint_" + nodeIndex;
            MeshModelData bone = new MeshModelData();

            // 设置根骨骼
            if ("root".equals(name)) {
                root = bone;
            }
            boneMap.put(i, bone);

            // 处理父子关系
            Integer parent = parentOf.get(i);
            if (parent != null) {
                MeshModelData parentPart = boneMap.get(parent);
                if (parentPart == null) {
                    parentPart = new MeshModelData();
                    boneMap.put(parent, parentPart);
                }
                parentPart.addChild(name, bone);
            }

            // 设置骨骼的初始姿态
            Vector3f translation = new Vector3f();
            Quaternionf rotation = new Quaternionf();
            readTransform(node, translation, rotation);
            float[] angles = quaternionToEulerZYX(rotation);
            bone.setPose(PartPose.offsetAndRotation(
                    translation.x, translation.y, translation.z,
                    angles[0], angles[1], angles[2]));
        }

        if (root == null) {
            throw new RuntimeException("'root' bone unfound, please check your model: " + location);
        }

        // 构建旋转轴点
        Map<MeshModelData, Pair<Matrix4f, Matrix3f>> pivots = new HashMap<>();
        buildPivots(root, pivots, new PoseStack());

        // 收集场景里所有带 mesh 的普通节点（骨骼节点上的 mesh 不读取）
        List<Integer> meshNodes = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        JsonArray scenes = doc.array("scenes");
        if (!scenes.isEmpty()) {
            int sceneIndex = GltfDocument.intOr(doc.getRoot(), "scene", 0);
            JsonArray sceneNodes = scenes.get(sceneIndex).getAsJsonObject().getAsJsonArray("nodes");
            if (sceneNodes != null) {
                for (JsonElement n : sceneNodes) {
                    collectMeshNodes(doc, n.getAsInt(), nodeToJoint, visited, meshNodes);
                }
            }
        }
        for (int i = 0; i < jointNodes.size(); i++) {
            collectMeshNodes(doc, jointNodes.get(i).getAsInt(), nodeToJoint, visited, meshNodes);
        }

        for (int nodeIndex : meshNodes) {
            int meshIndex = doc.node(nodeIndex).get("mesh").getAsInt();
            JsonArray primitives = doc.array("meshes").get(meshIndex).getAsJsonObject().getAsJsonArray("primitives");
            for (JsonElement primitive : primitives) {
                loadPrimitive(doc, primitive.getAsJsonObject(), boneMap, pivots);
            }
        }

        return root;
    }

    private static void collectMeshNodes(GltfDocument doc, int nodeIndex, Map<Integer, Integer> nodeToJoint,
                                         Set<Integer> visited, List<Integer> out) {
        if (!visited.add(nodeIndex)) {
            return;
        }
        JsonObject node = doc.node(nodeIndex);
        if (node.has("mesh") && !nodeToJoint.containsKey(nodeIndex)) {
            out.add(nodeIndex);
        }
        JsonArray children = node.getAsJsonArray("children");
        if (children != null) {
            for (JsonElement child : children) {
                collectMeshNodes(doc, child.getAsInt(), nodeToJoint, visited, out);
            }
        }
    }

    private static void readTransform(JsonObject node, Vector3f translation, Quaternionf rotation) {
        JsonArray matrix = node.getAsJsonArray("matrix");
        if (matrix != null) {
            float[] m = new float[16];
            for (int i = 0; i < 16; i++) {
                m[i] = matrix.get(i).getAsFloat();
            }
            Matrix4f mat = new Matrix4f().set(m); // glTF 与 JOML 都是列主序
            mat.getTranslation(translation);
            mat.getNormalizedRotation(rotation);
            return;
        }
        JsonArray t = node.getAsJsonArray("translation");
        if (t != null) {
            translation.set(t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat());
        }
        JsonArray r = node.getAsJsonArray("rotation");
        if (r != null) {
            rotation.set(r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat(), r.get(3).getAsFloat());
        }
    }

    /**
     * 返回单位：弧度
     * */
    private static float[] quaternionToEulerZYX(Quaternionf q) {
        float x = q.x;
        float y = q.y;
        float z = q.z;
        float w = q.w;

        // 以下按 ZYX 顺序解析欧拉角
        float sinRCosP = 2 * (w * x + y * z);
        float cosRCosP = 1 - 2 * (x * x + y * y);
        float rollX = (float) Math.atan2(sinRCosP, cosRCosP);

        float sinP = 2 * (w * y - z * x);
        float pitchY;
        if (Math.abs(sinP) >= 1)
            pitchY = (float) (Math.copySign(Math.PI / 2, sinP));
        else
            pitchY = (float) Math.asin(sinP);

        float sinYCosP = 2 * (w * z + x * y);
        float cosYCosP = 1 - 2 * (y * y + z * z);
        float yawZ = (float) Math.atan2(sinYCosP, cosYCosP);

        return new float[] {rollX, pitchY, yawZ};
    }

    /**
     * 递归构建骨骼的轴点：保存每个骨骼累积变换的逆矩阵（以及对应的法线矩阵），
     * 用来把模型空间的顶点变换到骨骼的局部空间。
     */
    protected static void buildPivots(MeshModelData bone, Map<MeshModelData, Pair<Matrix4f, Matrix3f>> pivots, PoseStack poseStack) {
        poseStack.pushPose();
        bone.translateAndRotate(poseStack);
        Matrix4f invert = new Matrix4f(poseStack.last().pose()).invert();
        Matrix3f normal = new Matrix3f();
        invert.normal(normal); // inverse-transpose 3x3
        pivots.put(bone, Pair.of(invert, normal));
        for (MeshModelData child : bone.getChildren().values()) {
            buildPivots(child, pivots, poseStack);
        }
        poseStack.popPose();
    }

    /**
     * 加载一个 primitive 的三角形，每个顶点归入权重最大的骨骼并变换到该骨骼的局部空间。
     */
    protected static void loadPrimitive(
            GltfDocument doc,
            JsonObject primitive,
            Map<Integer, MeshModelData> boneMap,
            Map<MeshModelData, Pair<Matrix4f, Matrix3f>> pivotPoseMap) {
        ResourceLocation location = doc.getLocation();
        int mode = GltfDocument.intOr(primitive, "mode", 4);
        if (mode != 4) {
            throw new IllegalStateException("Only triangle primitives are supported (mode " + mode + "): " + location);
        }
        JsonObject attributes = primitive.getAsJsonObject("attributes");
        float[] pos = doc.readFloats(require(attributes, "POSITION", location));
        float[] nor = doc.readFloats(require(attributes, "NORMAL", location));
        float[] uv = doc.readFloats(require(attributes, "TEXCOORD_0", location));

        // JOINTS_n / WEIGHTS_n，每组 4 个
        List<int[]> jointSets = new ArrayList<>();
        List<float[]> weightSets = new ArrayList<>();
        for (int set = 0; attributes.has("JOINTS_" + set) && attributes.has("WEIGHTS_" + set); set++) {
            jointSets.add(doc.readInts(attributes.get("JOINTS_" + set).getAsInt()));
            weightSets.add(doc.readFloats(attributes.get("WEIGHTS_" + set).getAsInt()));
        }
        if (jointSets.isEmpty()) {
            throw new IllegalStateException("Mesh without JOINTS_0/WEIGHTS_0 (not skinned): " + location);
        }

        int[] indices;
        if (primitive.has("indices")) {
            indices = doc.readInts(primitive.get("indices").getAsInt());
        } else {
            indices = new int[pos.length / 3];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
        }

        final Vector4f tempPos = new Vector4f();
        final Vector3f tempNor = new Vector3f();
        for (int vertexIndex : indices) {
            float x = pos[vertexIndex * 3];
            float y = pos[vertexIndex * 3 + 1];
            float z = pos[vertexIndex * 3 + 2];

            float nx = nor[vertexIndex * 3];
            float ny = nor[vertexIndex * 3 + 1];
            float nz = nor[vertexIndex * 3 + 2];

            float u = uv[vertexIndex * 2];
            float v = uv[vertexIndex * 2 + 1];

            // 找到权重最大的骨骼（权重相同取靠前的；全为 0 时与旧实现一致退回到 1 号骨骼）
            float maxWeight = Float.MIN_VALUE;
            int fineBoneIndex = 1;
            for (int s = 0; s < jointSets.size(); s++) {
                int[] joints = jointSets.get(s);
                float[] weights = weightSets.get(s);
                for (int j = 0; j < 4; j++) {
                    float boneWeight = weights[vertexIndex * 4 + j];
                    if (boneWeight > maxWeight) {
                        fineBoneIndex = joints[vertexIndex * 4 + j];
                        maxWeight = boneWeight;
                    }
                }
            }

            MeshModelData bone = boneMap.get(fineBoneIndex);
            if (bone == null) {
                throw new IllegalStateException("Vertex references unknown joint " + fineBoneIndex + ": " + location);
            }

            Pair<Matrix4f, Matrix3f> pose = pivotPoseMap.get(bone);
            if (pose != null) {
                tempPos.set(x, y, z, 1.0f);
                pose.getLeft().transform(tempPos);
                x = tempPos.x;
                y = tempPos.y;
                z = tempPos.z;

                tempNor.set(nx, ny, nz);
                pose.getRight().transform(tempNor);
                tempNor.normalize();
                nx = tempNor.x;
                ny = tempNor.y;
                nz = tempNor.z;
            }

            bone.pushVertex(x, y, z, u, v, nx, ny, nz, vertexIndex);
        }
    }

    private static int require(JsonObject attributes, String name, ResourceLocation location) {
        JsonElement element = attributes.get(name);
        if (element == null) {
            throw new IllegalStateException("Missing " + name + " attribute: " + location);
        }
        return element.getAsInt();
    }
}
