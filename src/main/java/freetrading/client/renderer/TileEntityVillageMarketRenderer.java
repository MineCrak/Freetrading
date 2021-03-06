package freetrading.client.renderer;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.glu.GLU;

import freetrading.ClientProxy;
import freetrading.client.Icon;
import freetrading.tileentity.TileEntityVillageMarket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import static freetrading.FreeTradingMod.*;

public class TileEntityVillageMarketRenderer extends TileEntitySpecialRenderer<TileEntityVillageMarket> {

	private RenderItem itemRenderer;
	private FontRenderer fontRenderer;

	private int fb = -1;
	private final int textureWidth = 64;
	private final int textureHeight = 64;
	private int nextAvailableId = 0;
	private boolean framebufferReady = false;
	Icon picon = SpecialRendererRegistry.registerIcon(new ResourceLocation(MODID + ":blocks/counter_sign_texture_space"));
	private final List<TileEntityVillageMarket> pendingRenderUpdate = new ArrayList<TileEntityVillageMarket>();
	
	private final static float CLOSE_RANGE_CUTTING_EDGE = 0.01F;

	public TileEntityVillageMarketRenderer() {}
	
	public void setRenders() {
		itemRenderer = Minecraft.getMinecraft().getRenderItem();
		fontRenderer = Minecraft.getMinecraft().fontRenderer;
	}
	
	@Override
	public void render(TileEntityVillageMarket te, double x, double y, double z, float partialTicks, int destroyStage, float alphaValue) {
		if (!framebufferReady) {
			this.generateFrameBuffer();
			Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
		}
		if (te.getIconId() == 0) {
			te.setIconId(++nextAvailableId);
			te.displayList = GLAllocation.generateDisplayLists(1);
		}
		if (te.needRenderUpdate) {
			pendingRenderUpdate.add(te);
	        GlStateManager.glNewList(te.displayList, 4864);
			GlStateManager.pushMatrix();
			this.drawSquare(1d, te.getIconId());
			GlStateManager.translate(0.5D, 2.0D / 16.0D, 0.5D);
			this.renderItem(te);
			GlStateManager.popMatrix();
	        GlStateManager.glEndList();
			te.needRenderUpdate = false;
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.callList(te.displayList);
		GlStateManager.popMatrix();
		super.render(te, x, y, z, partialTicks, destroyStage, alphaValue);
	}

	private void drawSquare(double scale, int subIconId) {
		float minu = getSubIconMinU(subIconId);
		float minv = getSubIconMinV(subIconId);
		float maxu = minu + getSubIconDU(subIconId);
		float maxv = minv + getSubIconDV(subIconId);

		double x1 = 7 / 16D;
		double y1 = 6 / 16D;
		double x2 = 1D;
		double y2 = 15 / 16D;
		double z1 = 1 / 16D;
		double z2 = 2 / 16D;

		BufferBuilder vb = Tessellator.getInstance().getBuffer();

		vb.begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
		vb.pos(x1 * scale, y1 * scale, z1 * scale)
				.tex(maxu, minv).normal(0, 0, -1).endVertex();
		vb.pos(x1 * scale, y2 * scale, z1 * scale)
				.tex(maxu, maxv).normal(0, 0, -1).endVertex();
		vb.pos(x2 * scale, y2 * scale, z1 * scale)
				.tex(minu, maxv).normal(0, 0, -1).endVertex();
		vb.pos(x2 * scale, y1 * scale, z1 * scale)
				.tex(minu, minv).normal(0, 0, -1).endVertex();
		vb.pos(x2 * scale, y1 * scale, z2 * scale)
				.tex(maxu, minv).normal(0, 0, 1).endVertex();
		vb.pos(x2 * scale, y2 * scale, z2 * scale)
				.tex(maxu, maxv).normal(0, 0, 1).endVertex();
		vb.pos(x1 * scale, y2 * scale, z2 * scale)
				.tex(minu, maxv).normal(0, 0, 1).endVertex();
		vb.pos(x1 * scale, y1 * scale, z2 * scale)
				.tex(minu, minv).normal(0, 0, 1).endVertex();

		Tessellator.getInstance().draw();
	}

	private void generateFrameBuffer() {
		fb = GL30.glGenFramebuffers();
		this.preparetexture();
		if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
			logger.info("Something went wrong while creating frame buffer!");
			logger.info("Framebuffer status: " + GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
		} else {
			logger.info("FrameBuffer loaded correctly!");
		}
		this.framebufferReady = true;
	}

	private void preparetexture() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fb);
		int texture = Minecraft.getMinecraft().renderEngine.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
				.getGlTextureId();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
	}
	
	private void drawEmeraldSign() {
		Icon emeraldIcon = ClientProxy.EMERALD_SHARD;
		float minu = emeraldIcon.getMinU();
		float minv = emeraldIcon.getMinV();
		float maxu = emeraldIcon.getMaxU();
		float maxv = emeraldIcon.getMaxV();
		double scale = 1.0d;
		double x1 = -1.0D;
		double y1 = -0.5D;
		double x2 = 0D;
		double y2 = 0.5D;
		double z = 2 / 16D;
		BufferBuilder vb = Tessellator.getInstance().getBuffer();
		vb.begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
		vb.pos(x2 * scale, y1 * scale, z * scale)
				.tex(maxu, minv).normal(0, 0, 1).endVertex();
		vb.pos(x2 * scale, y2 * scale, z * scale)
				.tex(maxu, maxv).normal(0, 0, 1).endVertex();
		vb.pos(x1 * scale, y2 * scale, z * scale)
				.tex(minu, maxv).normal(0, 0, 1).endVertex();
		vb.pos(x1 * scale, y1 * scale, z * scale)
				.tex(minu, minv).normal(0, 0, 1).endVertex();
		Tessellator.getInstance().draw();

	}

	private void drawTexture(TileEntityVillageMarket tile, int subIconId) {
		float minu = picon.getMinU();
		float minv = picon.getMinV();
		float maxu = picon.getMaxU();
		float maxv = picon.getMaxV();
		int iconwidth = picon.getIconWidth();
		int iconheight = picon.getIconHeight();
		float du = maxu - minu;
		float dv = maxv - minv;
		int iconNumU = picon.getIconWidth() / this.textureWidth;
		int posu = (int) (minu * iconwidth / du) + subIconId % iconNumU * textureWidth;
		int posv = (int) (minv * iconheight / dv) + subIconId / iconNumU * textureHeight;

		GL11.glViewport(posu, posv, textureWidth, textureHeight);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(-1d, 1d, -1d, 1d, CLOSE_RANGE_CUTTING_EDGE, Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 32F);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glTranslatef(0, 0, -1f);
		GLU.gluLookAt(0, 0, 0, -32f/* x reference */, 0f/* y reference */, 0f/* z reference */, 0.0f, 1.0f, 0.0f);
		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glRotatef(90f, 0, 1f, 0);
		GL11.glScalef(1f, 1f, 1f);
		GlStateManager.disableFog();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 0xf0, 0xf0);

		this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		this.drawBackground(1.0d);
		this.drawEmeraldSign();
		this.drawPrices(tile);
		GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
	}

	private void drawPrices(TileEntityVillageMarket tile) {
		GL11.glPushMatrix();
		GL11.glScalef(1f / 16f, -1f / 16f, 1f / 16f);
		GL11.glTranslatef(-2f, -6f, 0);
		GL11.glColor4f(0f, 0f, 0f, 1f);
		if (tile.price!=0)
			fontRenderer.drawString(String.valueOf(tile.price), 0, 0, 0);
		GL11.glPopMatrix();
	}

	
	private void drawBackground(double scale) {
		float minu = getSubIconMinU(0);
		float minv = getSubIconMinV(0);
		float maxu = minu + getSubIconDU(0);
		float maxv = minv + getSubIconDV(0);

		double x1 = -1D;
		double y1 = -1D;
		double x2 = 1D;
		double y2 = 1D;
		double z1 = 0D;

		BufferBuilder vb = Tessellator.getInstance().getBuffer();

		vb.begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
		vb.pos(x1 * scale, y1 * scale, z1 * scale)
				.tex(maxu, minv).normal(0, 0, -1).endVertex();
		vb.pos(x1 * scale, y2 * scale, z1 * scale)
				.tex(maxu, maxv).normal(0, 0, -1).endVertex();
		vb.pos(x2 * scale, y2 * scale, z1 * scale)
				.tex(minu, maxv).normal(0, 0, -1).endVertex();
		vb.pos(x2 * scale, y1 * scale, z1 * scale)
				.tex(minu, minv).normal(0, 0, -1).endVertex();
		
		
		vb.pos(x2 * scale, y1 * scale, z1 * scale)
		.tex(minu, minv).normal(0, 0, -1).endVertex();
		vb.pos(x2 * scale, y2 * scale, z1 * scale)
		.tex(minu, maxv).normal(0, 0, -1).endVertex();
		vb.pos(x1 * scale, y2 * scale, z1 * scale)
		.tex(maxu, maxv).normal(0, 0, -1).endVertex();
		vb.pos(x1 * scale, y1 * scale, z1 * scale)
		.tex(maxu, minv).normal(0, 0, -1).endVertex();

		Tessellator.getInstance().draw();
	}

	public float getSubIconMinU(int index) {
		int iconNumU = picon.getIconWidth() / this.textureWidth;
		float minu = picon.getMinU();
		float maxu = picon.getMaxU();
		float du = (maxu - minu) / iconNumU;
		return minu + index % iconNumU * du;
	}

	public float getSubIconMinV(int index) {
		int iconNumU = picon.getIconWidth() / this.textureWidth;
		int iconNumV = picon.getIconHeight() / this.textureHeight;
		float minv = picon.getMinV();
		float maxv = picon.getMaxV();
		float dv = (maxv - minv) / iconNumV;
		return minv + index / iconNumU * dv;
	}
	public float getSubIconDU(int index) {
		int iconNumU = picon.getIconWidth() / this.textureWidth;
		float minu = picon.getMinU();
		float maxu = picon.getMaxU();
		float du = (maxu - minu) / iconNumU;
		return du;
	}

	public float getSubIconDV(int index) {
		int iconNumV = picon.getIconHeight() / this.textureHeight;
		float minv = picon.getMinV();
		float maxv = picon.getMaxV();
		float dv = (maxv - minv) / iconNumV;
		return dv;
	}

	private void renderItem(TileEntityVillageMarket counter) {
		ItemStack itemstack = counter.getDisplayedItem();
		if (!itemstack.isEmpty()) {
			GlStateManager.pushMatrix();
			GlStateManager.disableLighting();
			GlStateManager.scale(0.5F, 0.5F, 0.5F);
			GlStateManager.rotate(90f, 1f, 0f, 0f);
			GlStateManager.pushAttrib();
			RenderHelper.enableStandardItemLighting();
			this.itemRenderer.renderItem(itemstack, ItemCameraTransforms.TransformType.FIXED);
			RenderHelper.disableStandardItemLighting();
			GlStateManager.popAttrib();
			GlStateManager.enableLighting();
			GlStateManager.popMatrix();
		}
	}
	
	@SubscribeEvent
	public void onTick(TickEvent.RenderTickEvent event)
	{ 
		if(event.phase == TickEvent.Phase.START || pendingRenderUpdate.isEmpty())
			return;
		this.preparetexture();
		for(TileEntityVillageMarket te:pendingRenderUpdate){
			this.drawTexture(te, te.getIconId());
		}
		Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
		pendingRenderUpdate.clear();
	}
}
