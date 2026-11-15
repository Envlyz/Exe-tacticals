package com.exetacticals.network;
public interface IPacket<T> {
    void onClient(T packet, PacketContext context);
    void onServer(T packet, PacketContext context);
}
