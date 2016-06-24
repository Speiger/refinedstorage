package refinedstorage.tile.controller;

import cofh.api.energy.IEnergyReceiver;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import refinedstorage.RefinedStorageBlocks;
import refinedstorage.api.network.INetworkMaster;
import refinedstorage.api.network.INetworkSlave;
import refinedstorage.apiimpl.network.NetworkMaster;
import refinedstorage.apiimpl.network.NetworkMasterRegistry;
import refinedstorage.block.BlockController;
import refinedstorage.block.EnumControllerType;
import refinedstorage.container.ContainerController;
import refinedstorage.tile.ISynchronizedContainer;
import refinedstorage.tile.TileBase;
import refinedstorage.tile.config.IRedstoneModeConfig;
import refinedstorage.tile.config.RedstoneMode;

import java.util.ArrayList;
import java.util.List;

public class TileController extends TileBase implements IEnergyReceiver, ISynchronizedContainer, IRedstoneModeConfig {
    private INetworkMaster network;

    // Only used client side
    private List<ClientSlave> clientSlaves = new ArrayList<ClientSlave>();
    private int energy;
    private int energyUsage;
    private EnumControllerType type;
    private RedstoneMode redstoneMode;

    public INetworkMaster getNetwork() {
        if (network == null) {
            network = NetworkMasterRegistry.get(worldObj, pos);
        }

        return network;
    }

    @Override
    public NBTTagCompound writeUpdate(NBTTagCompound tag) {
        super.writeUpdate(tag);

        tag.setInteger(NetworkMaster.NBT_ENERGY, getNetwork() != null ? getNetwork().getEnergy().getEnergyStored() : 0);

        return tag;
    }

    @Override
    public void readUpdate(NBTTagCompound tag) {
        energy = tag.getInteger(NetworkMaster.NBT_ENERGY);

        super.readUpdate(tag);
    }

    @Override
    public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
        return getNetwork() != null ? getNetwork().getEnergy().receiveEnergy(maxReceive, simulate) : 0;
    }

    @Override
    public int getEnergyStored(EnumFacing from) {
        return getNetwork() != null ? getNetwork().getEnergy().getEnergyStored() : 0;
    }

    public int getEnergyScaled(int i) {
        float stored = worldObj.isRemote ? energy : (getNetwork() != null ? getNetwork().getEnergy().getEnergyStored() : 0);
        float max = NetworkMaster.ENERGY_CAPACITY;

        return (int) (stored / max * (float) i);
    }

    @Override
    public int getMaxEnergyStored(EnumFacing from) {
        return getNetwork() != null ? getNetwork().getEnergy().getMaxEnergyStored() : 0;
    }

    @Override
    public boolean canConnectEnergy(EnumFacing from) {
        return true;
    }

    @Override
    public RedstoneMode getRedstoneMode() {
        return worldObj.isRemote ? redstoneMode : (getNetwork() != null ? ((NetworkMaster) getNetwork()).getRedstoneMode() : RedstoneMode.IGNORE);
    }

    @Override
    public void setRedstoneMode(RedstoneMode mode) {
        if (getNetwork() != null) {
            ((NetworkMaster) getNetwork()).setRedstoneMode(mode);
        }
    }

    public List<ClientSlave> getClientSlaves() {
        return clientSlaves;
    }

    public int getEnergy() {
        return energy;
    }

    public int getEnergyUsage() {
        return energyUsage;
    }

    public EnumControllerType getType() {
        if (type == null && worldObj.getBlockState(pos).getBlock() == RefinedStorageBlocks.CONTROLLER) {
            this.type = (EnumControllerType) worldObj.getBlockState(pos).getValue(BlockController.TYPE);
        }

        return type == null ? EnumControllerType.NORMAL : type;
    }

    @Override
    public void readContainerData(ByteBuf buf) {
        this.energy = buf.readInt();
        this.energyUsage = buf.readInt();
        this.redstoneMode = RedstoneMode.getById(buf.readInt());

        List<ClientSlave> slaves = new ArrayList<ClientSlave>();

        int size = buf.readInt();

        for (int i = 0; i < size; ++i) {
            ClientSlave slave = new ClientSlave();

            slave.energyUsage = buf.readInt();
            slave.amount = buf.readInt();
            slave.stack = ByteBufUtils.readItemStack(buf);

            slaves.add(slave);
        }

        this.clientSlaves = slaves;
    }

    @Override
    public void writeContainerData(ByteBuf buf) {
        buf.writeInt(getNetwork() != null ? getNetwork().getEnergy().getEnergyStored() : 0);
        buf.writeInt(getNetwork() != null ? getNetwork().getEnergyUsage() : 0);

        buf.writeInt(getNetwork() != null ? ((NetworkMaster) getNetwork()).getRedstoneMode().id : RedstoneMode.IGNORE.id);

        if (getNetwork() != null) {
            List<ClientSlave> clientSlaves = new ArrayList<ClientSlave>();

            for (INetworkSlave slave : getNetwork().getSlaves()) {
                if (slave.canUpdate()) {
                    IBlockState state = worldObj.getBlockState(slave.getPosition());

                    ClientSlave clientSlave = new ClientSlave();

                    clientSlave.energyUsage = slave.getEnergyUsage();
                    clientSlave.amount = 1;
                    clientSlave.stack = new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state));

                    if (clientSlave.stack.getItem() != null && clientSlaves.contains(clientSlave)) {
                        for (ClientSlave other : clientSlaves) {
                            if (other.equals(clientSlave)) {
                                other.amount++;

                                break;
                            }
                        }
                    } else {
                        clientSlaves.add(clientSlave);
                    }
                }
            }

            buf.writeInt(clientSlaves.size());

            for (ClientSlave slave : clientSlaves) {
                buf.writeInt(slave.energyUsage);
                buf.writeInt(slave.amount);
                ByteBufUtils.writeItemStack(buf, slave.stack);
            }
        } else {
            buf.writeInt(0);
        }
    }

    @Override
    public Class<? extends Container> getContainer() {
        return ContainerController.class;
    }
}
