package com.finderfeed.solarforge.client.screens;

import com.finderfeed.solarforge.ClientHelpers;
import com.finderfeed.solarforge.SolarForge;

import com.finderfeed.solarforge.local_library.helpers.RenderingTools;
import com.finderfeed.solarforge.magic.items.solar_lexicon.SolarLexicon;
import com.finderfeed.solarforge.magic.items.solar_lexicon.screen.ItemStackButton;
import com.finderfeed.solarforge.magic.items.solar_lexicon.screen.SolarLexiconRecipesScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class EightElementsFragmentScreen extends ScreenWithPages{

    public static final ResourceLocation MAIN_SCREEN = new ResourceLocation(SolarForge.MOD_ID,"textures/gui/lore_screen_new.png");
    public static final ResourceLocation IMAGE_LOCATION = new ResourceLocation(SolarForge.MOD_ID,"textures/lore_images/elements.png");
    public static final ResourceLocation IMAGE_LOCATION_2 = new ResourceLocation(SolarForge.MOD_ID,"textures/misc/all_elements.png");
    private final ResourceLocation MAIN_SCREEN_2 = new ResourceLocation("solarforge","textures/gui/solar_lexicon_info_screen_new.png");

    public EightElementsFragmentScreen() {
        super(9);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ItemStackButton(relX+74+53,relY+9,12,12,(button)->{minecraft.setScreen(new SolarLexiconRecipesScreen());}, Items.CRAFTING_TABLE.getDefaultInstance(),0.7f));
        addRenderableWidget(new ItemStackButton(relX+61+53,relY+9,12,12,(button)->{
            Minecraft mc = Minecraft.getInstance();
            SolarLexicon lexicon = (SolarLexicon) mc.player.getMainHandItem().getItem();
            lexicon.currentSavedScreen = this;
            minecraft.setScreen(null);
        }, Items.WRITABLE_BOOK.getDefaultInstance(),0.7f));
    }

    @Override
    public void render(PoseStack matrices, int mousex, int mousey, float partialTicks) {
        int stringColor = 0xee2222;
        if (getCurrentPage() == 1){

            ClientHelpers.bindText(MAIN_SCREEN);
            blit(matrices,relX,relY,0,0,256,256,256,256);
            ClientHelpers.bindText(IMAGE_LOCATION);
            blit(matrices,relX+19,relY+19,0,0,60,60,60,60);


            int posX = relX+14;
            int posY = relY+100;
            RenderingTools.drawBoundedText(matrices,posX,posY,45,new TranslatableComponent("eight_elements.lore").getString(),stringColor);
        }else{
            ClientHelpers.bindText(MAIN_SCREEN_2);
            blit(matrices,relX,relY,0,0,256,256,256,256);

            ClientHelpers.bindText(IMAGE_LOCATION_2);
            blit(matrices,relX+32,relY+32,(getCurrentPage()-2) * 16,0,16,16,128,16);

            int posX = relX+14;
            int posY = relY+82;
            RenderingTools.drawBoundedText(matrices,posX,posY,45,new TranslatableComponent("solarcraft.rune_element_"+(getCurrentPage()-1)).getString(),stringColor);
        }


        super.render(matrices, mousex, mousey, partialTicks);
    }

    @Override
    protected int getScrollValue() {
        return 0;
    }

    @Override
    protected int getMaxYDownScrollValue() {
        return 0;
    }

    @Override
    protected int getMaxXRightScrollValue() {
        return 0;
    }

    @Override
    protected int getMaxYUpScrollValue() {
        return 0;
    }

    @Override
    protected int getMaxXLeftScrollValue() {
        return 0;
    }

    @Override
    public int[] getPageButtonsCoords() {
        return new int[]{216,10};
    }
}
