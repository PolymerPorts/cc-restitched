package dan200.computercraft.fabric.poly;

import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.fabric.poly.textures.HeadTextures;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.MobAnchorElement;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;

import java.util.function.Consumer;

public class TurtleModel extends ElementHolder {

    private final ComputerProxy proxy;

    private final Matrix4fStack matrix4f = new Matrix4fStack(3);

    private final MobAnchorElement ride = new MobAnchorElement();
    private final ItemDisplayElement main = new ItemDisplayElement();
    private final ItemDisplayElement color = new ItemDisplayElement();
    private final ItemDisplayElement leftUpgrade = new ItemDisplayElement();
    private final ItemDisplayElement rightUpgrade = new ItemDisplayElement();
    private final IntList visibleEntities = new IntArrayList();
    private final boolean isRightBlock;
    private final boolean isLeftBlock;
    private float targetRotation;
    private int tickId;

    public TurtleModel(ComputerProxy proxy, Direction direction) {
        this.proxy = proxy;
        this.addElement(this.ride);
        this.setDirection(direction);

        this.main.setItem(PolymerUtils.createPlayerHead(this.proxy.getBlockEntity().getFamily() == ComputerFamily.ADVANCED ? HeadTextures.ADVANCED_TURTLE : HeadTextures.TURTLE));
        this.main.setModelTransformation(ItemDisplayContext.FIXED);
        this.main.setDisplayWidth(2);
        this.main.setDisplayHeight(2);
        //this.main.setInterpolationDuration(2);
        this.visibleEntities.add(this.main.getEntityId());
        this.addElement(this.main);

        this.color.setModelTransformation(ItemDisplayContext.FIXED);
        this.color.setDisplayWidth(2);
        this.color.setDisplayHeight(2);
        //this.color.setInterpolationDuration(2);
        this.visibleEntities.add(this.color.getEntityId());

        this.addElement(this.color);

        this.setColor(((TileTurtle) proxy.getBlockEntity()).brain.getDyeColour());


        var rightUpgrade = ((TileTurtle) proxy.getBlockEntity()).getUpgrade(TurtleSide.RIGHT);
        if (rightUpgrade != null) {
            this.isRightBlock = rightUpgrade.getCraftingItem().getItem() instanceof BlockItem;
            this.rightUpgrade.setItem(rightUpgrade.getCraftingItem());
            this.rightUpgrade.setModelTransformation(ItemDisplayContext.FIXED);
            this.rightUpgrade.setDisplayWidth(2);
            this.rightUpgrade.setDisplayHeight(2);
            //this.rightUpgrade.setInterpolationDuration(2);
            this.visibleEntities.add(this.rightUpgrade.getEntityId());

            this.addElement(this.rightUpgrade);
        } else {
            this.isRightBlock = false;
        }

        var leftUpgrade = ((TileTurtle) proxy.getBlockEntity()).getUpgrade(TurtleSide.LEFT);
        if (leftUpgrade != null) {
            this.isLeftBlock = leftUpgrade.getCraftingItem().getItem() instanceof BlockItem;

            this.leftUpgrade.setItem(leftUpgrade.getCraftingItem());
            this.leftUpgrade.setModelTransformation(ItemDisplayContext.FIXED);
            this.leftUpgrade.setDisplayWidth(2);
            this.leftUpgrade.setDisplayHeight(2);
            //this.leftUpgrade.setInterpolationDuration(2);
            this.visibleEntities.add(this.leftUpgrade.getEntityId());
            this.addElement(this.leftUpgrade);
        } else {
            this.isLeftBlock = false;
        }

        this.setupTransforms();

    }

    private void setupTransforms() {
        matrix4f.clear();
        matrix4f.rotateY(this.targetRotation);
        matrix4f.pushMatrix();
        this.main.setTransformation(matrix4f.translate(0, -0.1f, 0).scale(1.5f));
        matrix4f.popMatrix();

        matrix4f.pushMatrix();
        matrix4f.translate(0, 0f, 0).scale(1.2f);
        this.color.setTransformation(matrix4f);
        matrix4f.popMatrix();

        if (this.leftUpgrade.getHolder() != null) {

            matrix4f.pushMatrix();

            if (this.isLeftBlock) {
                matrix4f.translate(-0.2f, -0.1f, 0);
            } else {
                matrix4f.translate(-0.45f, 0, 0);
            }

            matrix4f.rotateX(((TileTurtle) proxy.getBlockEntity()).getToolRenderAngle( TurtleSide.LEFT, 0 ) * -Mth.DEG_TO_RAD);

            matrix4f.rotateY(-Mth.HALF_PI);
            this.leftUpgrade.setTransformation(matrix4f);
            matrix4f.popMatrix();
        }

        if (this.rightUpgrade.getHolder() != null) {

            matrix4f.pushMatrix();

            if (this.isRightBlock) {
                matrix4f.translate(0.2f, -0.1f, 0);
            } else {
                matrix4f.translate(0.45f, 0, 0);
            }

            matrix4f.rotateX(((TileTurtle) proxy.getBlockEntity()).getToolRenderAngle( TurtleSide.RIGHT, 0 ) * -Mth.DEG_TO_RAD);

            matrix4f.rotateY(-Mth.HALF_PI);
            this.rightUpgrade.setTransformation(matrix4f);
            matrix4f.popMatrix();
        }
    }

    @Override
    protected void startWatchingExtraPackets(ServerGamePacketListenerImpl player, Consumer<Packet<ClientGamePacketListener>> packetConsumer) {
        packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.ride.getEntityId(), this.visibleEntities));
    }

    @Override
    protected void notifyElementsOfPositionUpdate(Vec3 newPos, Vec3 delta) {
        this.ride.notifyMove(this.currentPos, newPos, delta);
        //super.notifyElementsOfPositionUpdate(newPos, delta);
    }

    @Override
    protected void onTick() {
        //this.main.startInterpolation();
        //this.color.startInterpolation();
        //this.rightUpgrade.startInterpolation();
        //this.leftUpgrade.startInterpolation();
        this.setupTransforms();
        this.tickId++;
    }

    public void setColor(DyeColor color) {
        if (color == null) {
            this.color.setItem(ItemStack.EMPTY);
        } else {
            this.color.setItem(BuiltInRegistries.ITEM.get(new ResourceLocation(color.getName() + "_wool")).getDefaultInstance());
        }
    }

    public void setDirection(Direction direction) {
        this.targetRotation = (360 + 180 - direction.toYRot()) % 360 * Mth.DEG_TO_RAD;
    }
}
