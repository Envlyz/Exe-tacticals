package com.exetacticals.network.s2c;

import com.exetacticals.client.render.fx.BulletImpactDecals;
import com.exetacticals.network.IPacket;
import com.exetacticals.network.PacketContext;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public final class BulletImpactPacket implements IPacket<BulletImpactPacket> {
    private final double x;
    private final double y;
    private final double z;
    private final Direction face;

    public BulletImpactPacket(Vec3 hit, Direction face) {
        this(hit.x, hit.y, hit.z, face);
    }

    private BulletImpactPacket(double x, double y, double z, Direction face) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    public static BulletImpactPacket decode(FriendlyByteBuf buf) {
        return new BulletImpactPacket(buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readEnum(Direction.class));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeEnum(face);
    }

    @Override
    public void onClient(BulletImpactPacket packet, PacketContext context) {
        context.enqueueWork(() -> BulletImpactDecals.record(new Vec3(packet.x, packet.y, packet.z), packet.face));
    }

    @Override
    public void onServer(BulletImpactPacket packet, PacketContext context) {
    }
}
