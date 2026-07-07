package com.contisupply.royalkennel;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Persistent + synced storage for a dog's cosmetics. The attachment is saved
 * with the wolf and automatically synced to every player tracking it, which is
 * what lets the client render hats/backpacks on any dog that walks by.
 */
public final class KennelAttachments {

    public static final AttachmentType<DogStyle> DOG_STYLE = AttachmentRegistry.<DogStyle>builder()
            .persistent(DogStyle.CODEC)
            .syncWith(DogStyle.STREAM_CODEC, AttachmentSyncPredicate.all())
            .buildAndRegister(RoyalKennel.id("dog_style"));

    private KennelAttachments() {
    }

    public static void init() {
        // Forces the static initializer to run during mod init.
    }
}
