package com.exetacticals.client.model.gltf.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.exetacticals.client.aiStuff.DevAssetResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 最小的 glTF 2.0 (.gltf, JSON) 读取器，只实现模型加载需要的部分：
 * 节点、网格、蒙皮和 accessor 数据（base64 内嵌或相对路径的外部 .bin）。
 */
@OnlyIn(Dist.CLIENT)
public class GltfDocument {
    public static final int BYTE = 5120;
    public static final int UNSIGNED_BYTE = 5121;
    public static final int SHORT = 5122;
    public static final int UNSIGNED_SHORT = 5123;
    public static final int UNSIGNED_INT = 5125;
    public static final int FLOAT = 5126;

    private final ResourceLocation location;
    private final JsonObject root;
    private final ByteBuffer[] buffers;

    private GltfDocument(ResourceLocation location, JsonObject root) throws IOException {
        this.location = location;
        this.root = root;
        JsonArray bufferArray = array("buffers");
        this.buffers = new ByteBuffer[bufferArray.size()];
        for (int i = 0; i < bufferArray.size(); i++) {
            buffers[i] = readBuffer(bufferArray.get(i).getAsJsonObject());
        }
    }

    public static GltfDocument read(ResourceLocation location) throws IOException {
        try (InputStream stream = DevAssetResolver.openOrNull(location, Minecraft.getInstance().getResourceManager())) {
            if (stream == null) {
                throw new IOException("Resource not found: " + location);
            }
            return parse(location, stream);
        }
    }

    /** 从已经打开的流解析（不关闭流）。 */
    public static GltfDocument parse(ResourceLocation location, InputStream stream) throws IOException {
        JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        return new GltfDocument(location, json);
    }

    public JsonArray array(String name) {
        JsonArray array = root.getAsJsonArray(name);
        return array == null ? new JsonArray() : array;
    }

    public JsonObject node(int index) {
        return array("nodes").get(index).getAsJsonObject();
    }

    public JsonObject getRoot() {
        return root;
    }

    private ByteBuffer readBuffer(JsonObject buffer) throws IOException {
        String uri = buffer.has("uri") ? buffer.get("uri").getAsString() : null;
        if (uri == null) {
            throw new IOException("glb / uri-less buffers are not supported: " + location);
        }
        byte[] bytes;
        if (uri.startsWith("data:")) {
            int comma = uri.indexOf(',');
            bytes = Base64.getDecoder().decode(uri.substring(comma + 1));
        } else {
            String path = location.getPath();
            int slash = path.lastIndexOf('/');
            String dir = slash < 0 ? "" : path.substring(0, slash + 1);
            ResourceLocation binLocation = new ResourceLocation(location.getNamespace(), dir + uri);
            try (InputStream stream = DevAssetResolver.openOrNull(binLocation, Minecraft.getInstance().getResourceManager())) {
                if (stream == null) {
                    throw new IOException("Buffer not found: " + binLocation);
                }
                bytes = stream.readAllBytes();
            }
        }
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    /** accessor 的元素数量 */
    public int count(int accessorIndex) {
        return array("accessors").get(accessorIndex).getAsJsonObject().get("count").getAsInt();
    }

    /** 每个元素的分量数（SCALAR=1, VEC2=2 ...） */
    public int components(int accessorIndex) {
        String type = array("accessors").get(accessorIndex).getAsJsonObject().get("type").getAsString();
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4", "MAT2" -> 4;
            case "MAT3" -> 9;
            case "MAT4" -> 16;
            default -> throw new IllegalArgumentException("Unknown accessor type " + type + " in " + location);
        };
    }

    /** 把 accessor 读成 float 数组（整数类型按原值读取，normalized 时按规范归一化）。 */
    public float[] readFloats(int accessorIndex) {
        JsonObject accessor = array("accessors").get(accessorIndex).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int components = components(accessorIndex);
        int componentType = accessor.get("componentType").getAsInt();
        boolean normalized = accessor.has("normalized") && accessor.get("normalized").getAsBoolean();
        float[] out = new float[count * components];
        if (!accessor.has("bufferView")) {
            return out; // 规范：没有 bufferView 时数据全为 0
        }
        View view = view(accessor, componentType, components);
        for (int i = 0; i < count; i++) {
            int base = view.offset + i * view.stride;
            for (int c = 0; c < components; c++) {
                int at = base + c * componentSize(componentType);
                float value = switch (componentType) {
                    case FLOAT -> view.buffer.getFloat(at);
                    case UNSIGNED_BYTE -> normalized ? (view.buffer.get(at) & 0xFF) / 255f : (view.buffer.get(at) & 0xFF);
                    case BYTE -> normalized ? Math.max(view.buffer.get(at) / 127f, -1f) : view.buffer.get(at);
                    case UNSIGNED_SHORT -> normalized ? (view.buffer.getShort(at) & 0xFFFF) / 65535f : (view.buffer.getShort(at) & 0xFFFF);
                    case SHORT -> normalized ? Math.max(view.buffer.getShort(at) / 32767f, -1f) : view.buffer.getShort(at);
                    case UNSIGNED_INT -> (float) (view.buffer.getInt(at) & 0xFFFFFFFFL);
                    default -> throw new IllegalArgumentException("Unknown componentType " + componentType + " in " + location);
                };
                out[i * components + c] = value;
            }
        }
        return out;
    }

    /** 把整数类型的 accessor（索引、骨骼编号）读成 int 数组。 */
    public int[] readInts(int accessorIndex) {
        JsonObject accessor = array("accessors").get(accessorIndex).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int components = components(accessorIndex);
        int componentType = accessor.get("componentType").getAsInt();
        int[] out = new int[count * components];
        if (!accessor.has("bufferView")) {
            return out;
        }
        View view = view(accessor, componentType, components);
        for (int i = 0; i < count; i++) {
            int base = view.offset + i * view.stride;
            for (int c = 0; c < components; c++) {
                int at = base + c * componentSize(componentType);
                out[i * components + c] = switch (componentType) {
                    case UNSIGNED_BYTE -> view.buffer.get(at) & 0xFF;
                    case BYTE -> view.buffer.get(at);
                    case UNSIGNED_SHORT -> view.buffer.getShort(at) & 0xFFFF;
                    case SHORT -> view.buffer.getShort(at);
                    case UNSIGNED_INT -> view.buffer.getInt(at);
                    case FLOAT -> (int) view.buffer.getFloat(at);
                    default -> throw new IllegalArgumentException("Unknown componentType " + componentType + " in " + location);
                };
            }
        }
        return out;
    }

    private View view(JsonObject accessor, int componentType, int components) {
        JsonObject bufferView = array("bufferViews").get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
        ByteBuffer buffer = buffers[bufferView.get("buffer").getAsInt()];
        int offset = intOr(bufferView, "byteOffset", 0) + intOr(accessor, "byteOffset", 0);
        int stride = intOr(bufferView, "byteStride", 0);
        if (stride == 0) {
            stride = componentSize(componentType) * components;
        }
        return new View(buffer, offset, stride);
    }

    private static int componentSize(int componentType) {
        return switch (componentType) {
            case BYTE, UNSIGNED_BYTE -> 1;
            case SHORT, UNSIGNED_SHORT -> 2;
            default -> 4;
        };
    }

    public static int intOr(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element == null ? fallback : element.getAsInt();
    }

    public ResourceLocation getLocation() {
        return location;
    }

    private record View(ByteBuffer buffer, int offset, int stride) {}
}
