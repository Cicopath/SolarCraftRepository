package com.finderfeed.solarforge;

import com.finderfeed.solarforge.abilities.ability_classes.ToggleableAbility;
import com.finderfeed.solarforge.abilities.screens.AbilityBuyScreen;
import com.finderfeed.solarforge.client.particles.ParticleTypesRegistry;
import com.finderfeed.solarforge.client.particles.SmallSolarStrikeParticle;
import com.finderfeed.solarforge.client.particles.SolarcraftParticle;
import com.finderfeed.solarforge.client.screens.CrystalEnergyVinesPuzzleScreen;
import com.finderfeed.solarforge.client.toasts.UnlockedEnergyTypeToast;
import com.finderfeed.solarforge.config.JsonFragmentsHelper;
import com.finderfeed.solarforge.config.SolarcraftClientConfig;
import com.finderfeed.solarforge.entities.not_alive.BallLightningProjectile;
import com.finderfeed.solarforge.events.RenderEventsHandler;
import com.finderfeed.solarforge.local_library.effects.LightningBoltPath;
import com.finderfeed.solarforge.local_library.helpers.FDMathHelper;
import com.finderfeed.solarforge.magic.blocks.blockentities.clearing_ritual.CrystalEnergyVinesTile;
import com.finderfeed.solarforge.magic.blocks.blockentities.RayTrapTileEntity;
import com.finderfeed.solarforge.magic.blocks.blockentities.RuneEnergyPylonTile;
import com.finderfeed.solarforge.magic.blocks.blockentities.containers.screens.RunicTableContainerScreen;
import com.finderfeed.solarforge.magic.items.primitive.solacraft_item_classes.FragmentItem;
import com.finderfeed.solarforge.magic.items.solar_lexicon.unlockables.AncientFragment;
import com.finderfeed.solarforge.magic.items.solar_lexicon.unlockables.RunePattern;
import com.finderfeed.solarforge.misc_things.*;
import com.finderfeed.solarforge.registries.items.ItemsRegister;
import com.finderfeed.solarforge.registries.sounds.Sounds;
import com.finderfeed.solarforge.magic.items.solar_lexicon.SolarLexicon;
import com.finderfeed.solarforge.magic.items.solar_lexicon.progressions.Progression;
import com.finderfeed.solarforge.magic.items.solar_lexicon.unlockables.ProgressionHelper;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;


public class ClientHelpers {




    public static void handlePuzzlePacket(BlockPos pos){
        Level level = getLevel();
        if (level.getBlockEntity(pos) instanceof CrystalEnergyVinesTile tile){
            Minecraft.getInstance().setScreen(new CrystalEnergyVinesPuzzleScreen(tile));
        }
    }

    public static void disableFlight(boolean disable){
        if (disable) {
            getClientPlayer().getAbilities().flying = !disable;
        }
        getClientPlayer().getAbilities().mayfly = !disable;

    }

    public static void handleToggleAbilityPacket(ToggleableAbility ability,boolean t){
        ability.setToggled(getClientPlayer(),t);
    }


    public static void deserializeServersideFragmentsAndPutThemInList(JsonObject object){
        List<AncientFragment> fragments = AncientFragment.deserializeFragments(JsonFragmentsHelper.serializedFragmentsArray(object));
        for (AncientFragment fragment : fragments) {
            AncientFragment check = listAlreadyContainsThatFragment(fragment);
            if (check == null) {
                AncientFragment.CLIENTSIDE_FRAGMENTS_CACHE.add(fragment);
                AncientFragment.ALL_FRAGMENTS.add(fragment);
            }else{
                AncientFragment.CLIENTSIDE_FRAGMENTS_CACHE.add(check);
            }
        }
        AncientFragment.initFragmentsMap();
    }

    public static void handleTeleportEntityPacket(int eID,Vec3 pos){
        Entity e = Minecraft.getInstance().level.getEntity(eID);
        if (e != null){
            e.teleportTo(pos.x,pos.y,pos.z);
        }
    }

    /**
     * Method to fix that singleplayer client-server shit
     */
    private static AncientFragment listAlreadyContainsThatFragment(AncientFragment jsonFragment){
        for (int i = AncientFragment.ALL_FRAGMENTS.size() - 1;i >= 0;i--){
            AncientFragment f = AncientFragment.ALL_FRAGMENTS.get(i);
            if (f.getId().equals(jsonFragment.getId())){
                return f;
            }
        }
        return null;
    }

    public static void deleteCachedFragments(){
        AncientFragment.ALL_FRAGMENTS.removeAll(AncientFragment.CLIENTSIDE_FRAGMENTS_CACHE);
        AncientFragment.CLIENTSIDE_FRAGMENTS_CACHE.clear();
        AncientFragment.FRAGMENTS_ID_MAP.clear();
    }

    public static boolean isShadersEnabled(){
        return SolarcraftClientConfig.SHADERS_ENABLED.get();
    }


    public static void doExplosionParticles(Vec3 pos){
        Helpers.createSmallSolarStrikeParticleExplosionWithLines(
            Minecraft.getInstance().level,pos,2,0.1f,0.5f
        );
    }

    public static void handleShadowBoltExplosion(Vec3 pos){
        Level world = Minecraft.getInstance().level;
        if (world == null) return;
        int intensity = 1;
        for (int x = -intensity; x <= intensity;x++){
            for (int y = -intensity; y <= intensity;y++){
                for (int z = -intensity; z <= intensity;z++){
                    Vec3 offset = new Vec3(x,y,z).normalize().multiply(2,2,2);
                    Vec3 finalpos = pos.add(offset);
                    LightningBoltPath path = LightningBoltPath.create(pos,finalpos,4);
                    path.setMaxOffset(0.5f);
                    for (int g = 0; g < 3;g++){
                        Vec3 posS = path.getPos(g);
                        Vec3 posE = path.getPos(g+1);
                        ParticleAnimationHelper.line(ParticleTypesRegistry.SMALL_SOLAR_STRIKE_PARTICLE.get(),
                                posS,posE,0.25f,()->70 + (int)(world.random.nextFloat()*60f),()->0,()->255,(5f-g)/3*0.3f );
                    }
                }
            }
        }
    }

    public static void handleBallLightningProjectileParticles(Vec3 pos){
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            Helpers.createSmallSolarStrikeParticleExplosion(
                    level,pos,2,0.07f,1.0f
            );
            List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class, BallLightningProjectile.BOX.move(pos), (l) -> !(l instanceof Player));
            for (LivingEntity ent : living) {
                double vecLen = ent.position().subtract(pos).length();
                if (vecLen <= 10) {
                    int maxDots = (int) Math.floor(vecLen / 1.5) + 2;
                    LightningBoltPath path = LightningBoltPath.create(pos, ent.position().add(0,ent.getBbHeight()/2,0), maxDots);
                    path.setMaxOffset(0.75);
                    for (int i = 0; i < maxDots - 1; i++) {
                        Vec3 iPos = path.getPos(i);
                        Vec3 ePos = path.getPos(i + 1);
                        ParticleAnimationHelper.line(ParticleTypesRegistry.SMALL_SOLAR_STRIKE_PARTICLE.get(), iPos, ePos,
                                0.20, () -> 220+level.random.nextInt(36), () -> 220+level.random.nextInt(36), () -> 0, 0.25f);
                    }
                }
            }
        }
    }

    public static boolean doClientPlayerHasFragment(AncientFragment fragment){
        return ProgressionHelper.doPlayerHasFragment(getClientPlayer(),fragment);
    }

    public static void updatePlayerPattern(CompoundTag patternData,boolean shouldHideButtons){
        if (getClientPlayer() != null){


            getClientPlayer().getPersistentData().put(RunePattern.PATTERN_SAVE_ID,patternData);

            if (Minecraft.getInstance().screen instanceof RunicTableContainerScreen screen){
                screen.forceUpdate(new RunePattern(getClientPlayer()),shouldHideButtons);
            }
        }
    }


    public static void updatePlayerFragments(CompoundTag fragmentData){
        if (getClientPlayer() != null){
            getClientPlayer().getPersistentData().put(ProgressionHelper.COMPOUND_TAG_FRAGMENTS,fragmentData);

        }
    }

    public static boolean isComponentObfuscated(ItemStack stack){
        if (stack.getItem() instanceof FragmentItem r) {
            if (r.getNeededFragment() == null) return false;
            Player player = ClientHelpers.getClientPlayer();

            if (player != null && !player.isCreative()) {
                return !ProgressionHelper.doPlayerHasFragment(player, r.getNeededFragment());
            } else {
                return false;
            }
        }else {
            return false;
        }
    }

    public static void doSolarStrikeExplosion(Vec3 pos){
        Level level = Minecraft.getInstance().level;
        if (level != null){

            for (int i = 0;i <48;i++){

                float length = 34;
                double offsetx = length * Math.cos(Math.toRadians(i*7.5));
                double offsetz = length * Math.sin(Math.toRadians(i*7.5));
                level.addParticle(ParticleTypesRegistry.SOLAR_STRIKE_PARTICLE.get(),pos.x +offsetx,pos.y,pos.z +offsetz,0,0.05,0);



            }
            for (int i = 0;i <24;i++){
                for (int g = 0; g < 10;g++){
                    float length = 34;
                    double offsetx = level.random.nextFloat()*length * Math.cos(Math.toRadians(i*15));
                    double offsetz = level.random.nextFloat()*length * Math.sin(Math.toRadians(i*15));
                    level.addParticle(ParticleTypesRegistry.SOLAR_STRIKE_PARTICLE.get(),pos.x +offsetx,pos.y,pos.z +offsetz,0,0.05,0);

                }

            }
            for (int h = 0;h <25;h++){

                for (int i = 0; i < 6; i++) {
                    for (int g = 0; g < 1; g++) {
                        float length = 3;
                        double offsetx = level.random.nextFloat() * length * Math.cos(Math.toRadians(i * 60));
                        double offsetz = level.random.nextFloat() * length * Math.sin(Math.toRadians(i * 60));
                        double offsety = level.random.nextFloat() * length + h*8;
                        level.addParticle(ParticleTypesRegistry.SOLAR_STRIKE_PARTICLE.get(), pos.x + offsetx, pos.y +offsety, pos.z + offsetz, 0, 0.05, 0);

                    }

                }
            }

        }
    }


    public static LocalPlayer getClientPlayer(){
        return Minecraft.getInstance().player;
    }

    public static ClientLevel getLevel(){
        return Minecraft.getInstance().level;
    }

    public static void updateClientRunicEnergyForPlayer(float amount, RunicEnergy.Type type){

        RunicEnergy.setEnergy(getClientPlayer(),amount,type);
    }

    public static void addEnergyTypeToast(String id){
        playSound(Sounds.PROGRESSION_GAIN.get(),1,1);
        UnlockedEnergyTypeToast.addOrUpdate(Minecraft.getInstance().getToasts(), RunicEnergy.Type.byId(id));
    }


    public static void handleSolarWandParticles(Vec3 pos,Vec3 vel){
        SmallSolarStrikeParticle particle = (SmallSolarStrikeParticle) Minecraft.getInstance().particleEngine.createParticle(ParticleTypesRegistry.SMALL_SOLAR_STRIKE_PARTICLE.get(),pos.x,pos.y,pos.z,vel.normalize().x,vel.normalize().y,vel.normalize().z);
        particle.setLifetime((int)Math.round(vel.length()/vel.normalize().length())*5/2 );
    }

    public static void updateEnergyTypeOnClient(BlockPos pos,String id){
        BlockEntity tile = Minecraft.getInstance().level.getBlockEntity(pos);
        if (tile instanceof RuneEnergyPylonTile){
            RuneEnergyPylonTile pylon = (RuneEnergyPylonTile) tile;
            pylon.setType(RunicEnergy.Type.byId(id));
        }
    }



    public static void playsoundInEars(SoundEvent event,float volume,float pitch){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event,volume,pitch));
    }

    public static void playTotemAnimation( ){
        LocalPlayer ent = Minecraft.getInstance().player;
        Minecraft.getInstance().particleEngine.createTrackingEmitter(ent, ParticleTypes.TOTEM_OF_UNDYING, 30);
        Minecraft.getInstance().gameRenderer.displayItemActivation(ItemsRegister.TOTEM_OF_IMMORTALITY.get().getDefaultInstance());
        Minecraft.getInstance().level.playLocalSound(ent.getX(), ent.getY(), ent.getZ(), SoundEvents.TOTEM_USE, ent.getSoundSource(), 1F, 0.6F, false);
    }

    public static void updateRepeatersOnClient(BlockPos pos,BlockPos pos2){
        BlockEntity tile = Minecraft.getInstance().level.getBlockEntity(pos);
        if (tile instanceof AbstractSolarNetworkRepeater){
            ((AbstractSolarNetworkRepeater) tile).connectedTo = pos2;
        }
    }

    public static void updateGeneratorOnClient(BlockPos pos, BlockPos pos2,int index,boolean remove){
        BlockEntity tile = Minecraft.getInstance().level.getBlockEntity(pos);
        if (tile instanceof AbstractEnergyGeneratorTileEntity){
            if (!remove) {

                if (((AbstractEnergyGeneratorTileEntity) tile).poslist.size()-1 >= index) {
                    ((AbstractEnergyGeneratorTileEntity) tile).poslist.set(index, pos2);
                }else{
                    ((AbstractEnergyGeneratorTileEntity) tile).poslist.add(pos2);
                }
            }else{

                ((AbstractEnergyGeneratorTileEntity) tile).poslist.remove(pos2);
            }
        }
    }
    public static void updateCoreOnClient(BlockPos pos, BlockPos pos2,int index,boolean remove){
        BlockEntity tile = Minecraft.getInstance().level.getBlockEntity(pos);
        if (tile instanceof AbstractSolarCore){
            if (!remove) {
                if (((AbstractSolarCore) tile).poslist.size()-1 >= index) {
                    ((AbstractSolarCore) tile).poslist.set(index, pos2);
                }else{
                    ((AbstractSolarCore) tile).poslist.add(pos2);
                }
            }else{
                ((AbstractSolarCore) tile).poslist.remove(pos2);
            }
        }
    }
    public static void playSoundAtPos(BlockPos pos,int soundID,float pitch, float volume){
        Level world = Minecraft.getInstance().player.level;


        world.playSound(Minecraft.getInstance().player,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,getSoundByID(soundID),
                SoundSource.BLOCKS,volume,pitch);

    }

    public static SoundEvent getSoundByID(int id){
        if (id == 1){
            return Sounds.SOLAR_MORTAR_SHOOT.get();
        }else if(id == 2){
            return Sounds.SOLAR_MORTAR_PROJECTILE.get();
        }
        return null;
    }


    public static void reloadProgression(ServerPlayer playerServer){

        Player player = Minecraft.getInstance().player;
        for (Progression a : Progression.getAllAchievements()){
            Helpers.setProgressionCompletionStatus(a,player,Helpers.hasPlayerCompletedProgression(a,playerServer));

        }
    }

    /**
     * Updates RayTrapTileEntity client trigger
     */
    public static void updateIntegerLASERTRAP(BlockPos pos, int i){
        BlockEntity tile = Minecraft.getInstance().level.getBlockEntity(pos);
        if (tile instanceof RayTrapTileEntity){
            ((RayTrapTileEntity) tile).CLIENT_TRIGGER_INTEGER = 1;
        }
    }

    /**
     * F3+A from code
     */
    public static void reloadChunks(){
        Minecraft.getInstance().levelRenderer.allChanged();
    }

    public static Component getNameBasedOnProgression(ItemStack stack){
        IProgressionBlock block = (IProgressionBlock) ((BlockItem)stack.getItem()).getBlock();
        Player playerEntity = getClientPlayer();
        if (playerEntity != null) {
            if (Helpers.hasPlayerCompletedProgression(block.getRequiredProgression(), playerEntity)) {
                return block.getUnlockedBlock().getName();
            } else {
                return block.getLockedBlock().asItem().getDefaultInstance().getHoverName();
            }
        }
        return block.getLockedBlock().asItem().getDefaultInstance().getHoverName();
    }

//    public static TranslatableComponent getTranslationFor(Block block){
//        if (block.equals(BlocksRegistry.ULDORADIUM_ORE.get())){
//            return new TranslatableComponent("item.solarforge.uldoradium_ore");
//        }
//        return null;
//    }


    public static void updateLexiconInventory(ItemStack[] stacks){
        ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
        if ((stack.getItem() instanceof SolarLexicon)){
            IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(null);
            if (handler != null){
                for (int i = 0; i < stacks.length;i++){
                    handler.insertItem(i,stacks[i],false);
                }
            }

        }
    }
    public static void triggerProgressionUnlockShader(){
        RenderEventsHandler.triggerProgressionShader();
    }

    public static void playSound(SoundEvent event,float a, float b){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event,a,b));
    }

    public static void bindText(ResourceLocation loc){
        RenderSystem.setShaderTexture(0,loc);
    }

    public static List<AbstractWidget> getScreenButtons(Screen screen){
        List<Widget> widgets = screen.renderables;
        List<AbstractWidget> returnable = new ArrayList<>();
        widgets.forEach((widget)->{
            if (widget instanceof AbstractWidget){
                returnable.add((AbstractWidget) widget);
            }
        });
        return returnable;
    }

    public static void openAbilityScreen(){
        Minecraft.getInstance().setScreen(new AbilityBuyScreen());
    }

    public static void createEffectParticle(double x, double y, double z,double xs, double ys, double zs, MobEffect effect){

        SmallSolarStrikeParticle particle = (SmallSolarStrikeParticle) Minecraft.getInstance().particleEngine.
                createParticle(ParticleTypesRegistry.SMALL_SOLAR_STRIKE_PARTICLE.get(),x,y,z,xs,ys,zs);
        int[] rgba = FDMathHelper.intToRgba(effect.getColor());
        particle.setColor((float)rgba[0]/255,(float)rgba[1]/255,(float)rgba[2]/255);
    }

    public static class ParticleConstructor{

        private final Particle particle;

        public ParticleConstructor(ParticleOptions opt,double posx,double posy,double posz,double speedx,double speedy,double speedz){
            particle = Minecraft.getInstance().particleEngine.createParticle(opt,posx,posy,posz,speedx,speedy,speedz);
        }

        public ParticleConstructor setColor(int r,int g,int b){
            particle.setColor((float)r/255,(float)g/255,(float)b/255);
            return this;
        }

        public ParticleConstructor setLifetime(int lifetime){
            particle.setLifetime(lifetime);
            return this;
        }

    }


    public static class ParticleAnimationHelper{

        public static void createParticle(ParticleOptions options,double x,double y,double z,double xd,double yd,double zd,
                                          Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxsize){
            if (Minecraft.getInstance().level == null) return;
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(options,x,y,z,xd,yd,zd);
            if (particle != null) {
                particle.setColor(red.get()/255f, green.get()/255f, blue.get()/255f);
                if (particle instanceof SolarcraftParticle solarcraftParticle) {
                    solarcraftParticle.setMaxSize(maxsize);

                }
            }
        }

        public static void line(ParticleOptions particle, Vec3 from, Vec3 to, double intensity, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize){
            Vec3 between = to.subtract(from);
            double l = between.length();
            for (double i = 0; i <= l;i+=intensity){
                Vec3 pos = from.add(between.multiply(i/l,i/l,i/l));
                Particle p = Minecraft.getInstance().particleEngine.createParticle(particle,pos.x,pos.y,pos.z,0,0,0);
                p.setColor((float)red.get()/255,(float)green.get()/255,(float)blue.get()/255);
                if (p instanceof SolarcraftParticle pd){
                    pd.setMaxSize(maxSize);
                }
            }
        }

        public static void randomline(ParticleOptions particle, Vec3 from, Vec3 to, double intensity, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize,float chance){
            Level wrl = Minecraft.getInstance().level;
            if (wrl == null) return;

            Vec3 between = to.subtract(from);
            double l = between.length();
            for (double i = 0; i <= l;i+=intensity){
                if (wrl.random.nextFloat() <= chance) {
                    Vec3 pos = from.add(between.multiply(i / l, i / l, i / l));
                    Particle p = Minecraft.getInstance().particleEngine.createParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
                    p.setColor((float) red.get() / 255, (float) green.get() / 255, (float) blue.get() / 255);
                    if (p instanceof SolarcraftParticle pd) {
                        pd.setMaxSize(maxSize);
                    }
                }
            }
        }

        public static void timedLine(ParticleOptions particle, Vec3 from, Vec3 to,int duration, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize){
            Vec3 between = to.subtract(from);
            if (Minecraft.getInstance().level != null) {
                float percentage = ((float) Minecraft.getInstance().level.getGameTime() % duration) / duration;
                Vec3 pos = from.add(between.multiply(percentage, percentage, percentage));
                Particle p = Minecraft.getInstance().particleEngine.createParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
                p.setColor((float) red.get() / 255, (float) green.get() / 255, (float) blue.get() / 255);
                if (p instanceof SolarcraftParticle pd) {
                    pd.setMaxSize(maxSize);
                }
            }

        }
        public static void verticalCircle(ParticleOptions particle, Vec3 center,double radius, int count,float[] speed, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize){
            if (Minecraft.getInstance().level != null) {
                float gametime = Minecraft.getInstance().level.getGameTime();
                double angle = 360d / count;
                for (int i = 0; i < count; i += 1) {
                    double a = Math.toRadians(i * angle + gametime);
                    double x = radius * Math.sin(a);
                    double z = radius * Math.cos(a);
                    Particle p = Minecraft.getInstance().particleEngine.createParticle(particle, center.x + x, center.y, center.z + z, speed[0], speed[1], speed[2]);
                    p.setColor((float) red.get() / 255, (float) green.get() / 255, (float) blue.get() / 255);
                    if (p instanceof SolarcraftParticle pd) {
                        pd.setMaxSize(maxSize);
                    }
                }
            }
        }

        public static void horizontalXCircle(ParticleOptions particle, Vec3 center,double radius, int count,float[] speed, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize,float timeMod,float offset){
            if (Minecraft.getInstance().level != null) {
                float gametime = Minecraft.getInstance().level.getGameTime()*timeMod;
                double angle = 360d / count;
                for (int i = 0; i < count; i += 1) {
                    double a = Math.toRadians(i * angle + gametime + offset);
                    double x = radius * Math.sin(a);
                    double y = radius * Math.cos(a);
                    Particle p = Minecraft.getInstance().particleEngine.createParticle(particle, center.x + x, center.y + y, center.z, speed[0], speed[1], speed[2]);
                    p.setColor((float) red.get() / 255, (float) green.get() / 255, (float) blue.get() / 255);
                    if (p instanceof SolarcraftParticle pd) {
                        pd.setMaxSize(maxSize);
                    }
                }
            }
        }
        public static void horizontalZCircle(ParticleOptions particle, Vec3 center,double radius, int count,float[] speed, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize,float timeMod,float offset){
            if (Minecraft.getInstance().level != null) {
                float gametime = Minecraft.getInstance().level.getGameTime() * timeMod;
                double angle = 360d / count;
                for (int i = 0; i < count; i += 1) {
                    double a = Math.toRadians(i * angle + gametime + offset);
                    double z = radius * Math.sin(a);
                    double y = radius * Math.cos(a);

                    Particle p = Minecraft.getInstance().particleEngine.createParticle(particle, center.x, center.y + y, center.z + z, speed[0], speed[1], speed[2]);
                    p.setColor((float) red.get() / 255, (float) green.get() / 255, (float) blue.get() / 255);
                    if (p instanceof SolarcraftParticle pd) {
                        pd.setMaxSize(maxSize);
                    }
                }
            }
        }

        public static void horizontalRotatedCircle(ParticleOptions particle, Vec3 center,double radius,double rotAngle, int count,float[] speed, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize){
            if (Minecraft.getInstance().level != null) {
                float gametime = Minecraft.getInstance().level.getGameTime();
                double angle = 360d / count;
                for (int i = 0; i < count; i += 1) {
                    double a = Math.toRadians(i * angle + gametime);
                    double x = radius * Math.sin(a);
                    double y = radius * Math.cos(a);
                    double[] rotatedXZ = FDMathHelper.rotatePointDegrees(x, 0, rotAngle);
                    x = rotatedXZ[0];
                    double z = rotatedXZ[1];

                    Particle p = Minecraft.getInstance().particleEngine.createParticle(particle, center.x + x, center.y + y, center.z + z, speed[0], speed[1], speed[2]);
                    p.setColor((float) red.get() / 255, (float) green.get() / 255, (float) blue.get() / 255);
                    if (p instanceof SolarcraftParticle pd) {
                        pd.setMaxSize(maxSize);
                    }
                }
            }
        }

        public static void verticalCircleWTime(ParticleOptions particle, Vec3 center,double radius, int count,float[] speed, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize,float time){
            if (Minecraft.getInstance().level != null) {
                float gametime = Minecraft.getInstance().level.getGameTime();
                double angle = 360d / count;
                for (int i = 0; i < count; i += 1) {
                    double a = Math.toRadians(i * angle + gametime);
                    double x = radius * Math.sin(a);
                    double z = radius * Math.cos(a);
                    Particle p = Minecraft.getInstance().particleEngine.createParticle(particle, center.x + x, center.y, center.z + z, speed[0], speed[1], speed[2]);
                    p.setColor((float) red.get() / 255, (float) green.get() / 255, (float) blue.get() / 255);
                    if (p instanceof SolarcraftParticle pd) {
                        pd.setMaxSize(maxSize);
                    }
                }
            }
        }



        public static void horizontalRotatedCircleWTime(ParticleOptions particle, Vec3 center,double radius,double rotAngle, int count,float[] speed, Supplier<Integer> red,Supplier<Integer> green,Supplier<Integer> blue,float maxSize,float time){
            if (Minecraft.getInstance().level != null) {
                double angle = 360d / count;
                for (int i = 0; i < count; i += 1) {
                    double a = Math.toRadians(i * angle + time);
                    double x = radius * Math.sin(a);
                    double y = radius * Math.cos(a);
                    double[] rotatedXZ = FDMathHelper.rotatePointDegrees(x, 0, rotAngle);
                    x = rotatedXZ[0];
                    double z = rotatedXZ[1];

                    Particle p = Minecraft.getInstance().particleEngine.createParticle(particle, center.x + x, center.y + y, center.z + z, speed[0], speed[1], speed[2]);
                    p.setColor((float) red.get() / 255, (float) green.get() / 255, (float) blue.get() / 255);
                    if (p instanceof SolarcraftParticle pd) {
                        pd.setMaxSize(maxSize);
                    }
                }
            }
        }

    }



}
