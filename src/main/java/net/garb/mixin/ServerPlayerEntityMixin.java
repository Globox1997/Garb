package net.garb.mixin;

import net.garb.character.CharacterData;
import net.garb.character.CharacterSaveManager;
import net.garb.character.GarbActiveCharacterCache;
import net.garb.character.GarbPlayerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {



//    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
//    private void getPlayerListNameMixin(CallbackInfoReturnable<Text> info) {
//        ServerPlayerEntity serverPlayerEntity = ((ServerPlayerEntity) (Object) this);
//
//        System.out.println("GET PLAYER LIST NAME");
//        int slot = GarbPlayerManager.getSelection(serverPlayerEntity.getUuid());
//        if (slot == -1) {
//            return;
//        }
//        MinecraftServer server = serverPlayerEntity.getServer();
//        if (server == null) {
//            return;
//        }
//        CharacterData data = CharacterSaveManager.load(server, serverPlayerEntity.getUuid(), slot);
//
//        System.out.println("YESS" + data.getName());
//
//        info.setReturnValue(Text.of(data.getName()));
//    }

}
