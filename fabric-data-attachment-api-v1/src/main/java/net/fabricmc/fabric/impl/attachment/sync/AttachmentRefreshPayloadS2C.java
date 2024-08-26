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

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/*
 * Empty optional: initial ping, client responds with the attachments it is aware of and clears
 * Optional present: response to the client, sending all of the attachments that it should actually know about
 */
public record AttachmentRefreshPayloadS2C(Optional<List<AttachmentChange>> attachments) implements CustomPayload {
	public static final PacketCodec<RegistryByteBuf, AttachmentRefreshPayloadS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.optional(AttachmentChange.LIST_PACKET_CODEC), AttachmentRefreshPayloadS2C::attachments,
			AttachmentRefreshPayloadS2C::new
	);
	public static final Id<AttachmentRefreshPayloadS2C> ID = new Id<>(AttachmentSyncImpl.REFRESH_PACKET_ID);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
