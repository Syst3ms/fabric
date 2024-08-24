/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.impl.attachment.sync;

import java.util.List;
import java.util.Optional;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentEntrypoint;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentTypeImpl;

// empty optional for removal
@SuppressWarnings("unchecked")
public record AttachmentChange(AttachmentTargetInfo<?> targetInfo, AttachmentType<?> type, @Nullable Object data) {
	public static final PacketCodec<PacketByteBuf, AttachmentChange> PACKET_CODEC = PacketCodec.ofStatic(
			AttachmentChange::writeToNetwork,
			AttachmentChange::readFromNetwork
	);
	public static final PacketCodec<PacketByteBuf, List<AttachmentChange>> LIST_PACKET_CODEC = PACKET_CODEC.collect(
			PacketCodecs.toList());

	private static void writeToNetwork(PacketByteBuf buf, AttachmentChange value) {
		Identifier id = value.type.identifier();

		if (!AttachmentSync.CLIENT_SUPPORTED_ATTACHMENTS.get().contains(id)) {
			AttachmentEntrypoint.LOGGER.warn(
					"Attachment type '{}' does not exist on client, skipping",
					id.toString()
			);
			return;
		}

		PacketCodec<PacketByteBuf, Object> packetCodec =
				(PacketCodec<PacketByteBuf, Object>) ((AttachmentTypeImpl<?>) value.type).packetCodec();

		if (packetCodec == null) {
			AttachmentEntrypoint.LOGGER.warn("Attachment type '{}' has no packet codec, skipping", id.toString());
			return;
		}

		AttachmentTargetInfo.PACKET_CODEC.encode(buf, value.targetInfo);
		buf.writeIdentifier(id);
		buf.writeOptional(Optional.ofNullable(value.data), packetCodec);
	}

	private static AttachmentChange readFromNetwork(PacketByteBuf buf) {
		AttachmentTargetInfo<?> target = AttachmentTargetInfo.PACKET_CODEC.decode(buf);
		Identifier id = buf.readIdentifier();
		AttachmentType<?> type = AttachmentRegistryImpl.get(id);

		if (type == null) {
			// Can't skip as we don't know the size of the data
			throw new DecoderException("Unknown attachment type '" + id.toString() + "'");
		}

		PacketCodec<PacketByteBuf, Object> packetCodec =
				(PacketCodec<PacketByteBuf, Object>) ((AttachmentTypeImpl<?>) type).packetCodec();

		if (packetCodec == null) {
			throw new EncoderException("Attachment type '" + id.toString() + "' has no packet codec, skipping");
		}

		return new AttachmentChange(target, type, buf.readOptional(packetCodec).orElse(null));
	}

	public void apply(World world) {
		targetInfo.getTarget(world).setAttached((AttachmentType<Object>) type, data);
	}
}
