package de.maxhenkel.voicechat.voice.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import de.maxhenkel.voicechat.Config;
import de.maxhenkel.voicechat.Main;
import de.maxhenkel.voicechat.gui.VoiceChatScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class ClientVoiceEvents {

    private static final ResourceLocation MICROPHONE_ICON = new ResourceLocation(Main.MODID, "textures/gui/microphone.png");
    private static final ResourceLocation SPEAKER_ICON = new ResourceLocation(Main.MODID, "textures/gui/speaker.png");

    private Client client;
    private Minecraft minecraft;

    public ClientVoiceEvents() {
        minecraft = Minecraft.getInstance();
    }

    @SubscribeEvent
    public void joinEvent(EntityJoinWorldEvent event) {
        if (event.getEntity() != minecraft.player) {
            return;
        }
        if (client != null) {
            return;
        }
        ServerData serverData = minecraft.getCurrentServerData();
        if (serverData != null) {
            try {
                Main.LOGGER.info("Connecting to server: '" + serverData.serverIP + ":" + Config.SERVER.VOICE_CHAT_PORT.get() + "'");
                client = new Client(serverData.serverIP, Config.SERVER.VOICE_CHAT_PORT.get());
                client.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @SubscribeEvent
    public void joinEvent(WorldEvent.Unload event) {
        // Not just changing the world - Disconnecting
        if (minecraft.playerController == null) {
            if (client != null) {
                client.close();
                client = null;
            }
        }
    }

    public Client getClient() {
        return client;
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Pre event) {
        if (!event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)) {
            return;
        }

        if (client == null || !client.isConnected()) {
            return;
        }

        if (!Main.KEY_PTT.isKeyDown()) {
            return;
        }

        RenderSystem.pushMatrix();

        minecraft.getTextureManager().bindTexture(MICROPHONE_ICON);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        //double width = minecraft.getMainWindow().getScaledWidth();
        double height = minecraft.getMainWindow().getScaledHeight();

        buffer.pos(16D, height - 32D, 0D).tex(0F, 0F).endVertex();
        buffer.pos(16D, height - 16D, 0D).tex(0F, 1F).endVertex();
        buffer.pos(32D, height - 16D, 0D).tex(1F, 1F).endVertex();
        buffer.pos(32D, height - 32D, 0D).tex(1F, 0F).endVertex();

        tessellator.draw();

        RenderSystem.popMatrix();
    }

    @SubscribeEvent
    public void onInput(InputEvent.KeyInputEvent event) {
        if (Main.KEY_VOICE_CHAT_SETTINGS.isPressed()) {
            minecraft.displayGuiScreen(new VoiceChatScreen());
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderNameplateEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) {
            return;
        }

        PlayerEntity playerEntity = (PlayerEntity) event.getEntity();
        if (client.getTalkCache().isTalking(playerEntity)) {
            renderSpeaker(playerEntity, event.getContent(), event.getMatrixStack(), event.getRenderTypeBuffer(), event.getPackedLight());
        }
    }

    protected void renderSpeaker(PlayerEntity player, String displayNameIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.translate(0D, player.getHeight() + 0.5D, 0D);
        matrixStackIn.rotate(minecraft.getRenderManager().getCameraOrientation());
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        matrixStackIn.translate(0D, -1D, 0D);

        float offset = (float) (minecraft.fontRenderer.getStringWidth(displayNameIn) / 2 + 1);
        IVertexBuilder builder = bufferIn.getBuffer(RenderType.getEntityCutout(SPEAKER_ICON));

        vertex(builder, matrixStackIn, offset, 10F, 0F, 0F, 1F, packedLightIn);
        vertex(builder, matrixStackIn, offset + 10F, 10F, 0F, 1F, 1F, packedLightIn);
        vertex(builder, matrixStackIn, offset + 10F, 0F, 0F, 1F, 0F, packedLightIn);
        vertex(builder, matrixStackIn, offset, 0F, 0F, 0F, 0F, packedLightIn);

        matrixStackIn.pop();
    }

    private static void vertex(IVertexBuilder builder, MatrixStack matrixStack, float x, float y, float z, float u, float v, int light) {
        MatrixStack.Entry entry = matrixStack.getLast();
        builder.pos(entry.getMatrix(), x, y, z)
                .color(255, 255, 255, 255)
                .tex(u, v)
                .overlay(OverlayTexture.NO_OVERLAY)
                .lightmap(light)
                .normal(entry.getNormal(), 0F, 0F, -1F)
                .endVertex();
    }

}