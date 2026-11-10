package dev.taezeem14.cigarette.mixin.impl;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.impl.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void onChannelRead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            PacketEvent event = new PacketEvent(PacketEvent.Direction.INCOMING, packet);
            Cigarette.INSTANCE.getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            PacketEvent event = new PacketEvent(PacketEvent.Direction.OUTGOING, packet);
            Cigarette.INSTANCE.getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
