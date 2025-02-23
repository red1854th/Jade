package mcp.mobius.waila.network;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.gson.Gson;

import mcp.mobius.waila.Waila;
import mcp.mobius.waila.impl.ObjectDataCenter;
import mcp.mobius.waila.impl.config.ConfigEntry;
import mcp.mobius.waila.impl.config.PluginConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

public class ServerPingPacket {

	public Map<ResourceLocation, Boolean> forcedKeys = Maps.newHashMap();

	public ServerPingPacket(@Nullable Map<ResourceLocation, Boolean> forcedKeys) {
		this.forcedKeys = forcedKeys;
	}

	public ServerPingPacket(PluginConfig config) {
		Set<ConfigEntry> entries = config.getSyncableConfigs();
		entries.forEach(e -> forcedKeys.put(e.getId(), e.getValue()));
	}

	public static ServerPingPacket read(FriendlyByteBuf buffer) {
		int size = buffer.readInt();
		Map<ResourceLocation, Boolean> temp = Maps.newHashMap();
		for (int i = 0; i < size; i++) {
			ResourceLocation id = new ResourceLocation(buffer.readUtf(128));
			boolean value = buffer.readBoolean();
			temp.put(id, value);
		}

		return new ServerPingPacket(temp);
	}

	public static void write(ServerPingPacket message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.forcedKeys.size());
		message.forcedKeys.forEach((k, v) -> {
			buffer.writeUtf(k.toString());
			buffer.writeBoolean(v);
		});
	}

	public static class Handler {

		public static void onMessage(ServerPingPacket message, Supplier<NetworkEvent.Context> context) {
			context.get().enqueueWork(() -> {
				ObjectDataCenter.serverConnected = true;
				message.forcedKeys.forEach(PluginConfig.INSTANCE::set);
				Waila.LOGGER.info("Received config from the server: {}", new Gson().toJson(message.forcedKeys));
			});
			context.get().setPacketHandled(true);
		}
	}
}
