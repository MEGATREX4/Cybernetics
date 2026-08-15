package com.perigrine3.createcybernetics.network;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.client.render.CyberentitySandevistanMirageTrail;
import com.perigrine3.createcybernetics.client.render.SandevistanMirageTrail;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.effect.*;
import com.perigrine3.createcybernetics.event.custom.SandevistanSnapshotRelay;
import com.perigrine3.createcybernetics.network.handler.*;
import com.perigrine3.createcybernetics.network.payload.*;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(
                SculkLungsEffect.SonicUseHeldPayload.TYPE,
                SculkLungsEffect.SonicUseHeldPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SculkLungsEffect.setUseHeld(sp, payload.held());
                    }
                })
        );

        r.playToServer(
                GuardianEyeEffect.GuardianEyeUseHeldPayload.TYPE,
                GuardianEyeEffect.GuardianEyeUseHeldPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        GuardianEyeEffect.setUseHeld(sp, payload.held());
                    }
                })
        );

        r.playToServer(
                NeuralContextualizerEffect.SwapHotbarPayload.TYPE,
                NeuralContextualizerEffect.SwapHotbarPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        NeuralContextualizerEffect.handleSwapHotbarPayload(sp, payload.slot());
                    }
                })
        );

        r.playToClient(
                TargetingHighlightPayload.TYPE,
                TargetingHighlightPayload.STREAM_CODEC,
                TargetingHighlightPayload::handle
        );


        r.playToServer(
                com.perigrine3.createcybernetics.network.payload.OpenExpandedInventoryPayload.TYPE,
                com.perigrine3.createcybernetics.network.payload.OpenExpandedInventoryPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.perigrine3.createcybernetics.network.payload.OpenExpandedInventoryPayload.handle(payload, ctx)
                )
        );

        r.playToServer(
                OpenSpinalInjectorPayload.TYPE,
                OpenSpinalInjectorPayload.STREAM_CODEC,
                OpenSpinalInjectorHandler::handle
        );

        r.playToServer(
                OpenArmCannonPayload.TYPE,
                OpenArmCannonPayload.STREAM_CODEC,
                OpenArmCannonHandler::handle
        );

        r.playToClient(
                EnergyHudSnapshotPayload.TYPE,
                EnergyHudSnapshotPayload.STREAM_CODEC,
                EnergyHudSnapshotPayload::handle
        );


        r.playToServer(
                ArmCannonWheelPayloads.RequestOpenArmCannonWheelPayload.TYPE,
                ArmCannonWheelPayloads.RequestOpenArmCannonWheelPayload.STREAM_CODEC,
                ArmCannonWheelHandlers::handleOpen
        );

        r.playToClient(
                ArmCannonWheelPayloads.OpenArmCannonWheelClientPayload.TYPE,
                ArmCannonWheelPayloads.OpenArmCannonWheelClientPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    ArmCannonWheelClientHandlers.handleOpen(payload, ctx);
                })
        );

        r.playToServer(
                ArmCannonWheelPayloads.SelectArmCannonAmmoSlotPayload.TYPE,
                ArmCannonWheelPayloads.SelectArmCannonAmmoSlotPayload.STREAM_CODEC,
                ArmCannonWheelHandlers::handleSelect
        );

        r.playToServer(
                ArmCannonFirePayload.TYPE,
                ArmCannonFirePayload.STREAM_CODEC,
                ArmCannonFireHandler::handle
        );

        r.playToServer(
                SetChipwareShardPayload.TYPE,
                SetChipwareShardPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;

                    PlayerCyberwareData data = sp.getData(ModAttachments.CYBERWARE);
                    if (data == null) return;
                    int slot = payload.slot();
                    ItemStack stack = payload.stack();
                    if (slot < 0 || slot >= PlayerCyberwareData.CHIPWARE_SLOT_COUNT) return;
                    data.setChipwareStack(slot, stack);

                    data.setDirty();
                    sp.syncData(ModAttachments.CYBERWARE);
                })
        );

        r.playToServer(
                OpenChipwareMiniPayload.TYPE,
                OpenChipwareMiniPayload.STREAM_CODEC,
                OpenChipwareMiniHandler::handle
        );

        r.playToClient(
                CerebralShutdownStatePayload.TYPE,
                CerebralShutdownStatePayload.STREAM_CODEC,
                CerebralShutdownStatePayload::handle
        );


        r.playToClient(
                CopernicusOxygenSyncPayload.TYPE,
                CopernicusOxygenSyncPayload.STREAM_CODEC,
                CopernicusOxygenSyncPayload::handle
        );

        r.playToServer(
                OpenHeatEnginePayload.TYPE,
                OpenHeatEnginePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    OpenHeatEnginePayloadHandler.handle(payload, sp);
                })
        );

        r.playToServer(
                OpenCyberdeckPayload.TYPE,
                OpenCyberdeckPayload.STREAM_CODEC,
                OpenCyberdeckPayloadHandler::handle
        );

        r.playToClient(
                SandevistanSnapshotPayload.TYPE,
                SandevistanSnapshotPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    SandevistanMirageTrail.acceptNetworkSnapshot(payload);
                })
        );

        r.playToServer(
                SandevistanSnapshotC2SPayload.TYPE,
                SandevistanSnapshotC2SPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SandevistanSnapshotRelay.handle(sp, payload);
                    }
                })
        );

        r.playToServer(
                InfologSaveChipwarePayload.TYPE,
                InfologSaveChipwarePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        InfologSaveChipwareHandler.handle(payload, sp);
                    }
                })
        );

        r.playToClient(
                EnergyHudSyncPayload.TYPE,
                EnergyHudSyncPayload.STREAM_CODEC,
                EnergyHudSyncPayload::handle
        );

        r.playToServer(
                CastCyberdeckQuickhackPayload.TYPE,
                CastCyberdeckQuickhackPayload.STREAM_CODEC,
                CastCyberdeckQuickhackHandler::handle
        );

        r.playToClient(
                BehindYouSoundPayload.TYPE,
                BehindYouSoundPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> BehindYouSoundPayload.handle(payload))
        );

        r.playToServer(
                com.perigrine3.createcybernetics.compat.corpse.OpenCorpseCyberwarePayload.TYPE,
                com.perigrine3.createcybernetics.compat.corpse.OpenCorpseCyberwarePayload.STREAM_CODEC,
                com.perigrine3.createcybernetics.compat.corpse.OpenCorpseCyberwarePayload::handle
        );

        r.playToClient(
                com.perigrine3.createcybernetics.compat.corpse.CorpseVisualSnapshotPayload.TYPE,
                com.perigrine3.createcybernetics.compat.corpse.CorpseVisualSnapshotPayload.STREAM_CODEC,
                com.perigrine3.createcybernetics.compat.corpse.CorpseVisualSnapshotPayload::handle
        );

        r.playToServer(
                com.perigrine3.createcybernetics.compat.corpse.RequestCorpseVisualSnapshotPayload.TYPE,
                com.perigrine3.createcybernetics.compat.corpse.RequestCorpseVisualSnapshotPayload.STREAM_CODEC,
                com.perigrine3.createcybernetics.compat.corpse.RequestCorpseVisualSnapshotPayload::handle
        );

        r.playToServer(
                ArcCannonFirePayload.TYPE,
                ArcCannonFirePayload.STREAM_CODEC,
                ArcCannonFirePayload::handle
        );

        r.playToServer(
                BiomonitorScanRequestPayload.TYPE,
                BiomonitorScanRequestPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        BiomonitorScanRequestPayload.handle(payload, sp);
                    }
                })
        );

        r.playToClient(
                BiomonitorVitalsPayload.TYPE,
                BiomonitorVitalsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        BiomonitorVitalsPayload.handle(payload)
                )
        );

        r.playToClient(
                CyberwareRejectionVisualPayload.TYPE,
                CyberwareRejectionVisualPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> CyberwareRejectionVisualPayload.handle(payload))
        );

        r.playToServer(
                NavigationMapPayloads.RequestMapImportPayload.TYPE,
                NavigationMapPayloads.RequestMapImportPayload.STREAM_CODEC,
                NavigationMapPayloads.RequestMapImportPayload::handle
        );

        r.playToClient(
                NavigationMapPayloads.ImportMapPayload.TYPE,
                NavigationMapPayloads.ImportMapPayload.STREAM_CODEC,
                NavigationMapPayloads.ImportMapPayload::handle
        );

        r.playToClient(
                SharedNavigationPlayersPayload.TYPE,
                SharedNavigationPlayersPayload.STREAM_CODEC,
                SharedNavigationPlayersPayload::handle
        );

        r.playToClient(
                SharedNavigationClearPayload.TYPE,
                SharedNavigationClearPayload.STREAM_CODEC,
                SharedNavigationClearPayload::handle
        );

        r.playToServer(
                SharedNavigationTileUploadPayload.TYPE,
                SharedNavigationTileUploadPayload.STREAM_CODEC,
                SharedNavigationTileUploadPayload::handle
        );

        r.playToServer(
                SharedNavigationWaypointUploadPayload.TYPE,
                SharedNavigationWaypointUploadPayload.STREAM_CODEC,
                SharedNavigationWaypointUploadPayload::handle
        );

        r.playToClient(
                SharedNavigationTileSyncPayload.TYPE,
                SharedNavigationTileSyncPayload.STREAM_CODEC,
                SharedNavigationTileSyncPayload::handle
        );

        r.playToClient(
                SharedNavigationWaypointSyncPayload.TYPE,
                SharedNavigationWaypointSyncPayload.STREAM_CODEC,
                SharedNavigationWaypointSyncPayload::handle
        );

        // ---------------- CYBEREYE IRIS LAYOUT SYNC ----------------

// Client -> Server
        r.playToServer(
                CybereyeIrisSyncC2SPayload.TYPE,
                CybereyeIrisSyncC2SPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        CybereyeIrisSyncC2SPayload.handle(payload, sp);
                    }
                })
        );

// Server -> Client
        r.playToClient(
                CybereyeIrisSyncS2CPayload.TYPE,
                CybereyeIrisSyncS2CPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> CybereyeIrisSyncS2CPayload.handle(payload))
        );

        r.playToServer(
                TattooUploadC2SPayload.TYPE,
                TattooUploadC2SPayload.STREAM_CODEC,
                TattooUploadC2SPayload::handle
        );

        r.playToClient(
                TattooListS2CPayload.TYPE,
                TattooListS2CPayload.STREAM_CODEC,
                TattooListS2CPayload::handle
        );

        r.playToServer(
                TattooImageRequestC2SPayload.TYPE,
                TattooImageRequestC2SPayload.STREAM_CODEC,
                TattooImageRequestC2SPayload::handle
        );

        r.playToClient(
                TattooImageDataS2CPayload.TYPE,
                TattooImageDataS2CPayload.STREAM_CODEC,
                TattooImageDataS2CPayload::handle
        );

        r.playToServer(
                TattooApplyC2SPayload.TYPE,
                TattooApplyC2SPayload.STREAM_CODEC,
                TattooApplyC2SPayload::handle
        );

        r.playToClient(
                TattooAccessSyncS2CPayload.TYPE,
                TattooAccessSyncS2CPayload.STREAM_CODEC,
                TattooAccessSyncS2CPayload::handle
        );

        r.playToServer(
                TattooApproveC2SPayload.TYPE,
                TattooApproveC2SPayload.STREAM_CODEC,
                TattooApproveC2SPayload::handle
        );

        r.playToClient(
                TattooPendingListS2CPayload.TYPE,
                TattooPendingListS2CPayload.STREAM_CODEC,
                TattooPendingListS2CPayload::handle
        );

        r.playToServer(
                TattooPendingListRequestC2SPayload.TYPE,
                TattooPendingListRequestC2SPayload.STREAM_CODEC,
                TattooPendingListRequestC2SPayload::handle
        );

        r.playToServer(
                TattooRemoveApprovedC2SPayload.TYPE,
                TattooRemoveApprovedC2SPayload.STREAM_CODEC,
                TattooRemoveApprovedC2SPayload::handle
        );

        r.playToServer(
                TattooRejectC2SPayload.TYPE,
                TattooRejectC2SPayload.STREAM_CODEC,
                TattooRejectC2SPayload::handle
        );

        r.playToClient(
                IronsManaClientSyncPayload.TYPE,
                IronsManaClientSyncPayload.STREAM_CODEC,
                IronsManaClientSyncPayload::handle
        );

        r.playToServer(
                OpenVampyresPayload.TYPE,
                OpenVampyresPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> OpenVampyresPayload.handle(payload, context))
        );

        r.playToServer(
                VampyresBiteHeldPayload.TYPE,
                VampyresBiteHeldPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> VampyresBiteHeldPayload.handle(payload, context))
        );




        /* ---------------- TOGGLE WHEEL PAYLOADS ---------------- */

        // Client asks server: "send me the enabled flags for installed toggleables"
        r.playToServer(
                CyberwareTogglePayloads.RequestToggleStatesPayload.TYPE,
                CyberwareTogglePayloads.RequestToggleStatesPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                    if (!sp.hasData(ModAttachments.CYBERWARE)) return;

                    PlayerCyberwareData data = sp.getData(ModAttachments.CYBERWARE);
                    if (data == null) return;

                    for (var entry : data.getAll().entrySet()) {
                        CyberwareSlot slot = entry.getKey();
                        InstalledCyberware[] arr = entry.getValue();
                        if (arr == null) continue;

                        for (int i = 0; i < arr.length; i++) {
                            InstalledCyberware inst = arr[i];
                            if (inst == null) continue;

                            ItemStack stack = inst.getItem();
                            if (stack == null || stack.isEmpty()) continue;
                            if (!stack.is(ModTags.Items.TOGGLEABLE_CYBERWARE)) continue;

                            boolean enabled = data.isEnabled(slot, i);
                            PacketDistributor.sendToPlayer(sp, new CyberwareEnabledStatePayload(slot.name(), i, enabled));
                        }
                    }
                })
        );

        // Client clicked a segment: toggle that installed (slot,index) on server and sync back
        r.playToServer(
                CyberwareTogglePayloads.ToggleCyberwarePayload.TYPE,
                CyberwareTogglePayloads.ToggleCyberwarePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                    if (!sp.hasData(ModAttachments.CYBERWARE)) return;

                    PlayerCyberwareData data = sp.getData(ModAttachments.CYBERWARE);
                    if (data == null) return;

                    CyberwareSlot slot;
                    try {
                        slot = CyberwareSlot.valueOf(payload.slotName());
                    } catch (IllegalArgumentException ex) {
                        return;
                    }

                    int index = payload.index();
                    InstalledCyberware inst = data.get(slot, index);
                    if (inst == null) return;

                    ItemStack stack = inst.getItem();
                    if (stack == null || stack.isEmpty()) return;
                    if (!stack.is(ModTags.Items.TOGGLEABLE_CYBERWARE)) return;

                    boolean nowEnabled = data.toggleEnabled(slot, index);
                    PacketDistributor.sendToPlayer(sp, new CyberwareEnabledStatePayload(slot.name(), index, nowEnabled));
                })
        );

        r.playToClient(
                CyberwareEnabledStatePayload.TYPE,
                CyberwareEnabledStatePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> CyberwareEnabledStatePayload.handle(payload, ctx))
        );

        r.playToClient(
                PlayerSurgeryStartPayload.TYPE,
                PlayerSurgeryStartPayload.STREAM_CODEC,
                PlayerSurgeryPayloadHandler::handleStart
        );

        r.playToClient(
                PlayerSurgeryRoundPayload.TYPE,
                PlayerSurgeryRoundPayload.STREAM_CODEC,
                PlayerSurgeryPayloadHandler::handleRound
        );

        r.playToServer(
                PlayerSurgeryClickPayload.TYPE,
                PlayerSurgeryClickPayload.STREAM_CODEC,
                PlayerSurgeryPayloadHandler::handleClick
        );

        r.playToClient(
                PlayerSurgeryResultPayload.TYPE,
                PlayerSurgeryResultPayload.STREAM_CODEC,
                PlayerSurgeryPayloadHandler::handleResult
        );

        r.playToClient(
                PlayerSurgeryEndPayload.TYPE,
                PlayerSurgeryEndPayload.STREAM_CODEC,
                PlayerSurgeryPayloadHandler::handleEnd
        );

        r.playToServer(
                PlayerSurgeryCancelPayload.TYPE,
                PlayerSurgeryCancelPayload.STREAM_CODEC,
                PlayerSurgeryPayloadHandler::handleCancel
        );

        /* ---------------- CHATSPACE ---------------- */

        r.playToServer(
                ChatSpaceRequestSyncPayload.TYPE,
                ChatSpaceRequestSyncPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChatSpacePayloadHandler.handleRequestSync(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        r.playToServer(
                ChatSpaceAddContactPayload.TYPE,
                ChatSpaceAddContactPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChatSpacePayloadHandler.handleAddContact(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        r.playToServer(
                ChatSpaceSendMessagePayload.TYPE,
                ChatSpaceSendMessagePayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChatSpacePayloadHandler.handleSendMessage(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        r.playToServer(
                ChatSpaceMarkReadPayload.TYPE,
                ChatSpaceMarkReadPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChatSpacePayloadHandler.handleMarkRead(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        r.playToClient(
                ChatSpaceSyncPayload.TYPE,
                ChatSpaceSyncPayload.STREAM_CODEC,
                ChatSpaceSyncPayload::handle
        );

        r.playToServer(
                InfologSaveComputerPayload.TYPE,
                InfologSaveComputerPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        InfologSaveComputerHandler.handle(
                                payload,
                                sp
                        );
                    }
                })
        );
    }
}
